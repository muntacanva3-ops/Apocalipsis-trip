package com.example.game.engine

import com.example.game.catalog.Catalog
import com.example.game.catalog.RelicItem
import com.example.game.model.CameraMode
import com.example.game.model.CarUpgrades
import com.example.game.model.PlayerMode
import com.example.game.model.PropData
import com.example.game.model.StructureType
import com.example.game.model.WorldItemInstance
import com.example.game.model.WorldStructure
import com.example.game.model.ZombieInstance
import com.example.game.sound.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class CarState(
  var x: Float = 0f,
  var y: Float = 0f,
  var z: Float = 0f,
  var speedKmh: Float = 0f,
  var yaw: Float = 0f
)

class GameEngine {
  val carState = CarState()
  val upgrades = CarUpgrades()

  // Player state
  private val _playerMode = MutableStateFlow(PlayerMode.DRIVING)
  val playerMode = _playerMode.asStateFlow()

  private val _cameraMode = MutableStateFlow(CameraMode.FIRST_PERSON_COCKPIT)
  val cameraMode = _cameraMode.asStateFlow()

  // On-foot position
  var footX = 0f
  var footY = 1.65f
  var footZ = 0f
  var footYaw = 0f
  var footPitch = 0f
  private var footVerticalVelocity = 0f
  private var isGrounded = true

  // Survival stats
  private val _health = MutableStateFlow(100f)
  val health = _health.asStateFlow()

  private val _hunger = MutableStateFlow(100f)
  val hunger = _hunger.asStateFlow()

  private val _thirst = MutableStateFlow(100f)
  val thirst = _thirst.asStateFlow()

  private val _fuel = MutableStateFlow(50f)
  val fuel = _fuel.asStateFlow()

  private val _maxFuel = MutableStateFlow(50f)
  val maxFuel = _maxFuel.asStateFlow()

  private val _money = MutableStateFlow(50)
  val money = _money.asStateFlow()

  private val _distanceKm = MutableStateFlow(0f)
  val distanceKm = _distanceKm.asStateFlow()

  private val _timeOfDay = MutableStateFlow(8.0f) // Starts at 8:00 AM
  val timeOfDay = _timeOfDay.asStateFlow()

  private val _headlightsOn = MutableStateFlow(false)
  val headlightsOn = _headlightsOn.asStateFlow()

  // Trunk inventory (30 unique relics can be stored here)
  private val _trunkItems = MutableStateFlow<List<RelicItem>>(emptyList())
  val trunkItems = _trunkItems.asStateFlow()

  // Held item (carried in hand when exploring on foot)
  private val _heldItem = MutableStateFlow<WorldItemInstance?>(null)
  val heldItem = _heldItem.asStateFlow()

  // Item currently in reach / targeted
  private val _targetedItem = MutableStateFlow<WorldItemInstance?>(null)
  val targetedItem = _targetedItem.asStateFlow()

  // Is player near the open trunk of the car?
  private val _isNearCarTrunk = MutableStateFlow(false)
  val isNearCarTrunk = _isNearCarTrunk.asStateFlow()

  // Shop modal state (every 100km)
  private val _isShopOpen = MutableStateFlow(false)
  val isShopOpen = _isShopOpen.asStateFlow()

  // Outpost detected within range
  private val _isAtOutpost = MutableStateFlow(false)
  val isAtOutpost = _isAtOutpost.asStateFlow()

  // Notifications / Alerts
  private val _notification = MutableStateFlow<String?>(null)
  val notification = _notification.asStateFlow()

  // Controls inputs
  var throttleInput = 0f // 1.0 forward, -1.0 reverse
  var steerInput = 0f    // -1.0 left, 1.0 right
  var moveForwardInput = 0f
  var moveStrafeInput = 0f
  var steerAngle = 0f

  // Visible entities
  val visibleStructures = CopyOnWriteArrayList<WorldStructure>()
  val visibleProps = CopyOnWriteArrayList<PropData>()
  val visibleItems = CopyOnWriteArrayList<WorldItemInstance>()
  val visibleZombies = CopyOnWriteArrayList<ZombieInstance>()

  // Camera look outputs
  val cameraPosition = FloatArray(3)
  val cameraLookAt = FloatArray(3)
  val cameraUp = floatArrayOf(0f, 1f, 0f)

