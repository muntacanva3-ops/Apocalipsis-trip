package com.example

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.GameEngine
import com.example.game.gl.WastelandRenderer
import com.example.game.model.PlayerMode
import com.example.ui.hud.GameHUD
import com.example.ui.hud.InventoryDialog
import com.example.ui.hud.MobileControlsOverlay
import com.example.ui.hud.ShopDialog
import com.example.ui.hud.TitleScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val engine = GameEngine()
  private var isMobileModeState = mutableStateOf(true)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          GameApp(
            engine = engine,
            isMobileMode = isMobileModeState.value,
            onToggleMobileMode = { isMobileModeState.value = !isMobileModeState.value },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_W -> {
        if (engine.playerMode.value == PlayerMode.DRIVING) {
          engine.throttleInput = 1.0f
        } else {
          engine.moveForwardInput = 1.0f
        }
        return true
      }
      KeyEvent.KEYCODE_S -> {
        if (engine.playerMode.value == PlayerMode.DRIVING) {
          // If stopped, S exits the car; if moving, S acts as brake/reverse
          if (kotlin.math.abs(engine.carState.speedKmh) < 5f) {
            engine.togglePlayerMode()
          } else {
            engine.throttleInput = -1.0f
          }
        } else {
          engine.moveForwardInput = -1.0f
        }
        return true
      }
      KeyEvent.KEYCODE_A -> {
        if (engine.playerMode.value == PlayerMode.DRIVING) {
          engine.steerInput = -1.0f
        } else {
          engine.moveStrafeInput = -1.0f
        }
        return true
      }
      KeyEvent.KEYCODE_D -> {
        if (engine.playerMode.value == PlayerMode.DRIVING) {
          engine.steerInput = 1.0f
        } else {
          engine.moveStrafeInput = 1.0f
        }
        return true
      }
      KeyEvent.KEYCODE_SPACE -> {
        if (engine.playerMode.value == PlayerMode.ON_FOOT) {
          engine.jump()
        }
        return true
      }
      KeyEvent.KEYCODE_F -> {
        engine.punchMelee()
        return true
      }
      KeyEvent.KEYCODE_E -> {
        engine.grabItemByHand()
        return true
      }
      KeyEvent.KEYCODE_L -> {
        engine.toggleHeadlights()
        return true
      }
      KeyEvent.KEYCODE_V -> {
        engine.toggleCameraView()
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_W -> {
        if (engine.playerMode.value == PlayerMode.DRIVING) {
          if (engine.throttleInput > 0f) engine.throttleInput = 0f
        } else {
          engine.moveForwardInput = 0f
        }
        return true
      }
      KeyEvent.KEYCODE_S -> {
        if (engine.playerMode.value == PlayerMode.DRIVING) {
          if (engine.throttleInput < 0f) engine.throttleInput = 0f
        } else {
          engine.moveForwardInput = 0f
        }
        return true
      }
      KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D -> {
        if (engine.playerMode.value == PlayerMode.DRIVING) {
          engine.steerInput = 0f
        } else {
          engine.moveStrafeInput = 0f
        }
        return true
      }
    }
    return super.onKeyUp(keyCode, event)
  }
}

@Composable
fun GameApp(
  engine: GameEngine,
  isMobileMode: Boolean,
  onToggleMobileMode: () -> Unit,
  modifier: Modifier = Modifier
) {
  var hasStartedGame by remember { mutableStateOf(false) }
  var isInventoryOpen by remember { mutableStateOf(false) }
  val isShopOpen by engine.isShopOpen.collectAsState()

  Box(modifier = modifier) {
    // 1. OpenGL ES 2.0 3D Viewport
    AndroidView(
      factory = { ctx ->
        GLSurfaceView(ctx).apply {
          setEGLContextClientVersion(2)
          setRenderer(WastelandRenderer(ctx, engine))
          renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
      },
      modifier = Modifier.fillMaxSize()
    )

    // 2. Title Screen or Gameplay HUD
    if (!hasStartedGame) {
      TitleScreen(
        isMobileControlMode = isMobileMode,
        onToggleControlMode = onToggleMobileMode,
        onStartGame = { hasStartedGame = true }
      )
    } else {
      // IN-GAME HUD
      GameHUD(
        engine = engine,
        isMobileControlMode = isMobileMode,
        onToggleControlMode = onToggleMobileMode,
        onOpenInventory = { isInventoryOpen = true }
      )

      // MOBILE CONTROLS OVERLAY
      MobileControlsOverlay(
        engine = engine,
        isMobileControlMode = isMobileMode
      )

      // MODALS
      if (isInventoryOpen) {
        InventoryDialog(
          engine = engine,
          onDismiss = { isInventoryOpen = false }
        )
      }

      if (isShopOpen) {
        ShopDialog(
          engine = engine,
          onDismiss = { engine.closeShop() }
        )
      }
    }
  }
}
