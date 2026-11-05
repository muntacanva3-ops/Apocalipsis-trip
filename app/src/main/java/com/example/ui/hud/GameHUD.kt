package com.example.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.model.CameraMode
import com.example.game.model.PlayerMode
import com.example.ui.theme.WastelandDarkBg
import com.example.ui.theme.WastelandGold
import com.example.ui.theme.WastelandGreen
import com.example.ui.theme.WastelandOrange
import com.example.ui.theme.WastelandOrangeDark
import com.example.ui.theme.WastelandRed
import com.example.ui.theme.WastelandSurface
import com.example.ui.theme.WastelandText
import kotlinx.coroutines.delay

@Composable
fun GameHUD(
  engine: GameEngine,
  isMobileControlMode: Boolean,
  onToggleControlMode: () -> Unit,
  onOpenInventory: () -> Unit,
  modifier: Modifier = Modifier
) {
  val distanceKm by engine.distanceKm.collectAsState()
  val fuel by engine.fuel.collectAsState()
  val maxFuel by engine.maxFuel.collectAsState()
  val money by engine.money.collectAsState()
  val health by engine.health.collectAsState()
  val hunger by engine.hunger.collectAsState()
  val thirst by engine.thirst.collectAsState()
  val timeOfDay by engine.timeOfDay.collectAsState()
  val headlightsOn by engine.headlightsOn.collectAsState()
  val playerMode by engine.playerMode.collectAsState()
  val cameraMode by engine.cameraMode.collectAsState()
  val notification by engine.notification.collectAsState()
  val isAtOutpost by engine.isAtOutpost.collectAsState()
  val trunkItems by engine.trunkItems.collectAsState()

  // Auto clear notification after 3.5s
  LaunchedEffect(notification) {
    if (notification != null) {
      delay(3500)
      engine.clearNotification()
    }
  }

  Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
    // TOP BAR: Distance, Time, Money, Mode & Quick Actions
    Column(
      modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Distance & Speed
        Surface(
          color = WastelandSurface.copy(alpha = 0.88f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, WastelandOrange.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Navigation,
              contentDescription = "Distancia",
              tint = WastelandOrange,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "KM ${String.format("%.1f", distanceKm)}",
              color = WastelandGold,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            if (playerMode == PlayerMode.DRIVING) {
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "${engine.carState.speedKmh.toInt()} km/h",
                color = WastelandText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Time of Day & Sky
        val hour = timeOfDay.toInt()
        val minute = ((timeOfDay - hour) * 60).toInt()
        val timeLabel = when {
          timeOfDay in 5.5f..8.5f -> "🌅 Amanecer"
          timeOfDay in 8.5f..17.0f -> "☀️ Día"
          timeOfDay in 17.0f..20.0f -> "🌆 Atardecer"
          else -> "🌙 Noche"
        }

        Surface(
          color = WastelandSurface.copy(alpha = 0.88f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = "${String.format("%02d:%02d", hour, minute)} $timeLabel",
            color = WastelandText,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }

        // Cash Money
        Surface(
          color = WastelandSurface.copy(alpha = 0.88f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, WastelandGreen.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "$",
              color = WastelandGreen,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$money",
              color = WastelandText,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Quick Tools: Headlights, View, Mode, Trunk
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Headlights
          IconButton(
            onClick = { engine.toggleHeadlights() },
            modifier = Modifier.size(36.dp).background(
              if (headlightsOn) WastelandOrange else WastelandSurface,
              CircleShape
            ).testTag("headlights_button")
          ) {
            Icon(
              imageVector = Icons.Default.Highlight,
              contentDescription = "Faros",
              tint = if (headlightsOn) WastelandDarkBg else WastelandText,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Camera View
          IconButton(
            onClick = { engine.toggleCameraView() },
            modifier = Modifier.size(36.dp).background(WastelandSurface, CircleShape)
              .testTag("camera_view_button")
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = "Cambiar Cámara",
              tint = if (cameraMode == CameraMode.FIRST_PERSON_COCKPIT) WastelandGold else WastelandText,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Inventory / Trunk
          IconButton(
            onClick = onOpenInventory,
            modifier = Modifier.size(36.dp).background(WastelandSurface, CircleShape)
              .border(1.dp, WastelandGold, CircleShape)
              .testTag("inventory_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Maletero e Inventario",
                tint = WastelandGold,
                modifier = Modifier.size(18.dp)
              )
              if (trunkItems.isNotEmpty()) {
                Box(
                  modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .background(WastelandOrange, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${trunkItems.size}",
                    color = WastelandDarkBg,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // SECOND ROW: Fuel Gauge & Survival Bars (Salud, Hambre, Sed)
      Surface(
        color = WastelandSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Fuel meter
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.3f)) {
            Icon(
              imageVector = Icons.Default.LocalGasStation,
              contentDescription = "Gasolina",
              tint = if (fuel < 10f) WastelandRed else WastelandGold,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Combustible: ${fuel.toInt()}/${maxFuel.toInt()}L",
                color = if (fuel < 10f) WastelandRed else WastelandText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(2.dp))
              LinearProgressIndicator(
                progress = { (fuel / maxFuel).coerceIn(0f, 1f) },
                color = if (fuel < 10f) WastelandRed else WastelandGold,
                trackColor = Color.DarkGray,
                modifier = Modifier.fillMaxWidth().height(6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(16.dp))

          // HP bar
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = "Salud",
              tint = WastelandRed,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "HP ${health.toInt()}", color = WastelandText, fontSize = 11.sp)
              LinearProgressIndicator(
                progress = { (health / 100f).coerceIn(0f, 1f) },
                color = WastelandRed,
                trackColor = Color.DarkGray,
                modifier = Modifier.fillMaxWidth().height(5.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          // Food & Water icons
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Fastfood,
              contentDescription = "Hambre",
              tint = if (hunger < 25f) WastelandRed else WastelandGreen,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "${hunger.toInt()}%",
              color = WastelandText,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.Opacity,
              contentDescription = "Sed",
              tint = if (thirst < 25f) WastelandRed else Color(0xFF29B6F6),
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "${thirst.toInt()}%",
              color = WastelandText,
              fontSize = 11.sp,
              modifier = Modifier.padding(start = 4.dp)
            )
          }
        }
      }

      // OUTPOST / SHOP PROMPT EVERY 100 KM
      if (isAtOutpost) {
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = { engine.openShop() },
          colors = ButtonDefaults.buttonColors(containerColor = WastelandOrange),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(0.9f).testTag("open_shop_banner")
        ) {
          Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = WastelandDarkBg)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "🛒 TIENDA Y TALLER MECÁNICO (KM ${(distanceKm / 100f).toInt() * 100})",
            color = WastelandDarkBg,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }

      // Notification banner
      AnimatedVisibility(
        visible = notification != null,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        if (notification != null) {
          Surface(
            color = WastelandOrangeDark.copy(alpha = 0.95f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 8.dp)
          ) {
            Text(
              text = notification!!,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }
        }
      }
    }

    // Controls Mode Switcher (top right corner below bar)
    Surface(
      color = WastelandSurface.copy(alpha = 0.85f),
      shape = RoundedCornerShape(8.dp),
      border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.Gray),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 110.dp)
    ) {
      Button(
        onClick = onToggleControlMode,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(32.dp).testTag("toggle_control_mode_button")
      ) {
        Text(
          text = if (isMobileControlMode) "📱 Móvil (Joysticks)" else "💻 PC (Teclado)",
          color = WastelandGold,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