  val playerZ: Float
    get() = if (_playerMode.value == PlayerMode.DRIVING) carState.z else footZ

  val playerPos: FloatArray
    get() = if (_playerMode.value == PlayerMode.DRIVING) {
      floatArrayOf(carState.x, carState.y, carState.z)
    } else {
      floatArrayOf(footX, footY, footZ)
    }

  private var lastUpdateTime = System.currentTimeMillis()
  private var lastChunkZ = 50f
  private var instanceCounter = 1L
  private var zombieIdCounter = 1L
  private var lastShopKmTriggered = -1

  init {
    // Generate initial world chunks
    generateChunksAhead(carState.z - 250f)
  }

  fun update() {
    val now = System.currentTimeMillis()
    val dt = ((now - lastUpdateTime) / 1000f).coerceIn(0.001f, 0.1f)
    lastUpdateTime = now

    // 1. Advance Day / Night Cycle (1 full cycle ~ 6 minutes)
    val timeInc = (dt / 15f)
    var newTime = _timeOfDay.value + timeInc
    if (newTime >= 24f) newTime -= 24f
    _timeOfDay.value = newTime

    // Auto headlights at night if user hasn't explicitly set
    if ((newTime < 5.5f || newTime > 19.5f) && !_headlightsOn.value) {
      _headlightsOn.value = true
    }

    // 2. Update survival stats (slowly drain hunger & thirst)
    _thirst.value = (_thirst.value - (dt * 0.12f)).coerceAtLeast(0f)
    _hunger.value = (_hunger.value - (dt * 0.08f)).coerceAtLeast(0f)
    if (_thirst.value <= 0f || _hunger.value <= 0f) {
      _health.value = (_health.value - (dt * 2f)).coerceAtLeast(0f)
    }

    // 3. Update Driving or On-Foot
    if (_playerMode.value == PlayerMode.DRIVING) {
      updateDriving(dt)
    } else {
      updateOnFoot(dt)
    }

    // 4. Update Zombies
    updateZombies(dt)

    // 5. Update Procedural Chunks & 100km Outpost Checks
    updateWorldGeneration()

    // 6. Update Camera Matrices
    updateCamera()
  }

  private fun updateDriving(dt: Float) {
    _maxFuel.value = upgrades.maxFuelCapacity

    // Steering
    val targetSteer = steerInput * 35f
    steerAngle += (targetSteer - steerAngle) * (dt * 8f)

    // Off-road check (|x| > 4.2m on the dirt shoulder)
    val isOffRoad = kotlin.math.abs(carState.x) > 4.2f
    val traction = if (isOffRoad) upgrades.offroadTraction else 1.0f

    // Acceleration & Braking
    if (_fuel.value > 0f) {
      if (throttleInput > 0f) {
        val accel = upgrades.accelerationRate * throttleInput * traction
        carState.speedKmh += accel * dt
        if (carState.speedKmh > upgrades.maxSpeedKmh) {
          carState.speedKmh = upgrades.maxSpeedKmh
        }
        // Fuel consumption
        val burn = (0.35f + (carState.speedKmh * 0.015f)) * dt
        _fuel.value = (_fuel.value - burn).coerceAtLeast(0f)
      } else if (throttleInput < 0f) {
        // Reverse or Brake
        if (carState.speedKmh > 5f) {
          carState.speedKmh -= 75f * dt // hard braking
          if (carState.speedKmh < 0f) carState.speedKmh = 0f
          SoundManager.playBrakeSqueal()
        } else {
          // Reverse
          carState.speedKmh -= 18f * dt
          if (carState.speedKmh < -25f) carState.speedKmh = -25f
        }
      } else {
        // Natural friction coasting
        if (carState.speedKmh > 0f) {
          carState.speedKmh -= (if (isOffRoad) 25f else 12f) * dt
          if (carState.speedKmh < 0f) carState.speedKmh = 0f
        } else if (carState.speedKmh < 0f) {
          carState.speedKmh += 20f * dt
          if (carState.speedKmh > 0f) carState.speedKmh = 0f
        }
      }
    } else {
      // Out of gas
      if (carState.speedKmh > 0f) {
        carState.speedKmh -= 18f * dt
        if (carState.speedKmh < 0f) carState.speedKmh = 0f
      }
    }

    // Engine hum sound
    SoundManager.playEngineHum(kotlin.math.abs(carState.speedKmh))

    // Car displacement (moving forward along -Z)
    val speedMps = (carState.speedKmh / 3.6f)
    val deltaZ = speedMps * dt
    carState.z -= deltaZ

    // Lateral steering movement (turning)
    val turnFactor = (carState.speedKmh / 80f).coerceIn(-1f, 1f)
    carState.x += (steerAngle / 35f) * turnFactor * 12f * dt
    carState.yaw = steerAngle * 0.4f

    // Keep car roughly on terrain (clamp to -130m .. 130m)
    carState.x = carState.x.coerceIn(-130f, 130f)

    // Update Distance Traveled (in KM)
    val addedKm = (deltaZ / 1000f)
    if (addedKm > 0f) {
      _distanceKm.value += addedKm
    }

    // Check 100 KM Shop Outpost trigger
    val currentHundredKm = (_distanceKm.value / 100f).toInt()
    if (currentHundredKm > 0 && currentHundredKm > lastShopKmTriggered) {
      lastShopKmTriggered = currentHundredKm
      _isAtOutpost.value = true
      _notification.value = "¡PUESTO COMERCIAL KM ${currentHundredKm * 100} ALCANZADO!"
    }
  }

