package com.example.game.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.GameEngine
import com.example.game.model.CameraMode
import com.example.game.model.PlayerMode
import com.example.game.model.StructureType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class WastelandRenderer(
  private val context: Context,
  private val engine: GameEngine
) : GLSurfaceView.Renderer {

  private val modelMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val projectionMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val scratchMatrix = FloatArray(16)

  private var programId = 0
  private var aPositionHandle = 0
  private var aColorHandle = 0
  private var aNormalHandle = 0
  private var uMvpMatrixHandle = 0
  private var uModelMatrixHandle = 0
  private var uLightDirHandle = 0
  private var uSunColorHandle = 0
  private var uAmbientColorHandle = 0
  private var uHeadlightPosHandle = 0
  private var uHeadlightDirHandle = 0
  private var uHeadlightEnabledHandle = 0
  private var uFogColorHandle = 0
  private var uFogDensityHandle = 0
  private var uCameraPosHandle = 0

  private var surfaceWidth = 1
  private var surfaceHeight = 1

  // Dynamic geometry buffers
  private var dynamicVertexBuffer: FloatBuffer? = null
  private val maxVertices = 40000
  private val vertexFloats = 10 // pos(3) + col(4) + norm(3)
  private val rawVertexArray = FloatArray(maxVertices * vertexFloats)
  private var currentVertexCount = 0

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glDepthFunc(GLES20.GL_LEQUAL)
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_BACK)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    initShaders()
    dynamicVertexBuffer = ByteBuffer.allocateDirect(maxVertices * vertexFloats * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    surfaceWidth = width
    surfaceHeight = height
    GLES20.glViewport(0, 0, width, height)
    val ratio = width.toFloat() / height.toFloat()
    Matrix.perspectiveM(projectionMatrix, 0, 68f, ratio, 0.1f, 350f)
  }

  override fun onDrawFrame(gl: GL10?) {
    engine.update()

    val timeOfDay = engine.timeOfDay.value
    val skyColors = getSkyAndFogColor(timeOfDay)

    GLES20.glClearColor(skyColors[0], skyColors[1], skyColors[2], 1.0f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    GLES20.glUseProgram(programId)

    // Setup camera matrices
    setupCamera()

    // Pass lighting and environment uniforms
    val sunAngle = (timeOfDay / 24f) * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
    val sunDirX = cos(sunAngle)
    val sunDirY = sin(sunAngle).coerceAtLeast(0.05f)
    val sunDirZ = 0.3f
    GLES20.glUniform3f(uLightDirHandle, sunDirX, sunDirY, sunDirZ)

    val sunIntensity = (sin(sunAngle)).coerceIn(0.05f, 1.0f)
    val isNight = timeOfDay < 5.5f || timeOfDay > 19.5f
    val sunR = if (isNight) 0.15f else (skyColors[4] * sunIntensity)
    val sunG = if (isNight) 0.20f else (skyColors[5] * sunIntensity)
    val sunB = if (isNight) 0.35f else (skyColors[6] * sunIntensity)
    GLES20.glUniform3f(uSunColorHandle, sunR, sunG, sunB)

    val amb = if (isNight) 0.12f else 0.35f
    GLES20.glUniform3f(uAmbientColorHandle, amb, amb, amb * 1.1f)

    // Headlights
    val car = engine.carState
    val rad = Math.toRadians(car.yaw.toDouble())
    val forwardX = -sin(rad).toFloat()
    val forwardZ = -cos(rad).toFloat()
    GLES20.glUniform3f(uHeadlightPosHandle, car.x, car.y + 0.6f, car.z)
    GLES20.glUniform3f(uHeadlightDirHandle, forwardX, -0.1f, forwardZ)
    GLES20.glUniform1f(uHeadlightEnabledHandle, if (engine.headlightsOn.value) 1.0f else 0.0f)

    // Fog
    GLES20.glUniform4f(uFogColorHandle, skyColors[0], skyColors[1], skyColors[2], 1.0f)
    GLES20.glUniform1f(uFogDensityHandle, if (isNight) 0.009f else 0.0065f)

    val camPos = engine.cameraPosition
    GLES20.glUniform3f(uCameraPosHandle, camPos[0], camPos[1], camPos[2])

    // Render world geometry
    renderWorldScene()
  }

  private fun setupCamera() {
    val camPos = engine.cameraPosition
    val camLook = engine.cameraLookAt
    val camUp = engine.cameraUp
    Matrix.setLookAtM(
      viewMatrix, 0,
      camPos[0], camPos[1], camPos[2],
      camLook[0], camLook[1], camLook[2],
      camUp[0], camUp[1], camUp[2]
    )
  }

  private fun renderWorldScene() {
    currentVertexCount = 0

    // 1. Render Sky Dome / Distant Mesas
    buildDistantCanyons()

    // 2. Render Road Chunks & Desert Terrain
    buildTerrainAndRoads()

    // 3. Render Roadside Props (Cacti, Rocks, Broken Cars)
    buildProps()

    // 4. Render Structures (Abandoned Houses, Museums, 100km Outpost)
    buildStructures()

    // 5. Render Relic Items on the Ground
    buildGroundItems()

    // 6. Render Zombies
    buildZombies()

    // 7. Render The Car (Exterior or Cockpit Interior)
    buildCar()

    // 8. Flush all dynamic geometry to GPU
    flushDynamicGeometry()
  }

  private fun buildDistantCanyons() {
    val playerZ = engine.playerZ
    // Draw mountain silhouettes on left and right horizons
    for (i in -4..4) {
      val cz = playerZ + (i * 70f)
      // Left canyon mesa
      addMesaBox(-110f, 0f, cz, 45f, 35f, 65f, 0.42f, 0.22f, 0.16f)
      // Right canyon mesa
      addMesaBox(110f, 0f, cz, 45f, 38f, 65f, 0.45f, 0.25f, 0.18f)
    }
  }

  private fun buildTerrainAndRoads() {
    val playerZ = engine.playerZ
    val chunkSpacing = 30f
    val baseChunkIndex = (playerZ / chunkSpacing).toInt()

    for (c in (baseChunkIndex - 4)..(baseChunkIndex + 8)) {
      val zStart = c * chunkSpacing
      val zEnd = (c + 1) * chunkSpacing

      // Desert floor (left side)
      addQuad(
        -150f, -0.05f, zStart,
        -5f, -0.05f, zStart,
        -5f, -0.05f, zEnd,
        -150f, -0.05f, zEnd,
        0.58f, 0.44f, 0.31f, 1f,
        0f, 1f, 0f
      )

      // Desert floor (right side)
      addQuad(
        5f, -0.05f, zStart,
        150f, -0.05f, zStart,
        150f, -0.05f, zEnd,
        5f, -0.05f, zEnd,
        0.58f, 0.44f, 0.31f, 1f,
        0f, 1f, 0f
      )

      // Shoulder dirt strip (left)
      addQuad(
        -5.0f, 0f, zStart,
        -4.2f, 0f, zStart,
        -4.2f, 0f, zEnd,
        -5.0f, 0f, zEnd,
        0.42f, 0.32f, 0.22f, 1f,
        0f, 1f, 0f
      )

      // Shoulder dirt strip (right)
      addQuad(
        4.2f, 0f, zStart,
        5.0f, 0f, zStart,
        5.0f, 0f, zEnd,
        4.2f, 0f, zEnd,
        0.42f, 0.32f, 0.22f, 1f,
        0f, 1f, 0f
      )

      // Asphalt main road
      addQuad(
        -4.2f, 0.02f, zStart,
        4.2f, 0.02f, zStart,
        4.2f, 0.02f, zEnd,
        -4.2f, 0.02f, zEnd,
        0.18f, 0.17f, 0.17f, 1f,
        0f, 1f, 0f
      )

      // Center dashed yellow line
      val dashMidZ = (zStart + zEnd) * 0.5f
      addQuad(
        -0.12f, 0.03f, dashMidZ - 3.5f,
        0.12f, 0.03f, dashMidZ - 3.5f,
        0.12f, 0.03f, dashMidZ + 3.5f,
        -0.12f, 0.03f, dashMidZ + 3.5f,
        0.95f, 0.78f, 0.15f, 1f,
        0f, 1f, 0f
      )

      // White shoulder lines
      addQuad(
        -3.8f, 0.03f, zStart,
        -3.65f, 0.03f, zStart,
        -3.65f, 0.03f, zEnd,
        -3.8f, 0.03f, zEnd,
        0.85f, 0.85f, 0.85f, 1f,
        0f, 1f, 0f
      )
      addQuad(
        3.65f, 0.03f, zStart,
        3.8f, 0.03f, zStart,
        3.8f, 0.03f, zEnd,
        3.65f, 0.03f, zEnd,
        0.85f, 0.85f, 0.85f, 1f,
        0f, 1f, 0f
      )
    }
  }

  private fun buildProps() {
    val props = engine.visibleProps
    for (prop in props) {
      when (prop.type) {
        0 -> { // Desert Boulder / Rock
          addBox(prop.x, 0f, prop.z, prop.scale * 1.6f, prop.scale * 1.1f, prop.scale * 1.4f, 0.48f, 0.38f, 0.30f)
        }
        1 -> { // Saguaro Cactus
          // Trunk
          addBox(prop.x, 0f, prop.z, 0.35f * prop.scale, 3.2f * prop.scale, 0.35f * prop.scale, 0.22f, 0.42f, 0.18f)
          // Left arm
          addBox(prop.x - 0.5f * prop.scale, 1.4f * prop.scale, prop.z, 0.6f * prop.scale, 0.25f * prop.scale, 0.25f * prop.scale, 0.22f, 0.42f, 0.18f)
          addBox(prop.x - 0.8f * prop.scale, 1.4f * prop.scale, prop.z, 0.25f * prop.scale, 1.1f * prop.scale, 0.25f * prop.scale, 0.22f, 0.42f, 0.18f)
          // Right arm
          addBox(prop.x + 0.5f * prop.scale, 1.8f * prop.scale, prop.z, 0.6f * prop.scale, 0.25f * prop.scale, 0.25f * prop.scale, 0.22f, 0.42f, 0.18f)
          addBox(prop.x + 0.8f * prop.scale, 1.8f * prop.scale, prop.z, 0.25f * prop.scale, 1.0f * prop.scale, 0.25f * prop.scale, 0.22f, 0.42f, 0.18f)
        }
        2 -> { // Wrecked rusted car chassis WITHOUT TAILGATE
          // Rusted body
          addBox(prop.x, 0.3f, prop.z, 1.9f, 0.7f, 3.8f, 0.42f, 0.24f, 0.16f)
          // Cabin
          addBox(prop.x, 1.0f, prop.z - 0.2f, 1.6f, 0.6f, 1.8f, 0.36f, 0.20f, 0.14f)
          // Open empty trunk bed in rear (no door!)
          addBox(prop.x, 0.65f, prop.z + 1.3f, 1.5f, 0.2f, 1.1f, 0.24f, 0.15f, 0.12f)
        }
      }
    }
  }

  private fun buildStructures() {
    val structures = engine.visibleStructures
    for (s in structures) {
      when (s.type) {
        StructureType.HOUSE -> {
          // Abandoned roadside house
          // Main walls
          addBox(s.x, 0f, s.z, 9.0f, 4.2f, 8.0f, 0.52f, 0.48f, 0.42f)
          // Interior floor cut/roof
          addBox(s.x, 4.2f, s.z, 10.0f, 0.4f, 9.0f, 0.35f, 0.25f, 0.20f)
          // Open doorway
          addBox(s.x, 0f, s.z - 4.1f, 2.0f, 3.0f, 0.4f, 0.15f, 0.12f, 0.10f)
          // Window openings
          addBox(s.x - 3.0f, 1.5f, s.z - 4.1f, 1.4f, 1.4f, 0.3f, 0.12f, 0.10f, 0.08f)
          addBox(s.x + 3.0f, 1.5f, s.z - 4.1f, 1.4f, 1.4f, 0.3f, 0.12f, 0.10f, 0.08f)
        }
        StructureType.MUSEUM -> {
          // Abandoned neoclassical museum with grand columns and dome
          // Main steps & pedestal
          addBox(s.x, 0f, s.z, 18.0f, 1.2f, 16.0f, 0.68f, 0.65f, 0.60f)
          // Main hall
          addBox(s.x, 1.2f, s.z + 2.0f, 15.0f, 6.0f, 12.0f, 0.60f, 0.58f, 0.54f)
          // 4 Grand Classical Columns
          val colY = 1.2f
          val colH = 5.0f
          for (c in listOf(-5.0f, -1.8f, 1.8f, 5.0f)) {
            addBox(s.x + c, colY, s.z - 4.5f, 0.8f, colH, 0.8f, 0.80f, 0.78f, 0.74f)
          }
          // Entablature / Pediment triangular-ish block above columns
          addBox(s.x, colY + colH, s.z - 4.5f, 14.0f, 1.2f, 1.8f, 0.72f, 0.70f, 0.66f)
          // Grand Central Dome on top
          addBox(s.x, colY + colH + 1.2f, s.z + 1.5f, 6.0f, 3.2f, 6.0f, 0.75f, 0.73f, 0.68f)
        }
        StructureType.OUTPOST_100KM -> {
          // 100 KM Trading Post / Mechanic Workshop
          // Gas station canopy
          addBox(s.x, 4.2f, s.z, 14.0f, 0.5f, 10.0f, 0.85f, 0.35f, 0.15f)
          // 2 Support pillars
          addBox(s.x - 4.5f, 0f, s.z, 0.6f, 4.2f, 0.6f, 0.90f, 0.85f, 0.20f)
          addBox(s.x + 4.5f, 0f, s.z, 0.6f, 4.2f, 0.6f, 0.90f, 0.85f, 0.20f)
          // Gas pump islands
          addBox(s.x, 0f, s.z, 1.8f, 1.5f, 1.0f, 0.90f, 0.15f, 0.15f)
          // Workshop store building
          addBox(s.x + 10.0f, 0f, s.z, 8.0f, 4.0f, 8.0f, 0.30f, 0.35f, 0.40f)
          // Glowing Neon Store Sign
          addBox(s.x + 10.0f, 4.3f, s.z - 3.8f, 6.0f, 1.0f, 0.3f, 0.1f, 0.95f, 0.4f)
        }
      }
    }
  }

  private fun buildGroundItems() {
    val items = engine.visibleItems
    val playerPos = engine.playerPos
    val hover = sin(System.currentTimeMillis() * 0.005).toFloat() * 0.12f

    for (item in items) {
      if (item.isPickedUp || item.isInTrunk) continue

      val r = ((item.item.colorHex shr 16) and 0xFF) / 255f
      val g = ((item.item.colorHex shr 8) and 0xFF) / 255f
      val b = (item.item.colorHex and 0xFF) / 255f

      // Glowing floating item box / beacon
      addBox(item.x, item.y + 0.3f + hover, item.z, 0.55f, 0.55f, 0.55f, r, g, b)
      // Base pedestal / beam
      addBox(item.x, item.y, item.z, 0.15f, 0.3f + hover, 0.15f, 1f, 0.9f, 0.4f)
    }

    // Hand-carried item when player is on foot!
    if (engine.playerMode.value == PlayerMode.ON_FOOT && engine.heldItem.value != null) {
      val carried = engine.heldItem.value!!
      val r = ((carried.item.colorHex shr 16) and 0xFF) / 255f
      val g = ((carried.item.colorHex shr 8) and 0xFF) / 255f
      val b = (carried.item.colorHex and 0xFF) / 255f

      // Position in front-right of camera
      val camPos = engine.cameraPosition
      val camLook = engine.cameraLookAt
      val dirX = camLook[0] - camPos[0]
      val dirZ = camLook[2] - camPos[2]
      val len = kotlin.math.sqrt(dirX * dirX + dirZ * dirZ).coerceAtLeast(0.01f)
      val ndx = dirX / len
      val ndz = dirZ / len
      val rightX = -ndz
      val rightZ = ndx

      val handX = camPos[0] + (ndx * 0.8f) + (rightX * 0.35f)
      val handY = camPos[1] - 0.35f
      val handZ = camPos[2] + (ndz * 0.8f) + (rightZ * 0.35f)

      addBox(handX, handY, handZ, 0.35f, 0.35f, 0.35f, r, g, b)
    }
  }

  private fun buildZombies() {
    val zombies = engine.visibleZombies
    for (z in zombies) {
      if (!z.isAlive) continue

      val scale = z.scale
      val walkSway = sin(z.walkCycle.toDouble()).toFloat() * 0.15f

      // Zombie body color scales with level: L1-2 greyish, L3-4 greenish, L5+ reddish mutant
      val (skinR, skinG, skinB) = when {
        z.level >= 5 -> Triple(0.75f, 0.22f, 0.22f)
        z.level >= 3 -> Triple(0.25f, 0.55f, 0.25f)
        else -> Triple(0.48f, 0.48f, 0.45f)
      }

      // Torso
      addBox(z.x, z.y + (0.9f * scale), z.z, 0.5f * scale, 0.7f * scale, 0.3f * scale, 0.35f, 0.30f, 0.25f)
      // Head
      addBox(z.x, z.y + (1.6f * scale), z.z, 0.4f * scale, 0.4f * scale, 0.4f * scale, skinR, skinG, skinB)
      // Glowing Eyes
      val eyeColor = if (z.level >= 4) 1.0f else 0.8f
      addBox(z.x - (0.1f * scale), z.y + (1.65f * scale), z.z - (0.22f * scale), 0.08f * scale, 0.06f * scale, 0.05f * scale, eyeColor, 0.1f, 0.1f)
      addBox(z.x + (0.1f * scale), z.y + (1.65f * scale), z.z - (0.22f * scale), 0.08f * scale, 0.06f * scale, 0.05f * scale, eyeColor, 0.1f, 0.1f)
      // Outstretched arms
      addBox(z.x - (0.35f * scale), z.y + (1.2f * scale) + walkSway, z.z - (0.45f * scale), 0.18f * scale, 0.18f * scale, 0.7f * scale, skinR, skinG, skinB)
      addBox(z.x + (0.35f * scale), z.y + (1.2f * scale) - walkSway, z.z - (0.45f * scale), 0.18f * scale, 0.18f * scale, 0.7f * scale, skinR, skinG, skinB)
      // Legs
      addBox(z.x - (0.18f * scale), z.y, z.z, 0.2f * scale, 0.9f * scale, 0.25f * scale, 0.22f, 0.20f, 0.20f)
      addBox(z.x + (0.18f * scale), z.y, z.z, 0.2f * scale, 0.9f * scale, 0.25f * scale, 0.22f, 0.20f, 0.20f)
    }
  }

  private fun buildCar() {
    val car = engine.carState
    val isCockpit = engine.playerMode.value == PlayerMode.DRIVING && engine.cameraMode.value == CameraMode.FIRST_PERSON_COCKPIT

    if (isCockpit) {
      // FIRST PERSON COCKPIT VIEW: Detailed interior
      // Dashboard shelf
      addBox(car.x, car.y + 0.75f, car.z - 1.2f, 1.6f, 0.35f, 0.7f, 0.18f, 0.15f, 0.14f)
      // Steering column & wheel
      val steerAngle = engine.steerAngle
      val swOffset = sin(Math.toRadians(steerAngle.toDouble())).toFloat() * 0.1f
      addBox(car.x - 0.35f + swOffset, car.y + 0.88f, car.z - 0.95f, 0.45f, 0.45f, 0.08f, 0.12f, 0.11f, 0.10f)
      // Steering wheel rim highlight
      addBox(car.x - 0.35f + swOffset, car.y + 1.08f, car.z - 0.96f, 0.42f, 0.06f, 0.06f, 0.85f, 0.40f, 0.15f)

      // Speedometer & Fuel Gauge Cluster
      addBox(car.x - 0.35f, car.y + 0.95f, car.z - 1.35f, 0.5f, 0.22f, 0.04f, 0.05f, 0.05f, 0.05f)
      // Speed needle (animated based on speed)
      val speedRatio = (car.speedKmh / 180f).coerceIn(0f, 1f)
      addBox(car.x - 0.35f + (speedRatio * 0.18f), car.y + 0.98f, car.z - 1.37f, 0.04f, 0.12f, 0.02f, 0.95f, 0.15f, 0.15f)
      // Fuel needle
      val fuelRatio = (engine.fuel.value / engine.maxFuel.value).coerceIn(0f, 1f)
      addBox(car.x - 0.18f + (fuelRatio * 0.12f), car.y + 0.98f, car.z - 1.37f, 0.03f, 0.09f, 0.02f, 0.15f, 0.85f, 0.25f)

      // Hood scoop / Blower visible through windshield vibrating with engine RPM
      val engineVibe = sin(System.currentTimeMillis() * 0.03).toFloat() * 0.015f
      addBox(car.x, car.y + 0.85f + engineVibe, car.z - 2.5f, 0.65f, 0.45f, 0.8f, 0.85f, 0.32f, 0.12f)
      // Windshield A-pillars
      addBox(car.x - 0.82f, car.y + 0.85f, car.z - 1.1f, 0.12f, 0.9f, 0.15f, 0.22f, 0.18f, 0.16f)
      addBox(car.x + 0.82f, car.y + 0.85f, car.z - 1.1f, 0.12f, 0.9f, 0.15f, 0.22f, 0.18f, 0.16f)
      // Rearview mirror
      addBox(car.x, car.y + 1.62f, car.z - 1.1f, 0.32f, 0.12f, 0.05f, 0.65f, 0.70f, 0.75f)
    } else {
      // EXTERIOR VIEW (when in 3rd person OR when player stepped out on foot!)
      // Heavy chassis base
      addBox(car.x, car.y + 0.35f, car.z, 2.1f, 0.65f, 4.4f, 0.62f, 0.28f, 0.14f)
      // Cabin roof and windows
      addBox(car.x, car.y + 1.0f, car.z - 0.3f, 1.8f, 0.6f, 2.0f, 0.22f, 0.20f, 0.18f)
      // Front Cowcatcher / Spiked Bumper
      addBox(car.x, car.y + 0.25f, car.z - 2.3f, 2.3f, 0.45f, 0.25f, 0.12f, 0.12f, 0.12f)
      addBox(car.x - 0.8f, car.y + 0.35f, car.z - 2.45f, 0.15f, 0.3f, 0.35f, 0.7f, 0.7f, 0.7f)
      addBox(car.x + 0.8f, car.y + 0.35f, car.z - 2.45f, 0.15f, 0.3f, 0.35f, 0.7f, 0.7f, 0.7f)
      // Supercharger Hood Blower
      addBox(car.x, car.y + 0.75f, car.z - 1.5f, 0.7f, 0.4f, 0.8f, 0.85f, 0.42f, 0.12f)

      // OPEN TRUNK / CARGO BED WITHOUT TAILGATE (Todos los coches no tendrán puerta de maletero)
      // Bed floor
      addBox(car.x, car.y + 0.48f, car.z + 1.4f, 1.7f, 0.15f, 1.4f, 0.20f, 0.16f, 0.14f)
      // Left and right trunk bed walls, open back
      addBox(car.x - 0.9f, car.y + 0.68f, car.z + 1.4f, 0.15f, 0.35f, 1.4f, 0.55f, 0.24f, 0.12f)
      addBox(car.x + 0.9f, car.y + 0.68f, car.z + 1.4f, 0.15f, 0.35f, 1.4f, 0.55f, 0.24f, 0.12f)

      // Render physical loaded items in the open trunk!
      val trunkItems = engine.trunkItems.value
      for (i in trunkItems.indices.take(8)) {
        val relic = trunkItems[i]
        val rx = car.x + ((i % 3) - 1) * 0.45f
        val rz = car.z + 1.1f + ((i / 3) * 0.45f)
        val r = ((relic.colorHex shr 16) and 0xFF) / 255f
        val g = ((relic.colorHex shr 8) and 0xFF) / 255f
        val b = (relic.colorHex and 0xFF) / 255f
        addBox(rx, car.y + 0.65f, rz, 0.3f, 0.25f, 0.3f, r, g, b)
      }

      // Wheels
      addBox(car.x - 1.05f, car.y + 0.25f, car.z - 1.4f, 0.35f, 0.6f, 0.6f, 0.12f, 0.12f, 0.12f)
      addBox(car.x + 1.05f, car.y + 0.25f, car.z - 1.4f, 0.35f, 0.6f, 0.6f, 0.12f, 0.12f, 0.12f)
      addBox(car.x - 1.05f, car.y + 0.25f, car.z + 1.4f, 0.35f, 0.6f, 0.6f, 0.12f, 0.12f, 0.12f)
      addBox(car.x + 1.05f, car.y + 0.25f, car.z + 1.4f, 0.35f, 0.6f, 0.6f, 0.12f, 0.12f, 0.12f)

      // Headlight bulbs glowing
      val hlColor = if (engine.headlightsOn.value) 1.0f else 0.4f
      addBox(car.x - 0.7f, car.y + 0.5f, car.z - 2.22f, 0.25f, 0.2f, 0.05f, hlColor, hlColor * 0.9f, 0.2f)
      addBox(car.x + 0.7f, car.y + 0.5f, car.z - 2.22f, 0.25f, 0.2f, 0.05f, hlColor, hlColor * 0.9f, 0.2f)
    }
  }

  // Helper geometry primitives
  private fun addBox(
    cx: Float, cy: Float, cz: Float,
    w: Float, h: Float, d: Float,
    r: Float, g: Float, b: Float, a: Float = 1.0f
  ) {
    val hw = w * 0.5f
    val hd = d * 0.5f
    val y0 = cy
    val y1 = cy + h

    // Front (Z-)
    addQuad(
      cx - hw, y0, cz - hd,
      cx + hw, y0, cz - hd,
      cx + hw, y1, cz - hd,
      cx - hw, y1, cz - hd,
      r, g, b, a,
      0f, 0f, -1f
    )
    // Back (Z+)
    addQuad(
      cx + hw, y0, cz + hd,
      cx - hw, y0, cz + hd,
      cx - hw, y1, cz + hd,
      cx + hw, y1, cz + hd,
      r * 0.85f, g * 0.85f, b * 0.85f, a,
      0f, 0f, 1f
    )
    // Left (X-)
    addQuad(
      cx - hw, y0, cz + hd,
      cx - hw, y0, cz - hd,
      cx - hw, y1, cz - hd,
      cx - hw, y1, cz + hd,
      r * 0.75f, g * 0.75f, b * 0.75f, a,
      -1f, 0f, 0f
    )
    // Right (X+)
    addQuad(
      cx + hw, y0, cz - hd,
      cx + hw, y0, cz + hd,
      cx + hw, y1, cz + hd,
      cx + hw, y1, cz - hd,
      r * 0.9f, g * 0.9f, b * 0.9f, a,
      1f, 0f, 0f
    )
    // Top (Y+)
    addQuad(
      cx - hw, y1, cz - hd,
      cx + hw, y1, cz - hd,
      cx + hw, y1, cz + hd,
      cx - hw, y1, cz + hd,
      r * 1.1f.coerceAtMost(1f), g * 1.1f.coerceAtMost(1f), b * 1.1f.coerceAtMost(1f), a,
      0f, 1f, 0f
    )
  }

  private fun addMesaBox(
    cx: Float, cy: Float, cz: Float,
    w: Float, h: Float, d: Float,
    r: Float, g: Float, b: Float
  ) {
    addBox(cx, cy, cz, w, h, d, r, g, b, 1.0f)
  }

  private fun addQuad(
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    x3: Float, y3: Float, z3: Float,
    x4: Float, y4: Float, z4: Float,
    r: Float, g: Float, b: Float, a: Float,
    nx: Float, ny: Float, nz: Float
  ) {
    if (currentVertexCount + 6 > maxVertices) {
      flushDynamicGeometry()
    }

    // Triangle 1: 1 -> 2 -> 3
    putVertex(x1, y1, z1, r, g, b, a, nx, ny, nz)
    putVertex(x2, y2, z2, r, g, b, a, nx, ny, nz)
    putVertex(x3, y3, z3, r, g, b, a, nx, ny, nz)

    // Triangle 2: 1 -> 3 -> 4
    putVertex(x1, y1, z1, r, g, b, a, nx, ny, nz)
    putVertex(x3, y3, z3, r, g, b, a, nx, ny, nz)
    putVertex(x4, y4, z4, r, g, b, a, nx, ny, nz)
  }

  private fun putVertex(
    x: Float, y: Float, z: Float,
    r: Float, g: Float, b: Float, a: Float,
    nx: Float, ny: Float, nz: Float
  ) {
    val offset = currentVertexCount * vertexFloats
    rawVertexArray[offset] = x
    rawVertexArray[offset + 1] = y
    rawVertexArray[offset + 2] = z
    rawVertexArray[offset + 3] = r
    rawVertexArray[offset + 4] = g
    rawVertexArray[offset + 5] = b
    rawVertexArray[offset + 6] = a
    rawVertexArray[offset + 7] = nx
    rawVertexArray[offset + 8] = ny
    rawVertexArray[offset + 9] = nz
    currentVertexCount++
  }

  private fun flushDynamicGeometry() {
    if (currentVertexCount == 0) return

    val buffer = dynamicVertexBuffer ?: return
    buffer.position(0)
    buffer.put(rawVertexArray, 0, currentVertexCount * vertexFloats)
    buffer.position(0)

    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.multiplyMM(scratchMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, scratchMatrix, 0)

    GLES20.glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0)

    val stride = vertexFloats * 4
    buffer.position(0)
    GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)
    GLES20.glEnableVertexAttribArray(aPositionHandle)

    buffer.position(3)
    GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, stride, buffer)
    GLES20.glEnableVertexAttribArray(aColorHandle)

    buffer.position(7)
    GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)
    GLES20.glEnableVertexAttribArray(aNormalHandle)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, currentVertexCount)

    GLES20.glDisableVertexAttribArray(aPositionHandle)
    GLES20.glDisableVertexAttribArray(aColorHandle)
    GLES20.glDisableVertexAttribArray(aNormalHandle)

    currentVertexCount = 0
  }

  private fun getSkyAndFogColor(timeOfDay: Float): FloatArray {
    // Returns [r, g, b, fogDensity, sunR, sunG, sunB]
    return when {
      timeOfDay in 5.0f..8.0f -> {
        // Sunrise: rose gold / orange dawn
        val t = (timeOfDay - 5.0f) / 3.0f
        floatArrayOf(
          0.85f * (1 - t) + 0.45f * t,
          0.45f * (1 - t) + 0.65f * t,
          0.35f * (1 - t) + 0.85f * t,
          0.007f,
          1.0f, 0.75f, 0.5f
        )
      }
      timeOfDay in 8.0f..17.0f -> {
        // Scorching desert midday
        floatArrayOf(
          0.38f, 0.62f, 0.82f,
          0.005f,
          1.0f, 0.95f, 0.85f
        )
      }
      timeOfDay in 17.0f..20.0f -> {
        // Sunset: intense post-apocalyptic fiery orange & dusk
        val t = (timeOfDay - 17.0f) / 3.0f
        floatArrayOf(
          0.92f * (1 - t) + 0.15f * t,
          0.42f * (1 - t) + 0.12f * t,
          0.20f * (1 - t) + 0.22f * t,
          0.008f,
          1.0f, 0.50f, 0.25f
        )
      }
      else -> {
        // Midnight wasteland: dark starry sky
        floatArrayOf(
          0.06f, 0.07f, 0.14f,
          0.010f,
          0.3f, 0.35f, 0.5f
        )
      }
    }
  }

  private fun initShaders() {
    val vertexShaderCode = """
      uniform mat4 u_MVPMatrix;
      uniform mat4 u_ModelMatrix;
      uniform vec3 u_LightDir;
      uniform vec3 u_SunColor;
      uniform vec3 u_AmbientColor;
      uniform vec3 u_HeadlightPos;
      uniform vec3 u_HeadlightDir;
      uniform float u_HeadlightEnabled;
      uniform vec3 u_CameraPos;

      attribute vec4 a_Position;
      attribute vec4 a_Color;
      attribute vec3 a_Normal;

      varying vec4 v_Color;
      varying float v_Distance;

      void main() {
        gl_Position = u_MVPMatrix * a_Position;
        vec4 worldPos = u_ModelMatrix * a_Position;
        vec3 norm = normalize((u_ModelMatrix * vec4(a_Normal, 0.0)).xyz);

        // Sun diffuse
        float diff = max(dot(norm, normalize(u_LightDir)), 0.0);
        vec3 light = u_AmbientColor + (u_SunColor * diff);

        // Car Headlight Spotlight
        if (u_HeadlightEnabled > 0.5) {
          vec3 lightToVertex = worldPos.xyz - u_HeadlightPos;
          float dist = length(lightToVertex);
          if (dist > 0.1 && dist < 45.0) {
            vec3 lightDir = normalize(lightToVertex);
            float spot = dot(lightDir, normalize(u_HeadlightDir));
            if (spot > 0.72) {
              float intensity = smoothstep(0.72, 0.95, spot) * (1.0 - (dist / 45.0));
              light += vec3(1.0, 0.96, 0.75) * (intensity * 2.2);
            }
          }
        }

        v_Color = vec4(a_Color.rgb * light, a_Color.a);
        v_Distance = length(u_CameraPos - worldPos.xyz);
      }
    """.trimIndent()

    val fragmentShaderCode = """
      precision mediump float;
      varying vec4 v_Color;
      varying float v_Distance;
      uniform vec4 u_FogColor;
      uniform float u_FogDensity;

      void main() {
        float fogFactor = exp(-v_Distance * u_FogDensity);
        fogFactor = clamp(fogFactor, 0.0, 1.0);
        gl_FragColor = mix(u_FogColor, v_Color, fogFactor);
      }
    """.trimIndent()

    val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    programId = GLES20.glCreateProgram()
    GLES20.glAttachShader(programId, vs)
    GLES20.glAttachShader(programId, fs)
    GLES20.glLinkProgram(programId)

    aPositionHandle = GLES20.glGetAttribLocation(programId, "a_Position")
    aColorHandle = GLES20.glGetAttribLocation(programId, "a_Color")
    aNormalHandle = GLES20.glGetAttribLocation(programId, "a_Normal")

    uMvpMatrixHandle = GLES20.glGetUniformLocation(programId, "u_MVPMatrix")
    uModelMatrixHandle = GLES20.glGetUniformLocation(programId, "u_ModelMatrix")
    uLightDirHandle = GLES20.glGetUniformLocation(programId, "u_LightDir")
    uSunColorHandle = GLES20.glGetUniformLocation(programId, "u_SunColor")
    uAmbientColorHandle = GLES20.glGetUniformLocation(programId, "u_AmbientColor")
    uHeadlightPosHandle = GLES20.glGetUniformLocation(programId, "u_HeadlightPos")
    uHeadlightDirHandle = GLES20.glGetUniformLocation(programId, "u_HeadlightDir")
    uHeadlightEnabledHandle = GLES20.glGetUniformLocation(programId, "u_HeadlightEnabled")
    uFogColorHandle = GLES20.glGetUniformLocation(programId, "u_FogColor")
    uFogDensityHandle = GLES20.glGetUniformLocation(programId, "u_FogDensity")
    uCameraPosHandle = GLES20.glGetUniformLocation(programId, "u_CameraPos")
  }

  private fun loadShader(type: Int, shaderCode: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, shaderCode)
    GLES20.glCompileShader(shader)
    return shader
  }
}
