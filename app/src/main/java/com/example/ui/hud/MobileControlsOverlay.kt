package com.example.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.model.PlayerMode
import com.example.ui.theme.WastelandDarkBg
import com.example.ui.theme.WastelandGold
import com.example.ui.theme.WastelandGreen
import com.example.ui.theme.WastelandOrange
import com.example.ui.theme.WastelandOrangeDark
import com.example.ui.theme.WastelandRed
import com.example.ui.theme.WastelandSurface
import com.example.ui.theme.WastelandText
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun MobileControlsOverlay(
  engine: GameEngine,
  isMobileControlMode: Boolean,
  modifier: Modifier = Modifier
) {
  val playerMode by engine.playerMode.collectAsState()
  val heldItem by engine.heldItem.collectAsState()
  val targetedItem by engine.targetedItem.collectAsState()
  val isNearTrunk by engine.isNearCarTrunk.collectAsState()

  // Virtual joystick state
  var joystickKnobX by remember { mutableStateOf(0f) }
  var joystickKnobY by remember { mutableStateOf(0f) }
  val maxRadius = 55f

  Box(modifier = modifier.fillMaxSize()) {
    // Touch look surface for 360 degree camera panning on foot
    if (playerMode == PlayerMode.ON_FOOT) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              engine.footYaw += dragAmount.x * 0.35f
              engine.footPitch = (engine.footPitch - dragAmount.y * 0.25f).coerceIn(-65f, 65f)
            }
          }
      )
    }

    if (isMobileControlMode) {
      // LEFT SIDE: VIRTUAL JOYSTICK (WASD)
      Box(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 24.dp, bottom = 28.dp)
          .size(140.dp)
          .clip(CircleShape)
          .background(WastelandSurface.copy(alpha = 0.55f))
          .border(2.dp, WastelandOrange.copy(alpha = 0.7f), CircleShape)
          .pointerInput(playerMode) {
            detectDragGestures(
              onDragStart = {},
              onDragEnd = {
                joystickKnobX = 0f
                joystickKnobY = 0f
                if (playerMode == PlayerMode.DRIVING) {
                  engine.steerInput = 0f
                  engine.throttleInput = 0f
                } else {
                  engine.moveForwardInput = 0f
                  engine.moveStrafeInput = 0f
                }
              },
              onDragCancel = {
                joystickKnobX = 0f
                joystickKnobY = 0f
                if (playerMode == PlayerMode.DRIVING) {
                  engine.steerInput = 0f
                  engine.throttleInput = 0f
                } else {
                  engine.moveForwardInput = 0f
                  engine.moveStrafeInput = 0f
                }
              }
            ) { change, dragAmount ->
              change.consume()
              val newX = joystickKnobX + dragAmount.x
              val newY = joystickKnobY + dragAmount.y
              val dist = sqrt(newX * newX + newY * newY)
              if (dist <= maxRadius) {
                joystickKnobX = newX
                joystickKnobY = newY
              } else {
                joystickKnobX = (newX / dist) * maxRadius
                joystickKnobY = (newY / dist) * maxRadius
              }

              val normX = (joystickKnobX / maxRadius).coerceIn(-1f, 1f)
              val normY = (joystickKnobY / maxRadius).coerceIn(-1f, 1f)

              if (playerMode == PlayerMode.DRIVING) {
                engine.steerInput = normX
                // Up is gas (-Y), Down is reverse (+Y)
                engine.throttleInput = -normY
              } else {
                // On foot
                engine.moveStrafeInput = normX
                engine.moveForwardInput = -normY
              }
            }
          },
        contentAlignment = Alignment.Center
      ) {
        // Center knob
        Box(
          modifier = Modifier
            .offset { IntOffset(joystickKnobX.roundToInt(), joystickKnobY.roundToInt()) }
            .size(54.dp)
            .clip(CircleShape)
            .background(WastelandOrange)
            .border(2.dp, WastelandGold, CircleShape)
        )
      }

      // RIGHT SIDE: ACTION BUTTONS
      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (playerMode == PlayerMode.DRIVING) {
          // DRIVING ACTIONS
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // BRAKE / REVERSE BUTTON
            Button(
              onClick = {},
              modifier = Modifier
                .size(72.dp)
                .pointerInput(Unit) {
                  detectDragGestures(
                    onDragStart = { engine.throttleInput = -1.0f },
                    onDragEnd = { engine.throttleInput = 0f },
                    onDragCancel = { engine.throttleInput = 0f }
                  ) { change, _ -> change.consume() }
                }
                .testTag("brake_button"),
              colors = ButtonDefaults.buttonColors(containerColor = WastelandRed.copy(alpha = 0.85f)),
              shape = CircleShape
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Freno", tint = Color.White)
                Text(text = "FRENO", fontSize = 9.sp, fontWeight = FontWeight.Bold)
              }
            }

            // ACCELERATE / GAS BUTTON
            Button(
              onClick = {},
              modifier = Modifier
                .size(76.dp)
                .pointerInput(Unit) {
                  detectDragGestures(
                    onDragStart = { engine.throttleInput = 1.0f },
                    onDragEnd = { engine.throttleInput = 0f },
                    onDragCancel = { engine.throttleInput = 0f }
                  ) { change, _ -> change.consume() }
                }
                .testTag("gas_button"),
              colors = ButtonDefaults.buttonColors(containerColor = WastelandGreen.copy(alpha = 0.85f)),
              shape = CircleShape
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Gas", tint = Color.White)
                Text(text = "GAS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          // EXIT CAR ON FOOT [S] BUTTON
          Button(
            onClick = { engine.togglePlayerMode() },
            colors = ButtonDefaults.buttonColors(containerColor = WastelandOrangeDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(44.dp).testTag("exit_car_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = WastelandText)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Bajar a Pie [S]",
              color = WastelandText,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        } else {
          // ON-FOOT ACTIONS (Salir a pie, Agarrar a Mano, Saltar, Golpear, Subir)
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // JUMP BUTTON
            Button(
              onClick = { engine.jump() },
              colors = ButtonDefaults.buttonColors(containerColor = WastelandSurface),
              shape = CircleShape,
              modifier = Modifier.size(60.dp).border(1.dp, WastelandGold, CircleShape)
                .testTag("jump_button")
            ) {
              Text(text = "SALTAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WastelandGold)
            }

            // PUNCH ZOMBIE [F] BUTTON
            Button(
              onClick = { engine.punchMelee() },
              colors = ButtonDefaults.buttonColors(containerColor = WastelandRed),
              shape = CircleShape,
              modifier = Modifier.size(64.dp).testTag("punch_button")
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SportsMma, contentDescription = "Golpear", tint = Color.White)
                Text(text = "GOLPEAR", fontSize = 8.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          // HAND GRAB & DROP TO TRUNK BUTTON
          val canGrab = targetedItem != null
          val isHolding = heldItem != null
          val buttonBg = when {
            isHolding && isNearTrunk -> WastelandGreen
            isHolding -> WastelandGold
            canGrab -> WastelandOrange
            else -> WastelandSurface
          }

          val buttonText = when {
            isHolding && isNearTrunk -> "¡METER AL MALETERO!"
            isHolding -> "Soltar al Suelo"
            canGrab -> "Agarrar ${targetedItem!!.item.name.take(12)}..."
            else -> "Mano Vacía"
          }

          Button(
            onClick = { engine.grabItemByHand() },
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .height(48.dp)
              .border(1.5.dp, if (isHolding) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
              .testTag("grab_hand_button")
          ) {
            Icon(Icons.Default.PanTool, contentDescription = null, tint = WastelandDarkBg)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = buttonText,
              color = WastelandDarkBg,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }

          // RE-ENTER CAR BUTTON
          Button(
            onClick = { engine.togglePlayerMode() },
            colors = ButtonDefaults.buttonColors(containerColor = WastelandOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(42.dp).testTag("enter_car_button")
          ) {
            Icon(Icons.Default.DriveEta, contentDescription = null, tint = WastelandDarkBg)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Subir al Coche [S]",
              color = WastelandDarkBg,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }
    }
  }
}