  private fun updateOnFoot(dt: Float) {
    SoundManager.stopEngine()

    // Move forward/strafe based on footYaw
    val rad = Math.toRadians(footYaw.toDouble())
    val forwardX = -sin(rad).toFloat()
    val forwardZ = -cos(rad).toFloat()
    val rightX = cos(rad).toFloat()
    val rightZ = -sin(rad).toFloat()

    val walkSpeed = 5.2f
    val moveX = (forwardX * moveForwardInput + rightX * moveStrafeInput) * walkSpeed * dt
    val moveZ = (forwardZ * moveForwardInput + rightZ * moveStrafeInput) * walkSpeed * dt

    footX += moveX
    footZ += moveZ

    // Gravity and jumping
    if (!isGrounded) {
      footVerticalVelocity -= 18f * dt
      footY += footVerticalVelocity * dt
      if (footY <= 1.65f) {
        footY = 1.65f
        footVerticalVelocity = 0f
        isGrounded = true
      }
    }

    // Check targeting nearby items on the ground
    var closestItem: WorldItemInstance? = null
    var closestDist = 3.8f

    for (item in visibleItems) {
      if (item.isPickedUp || item.isInTrunk) continue
      val dx = item.x - footX
      val dy = item.y - footY
      val dz = item.z - footZ
      val dist = sqrt(dx * dx + dy * dy + dz * dz)
      if (dist < closestDist) {
        closestDist = dist
        closestItem = item
      }
    }
    _targetedItem.value = closestItem

    // Check proximity to the car's open trunk (back of car is carState.z + 1.4f)
    val trunkX = carState.x
    val trunkZ = carState.z + 1.4f
    val distToTrunk = sqrt((trunkX - footX) * (trunkX - footX) + (trunkZ - footZ) * (trunkZ - footZ))
    _isNearCarTrunk.value = distToTrunk < 3.2f
  }

  fun jump() {
    if (_playerMode.value == PlayerMode.ON_FOOT && isGrounded) {
      footVerticalVelocity = 6.5f
      isGrounded = false
      SoundManager.playPunchWhoosh()
    }
  }

  fun togglePlayerMode() {
    if (_playerMode.value == PlayerMode.DRIVING) {
      // Exit car on foot (S key or button)
      // Car must be stopped or moving slow
      if (kotlin.math.abs(carState.speedKmh) > 25f) {
        _notification.value = "Frena el coche para poder salir a pie"
        return
      }
      carState.speedKmh = 0f
      SoundManager.playDoorClick()
      _playerMode.value = PlayerMode.ON_FOOT
      footX = carState.x - 1.8f
      footY = 1.65f
      footZ = carState.z
      footYaw = 90f
      footPitch = 0f
      _notification.value = "Explorando a pie. Coge objetos con la mano y llévalos al maletero."
    } else {
      // Enter car
      val distToCar = sqrt((carState.x - footX) * (carState.x - footX) + (carState.z - footZ) * (carState.z - footZ))
      if (distToCar < 4.5f) {
        SoundManager.playDoorClick()
        _playerMode.value = PlayerMode.DRIVING
        _notification.value = "Al volante del V8. ¡Pisa el acelerador!"
      } else {
        _notification.value = "Acércate al coche para subirte"
      }
    }
  }

  fun toggleCameraView() {
    _cameraMode.value = if (_cameraMode.value == CameraMode.FIRST_PERSON_COCKPIT) {
      CameraMode.THIRD_PERSON
    } else {
      CameraMode.FIRST_PERSON_COCKPIT
    }
  }

  fun toggleHeadlights() {
    _headlightsOn.value = !_headlightsOn.value
  }

  fun grabItemByHand() {
    if (_playerMode.value != PlayerMode.ON_FOOT) return

    if (_heldItem.value != null) {
      // Already carrying an item: drop or place in trunk
      if (_isNearCarTrunk.value) {
        dropItemIntoTrunk()
      } else {
        // Drop on ground
        val dropped = _heldItem.value!!
        dropped.x = footX
        dropped.y = 0.1f
        dropped.z = footZ - 1.0f
        dropped.isCarriedInHand = false
        dropped.isPickedUp = false
        _heldItem.value = null
        _notification.value = "Soltaste ${dropped.item.name} en el suelo"
      }
      return
    }

    val target = _targetedItem.value
    if (target != null) {
      target.isPickedUp = true
      target.isCarriedInHand = true
      _heldItem.value = target
      SoundManager.playItemChime()
      _notification.value = "¡Agarraste ${target.item.name}! Llévalo al maletero abierto."
    }
  }

  fun dropItemIntoTrunk() {
    val carried = _heldItem.value ?: return
    carried.isCarriedInHand = false
    carried.isInTrunk = true
    _heldItem.value = null

    val currentList = _trunkItems.value.toMutableList()
    currentList.add(carried.item)
    _trunkItems.value = currentList

    SoundManager.playItemChime()
    _notification.value = "¡${carried.item.name} asegurado en el maletero! (Valor: $${carried.item.value})"
  }

  fun addItemToTrunk(item: RelicItem) {
    val currentList = _trunkItems.value.toMutableList()
    currentList.add(item)
    _trunkItems.value = currentList
  }

  fun punchMelee() {
    if (_playerMode.value != PlayerMode.ON_FOOT) return
    SoundManager.playPunchWhoosh()

    // Find zombie in punch range
    for (z in visibleZombies) {
      if (!z.isAlive) continue
      val dx = z.x - footX
      val dz = z.z - footZ
      val dist = sqrt(dx * dx + dz * dz)
      if (dist < 2.4f) {
        z.hp -= 35f
        z.hitStun = 0.4f
        SoundManager.playZombieHit()
        if (z.hp <= 0f) {
          z.isAlive = false
          _money.value += z.bounty
          SoundManager.playCashChime()
          _notification.value = "¡Zombie aniquilado! +$${z.bounty}"
        } else {
          _notification.value = "¡Golpeaste al zombie! HP restante: ${z.hp.toInt()}"
        }
        break
      }
    }
  }

  fun useItemFromTrunk(item: RelicItem) {
    val currentList = _trunkItems.value.toMutableList()
    if (!currentList.remove(item)) return
    _trunkItems.value = currentList

    when (item.utilityType) {
      com.example.game.catalog.ItemUtilityType.FUEL -> {
        _fuel.value = (_fuel.value + item.utilityAmount).coerceAtMost(_maxFuel.value)
        _notification.value = "Repostaste +${item.utilityAmount}L de combustible"
      }
      com.example.game.catalog.ItemUtilityType.WATER -> {
        _thirst.value = (_thirst.value + item.utilityAmount).coerceAtMost(100f)
        _notification.value = "Bebiste agua (+${item.utilityAmount} Sed)"
      }
      com.example.game.catalog.ItemUtilityType.FOOD -> {
        _hunger.value = (_hunger.value + item.utilityAmount).coerceAtMost(100f)
        _notification.value = "Comiste ración (+${item.utilityAmount} Nutrición)"
      }
      com.example.game.catalog.ItemUtilityType.HEALTH -> {
        _health.value = (_health.value + item.utilityAmount).coerceAtMost(100f)
        _notification.value = "Usaste botiquín (+${item.utilityAmount} Salud)"
      }
      com.example.game.catalog.ItemUtilityType.OIL -> {
        _notification.value = "Motor lubricado. Menor consumo y mejor rendimiento."
      }
      com.example.game.catalog.ItemUtilityType.SCRAP -> {
        _notification.value = "Chapa y defensas reforzadas con chatarra."
      }
      com.example.game.catalog.ItemUtilityType.VALUABLE -> {
        _money.value += item.value
        SoundManager.playCashChime()
        _notification.value = "Vendiste ${item.name} por $${item.value}"
        return
      }
    }
    SoundManager.playItemChime()
  }

  fun sellItem(item: RelicItem) {
    val currentList = _trunkItems.value.toMutableList()
    if (currentList.remove(item)) {
      _trunkItems.value = currentList
      _money.value += item.value
      SoundManager.playCashChime()
      _notification.value = "Vendido: ${item.name} por +$${item.value}"
    }
  }

  fun buyUpgrade(key: String): Boolean {
    val (level, cost) = when (key) {
      "engine" -> Pair(upgrades.engineBlockLevel, com.example.game.model.UpgradePricing.getCost(upgrades.engineBlockLevel))
      "carburetor" -> Pair(upgrades.carburetorLevel, com.example.game.model.UpgradePricing.getCost(upgrades.carburetorLevel))
      "radiator" -> Pair(upgrades.radiatorLevel, com.example.game.model.UpgradePricing.getCost(upgrades.radiatorLevel))
      "fuelTank" -> Pair(upgrades.fuelTankLevel, com.example.game.model.UpgradePricing.getCost(upgrades.fuelTankLevel))
      "tires" -> Pair(upgrades.tiresLevel, com.example.game.model.UpgradePricing.getCost(upgrades.tiresLevel))
      else -> return false
    }

    if (cost == null || _money.value < cost) {
      _notification.value = "No tienes suficiente dinero ($${cost ?: 0})"
      return false
    }

    _money.value -= cost
    when (key) {
      "engine" -> upgrades.engineBlockLevel++
      "carburetor" -> upgrades.carburetorLevel++
      "radiator" -> upgrades.radiatorLevel++
      "fuelTank" -> {
        upgrades.fuelTankLevel++
        _maxFuel.value = upgrades.maxFuelCapacity
      }
      "tires" -> upgrades.tiresLevel++
    }
    SoundManager.playCashChime()
    _notification.value = "¡Mejora instalada con éxito!"
    return true
  }

  fun buyResource(type: String, cost: Int, amount: Float): Boolean {
    if (_money.value < cost) {
      _notification.value = "Dinero insuficiente para comprar $type"
      return false
    }
    _money.value -= cost
    when (type) {
      "gasolina" -> _fuel.value = (_fuel.value + amount).coerceAtMost(_maxFuel.value)
      "agua" -> _thirst.value = (_thirst.value + amount).coerceAtMost(100f)
      "comida" -> _hunger.value = (_hunger.value + amount).coerceAtMost(100f)
      "botiquin" -> _health.value = (_health.value + amount).coerceAtMost(100f)
    }
    SoundManager.playCashChime()
    _notification.value = "Compraste $type con éxito"
    return true
  }

  fun openShop() {
    _isShopOpen.value = true
  }

  fun closeShop() {
    _isShopOpen.value = false
  }

  fun clearNotification() {
    _notification.value = null
  }

  private fun updateZombies(dt: Float) {
    val targetX = if (_playerMode.value == PlayerMode.DRIVING) carState.x else footX
    val targetZ = if (_playerMode.value == PlayerMode.DRIVING) carState.z else footZ

    for (z in visibleZombies) {
      if (!z.isAlive) continue

      z.walkCycle += dt * 5f
      if (z.hitStun > 0f) {
        z.hitStun -= dt
        continue
      }

      val dx = targetX - z.x
      val dz = targetZ - z.z
      val dist = sqrt(dx * dx + dz * dz)

      // Zombies aggro within 45m
      if (dist < 45f && dist > 1.2f) {
        val speed = z.speed * dt
        z.x += (dx / dist) * speed
        z.z += (dz / dist) * speed
      }

      // Check collision with moving car
      if (_playerMode.value == PlayerMode.DRIVING) {
        val carDx = z.x - carState.x
        val carDz = z.z - carState.z
        val carDist = sqrt(carDx * carDx + carDz * carDz)

        if (carDist < 2.5f) {
          if (kotlin.math.abs(carState.speedKmh) > 15f) {
            // CRUSH ZOMBIE WITH CAR!
            z.isAlive = false
            _money.value += z.bounty
            carState.speedKmh *= 0.88f // slight resistance
            SoundManager.playZombieHit()
            SoundManager.playCashChime()
            _notification.value = "¡ZOMBIE ATROPELLADO! +$${z.bounty}"
          } else {
            // Car stopped, zombie bangs on car
            SoundManager.playZombieGroan()
          }
        }
      } else {
        // Player on foot: Zombie attacks player!
        if (dist < 1.5f) {
          _health.value = (_health.value - (z.damage * dt)).coerceAtLeast(0f)
          SoundManager.playZombieHit()
        }
      }
    }
  }

  private fun updateWorldGeneration() {
    val playerZ = playerZ
    if (playerZ < lastChunkZ - 120f) {
      generateChunksAhead(playerZ - 180f)
      recycleOldChunks(playerZ + 80f)
    }
  }

  private fun generateChunksAhead(targetZ: Float) {
    val random = Random(lastChunkZ.toLong())
    var currentZ = lastChunkZ

    while (currentZ > targetZ) {
      currentZ -= 45f

      // Generate Roadside Props (Rocks, Cacti, Wrecked Cars)
      val propCount = random.nextInt(2, 6)
      for (i in 0 until propCount) {
        val isLeft = random.nextBoolean()
        val side = if (isLeft) -1f else 1f
        val px = (random.nextFloat() * 25f + 6.5f) * side
        val pz = currentZ + (random.nextFloat() * 30f - 15f)
        val ptype = random.nextInt(3) // 0: rock, 1: cactus, 2: wrecked car
        val scale = if (ptype == 0) random.nextFloat() * 1.2f + 0.8f else random.nextFloat() * 0.6f + 0.9f
        visibleProps.add(PropData(ptype, px, 0f, pz, random.nextFloat() * 360f, scale))
      }

      // Check if this chunk is near a 100km multiple for the Trading Post!
      // In our distance scaling, 100km corresponds to roughly every 4500-6000 world units or distance marks
      val approxKm = kotlin.math.abs(currentZ) / 100f
      val is100kmOutpost = (approxKm > 10f && approxKm.toInt() % 100 == 0)

      if (is100kmOutpost) {
        visibleStructures.add(
          WorldStructure(
            type = StructureType.OUTPOST_100KM,
            x = 8.0f,
            y = 0f,
            z = currentZ,
            rotation = 0f
          )
        )
      } else {
        // Randomly place an Abandoned House or Abandoned Museum
        val structRoll = random.nextFloat()
        if (structRoll < 0.28f) {
          // Abandoned House
          val side = if (random.nextBoolean()) -1f else 1f
          val sx = (random.nextFloat() * 10f + 16f) * side
          visibleStructures.add(
            WorldStructure(StructureType.HOUSE, sx, 0f, currentZ, 0f)
          )
          // Spawn 1-2 House relics
          spawnStructureItems(sx, currentZ, "Casa Abandonada", random)
        } else if (structRoll > 0.88f) {
          // Abandoned Museum
          val side = if (random.nextBoolean()) -1f else 1f
          val sx = (random.nextFloat() * 12f + 24f) * side
          visibleStructures.add(
            WorldStructure(StructureType.MUSEUM, sx, 0f, currentZ, 0f)
          )
          // Spawn rare Museum relics
          spawnStructureItems(sx, currentZ, "Museo Abandonado", random)
        }
      }

      // Spawn roadside relics on the shoulder
      if (random.nextFloat() < 0.35f) {
        val roadRelic = Catalog.ITEMS.filter { it.locationFound == "Carretera" }.random(random)
        val side = if (random.nextBoolean()) -1f else 1f
        val rx = (random.nextFloat() * 4f + 4.5f) * side
        visibleItems.add(
          WorldItemInstance(
            instanceId = instanceCounter++,
            item = roadRelic,
            x = rx,
            y = 0.2f,
            z = currentZ + random.nextFloat() * 20f
          )
        )
      }

      // Spawn Progressive Zombies (getting stronger with distance!)
      val currentDist = _distanceKm.value
      val zombieLevel = (1 + (currentDist / 20f).toInt()).coerceIn(1, 8)
      val zombieCount = random.nextInt(1, 1 + (zombieLevel.coerceAtMost(4)))

      for (z in 0 until zombieCount) {
        val side = if (random.nextBoolean()) -1f else 1f
        val zx = (random.nextFloat() * 15f + 4.5f) * side
        val zz = currentZ + (random.nextFloat() * 35f - 15f)

        val maxHp = 20f + (zombieLevel * 18f)
        val speed = 2.0f + (zombieLevel * 0.45f)
        val damage = 6.0f + (zombieLevel * 3.5f)
        val bounty = 12 + (zombieLevel * 14)
        val scale = 0.95f + (zombieLevel * 0.08f)

        visibleZombies.add(
          ZombieInstance(
            id = zombieIdCounter++,
            x = zx,
            y = 0f,
            z = zz,
            level = zombieLevel,
            maxHp = maxHp,
            hp = maxHp,
            speed = speed,
            damage = damage,
            bounty = bounty,
            scale = scale
          )
        )
      }
    }
    lastChunkZ = currentZ
  }

  private fun spawnStructureItems(sx: Float, sz: Float, location: String, random: Random) {
    val pool = Catalog.ITEMS.filter { it.locationFound == location }
    if (pool.isEmpty()) return
    val count = random.nextInt(1, 3)
    for (i in 0 until count) {
      val item = pool.random(random)
      val ix = sx + (random.nextFloat() * 4f - 2f)
      val iz = sz + (random.nextFloat() * 4f - 2f)
      visibleItems.add(
        WorldItemInstance(
          instanceId = instanceCounter++,
          item = item,
          x = ix,
          y = 0.2f,
          z = iz
        )
      )
    }
  }

  private fun recycleOldChunks(thresholdZ: Float) {
    visibleStructures.removeAll { it.z > thresholdZ }
    visibleProps.removeAll { it.z > thresholdZ }
    visibleItems.removeAll { it.z > thresholdZ && !it.isInTrunk && !it.isCarriedInHand }
    visibleZombies.removeAll { it.z > thresholdZ }
  }

  private fun updateCamera() {
    if (_playerMode.value == PlayerMode.DRIVING) {
      if (_cameraMode.value == CameraMode.FIRST_PERSON_COCKPIT) {
        // Cockpit Eye view inside driver seat
        cameraPosition[0] = carState.x - 0.35f
        cameraPosition[1] = carState.y + 1.22f
        cameraPosition[2] = carState.z - 0.25f

        val rad = Math.toRadians(carState.yaw.toDouble())
        cameraLookAt[0] = cameraPosition[0] - sin(rad).toFloat() * 15f
        cameraLookAt[1] = cameraPosition[1] - 0.05f
        cameraLookAt[2] = cameraPosition[2] - cos(rad).toFloat() * 15f
      } else {
        // 3rd Person chase camera behind car
        val rad = Math.toRadians(carState.yaw.toDouble())
        cameraPosition[0] = carState.x + sin(rad).toFloat() * 7.5f
        cameraPosition[1] = carState.y + 3.2f
        cameraPosition[2] = carState.z + cos(rad).toFloat() * 7.5f

        cameraLookAt[0] = carState.x
        cameraLookAt[1] = carState.y + 1.2f
        cameraLookAt[2] = carState.z - 4f
      }
    } else {
      // 1st Person On-Foot exploration
      cameraPosition[0] = footX
      cameraPosition[1] = footY
      cameraPosition[2] = footZ

      val yawRad = Math.toRadians(footYaw.toDouble())
      val pitchRad = Math.toRadians(footPitch.toDouble())

      val forwardX = -sin(yawRad).toFloat() * cos(pitchRad).toFloat()
      val forwardY = sin(pitchRad).toFloat()
      val forwardZ = -cos(yawRad).toFloat() * cos(pitchRad).toFloat()

      cameraLookAt[0] = footX + forwardX * 10f
      cameraLookAt[1] = footY + forwardY * 10f
      cameraLookAt[2] = footZ + forwardZ * 10f
    }
  }
}
