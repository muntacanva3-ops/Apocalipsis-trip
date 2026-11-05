package com.example.ui.hud

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WastelandDarkBg
import com.example.ui.theme.WastelandGold
import com.example.ui.theme.WastelandGreen
import com.example.ui.theme.WastelandOrange
import com.example.ui.theme.WastelandOrangeDark
import com.example.ui.theme.WastelandSurface
import com.example.ui.theme.WastelandSurfaceVariant
import com.example.ui.theme.WastelandText
import com.example.ui.theme.WastelandTextSecondary

@Composable
fun TitleScreen(
  isMobileControlMode: Boolean,
  onToggleControlMode: () -> Unit,
  onStartGame: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            WastelandDarkBg,
            WastelandSurface,
            WastelandOrangeDark.copy(alpha = 0.5f),
            WastelandDarkBg
          )
        )
      )
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      colors = CardDefaults.cardColors(containerColor = WastelandSurface.copy(alpha = 0.95f)),
      shape = RoundedCornerShape(20.dp),
      border = androidx.compose.foundation.BorderStroke(2.dp, WastelandOrange)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App Icon / Logo
        Surface(
          color = WastelandOrangeDark,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.size(68.dp),
          border = androidx.compose.foundation.BorderStroke(2.dp, WastelandGold)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.DirectionsCar,
              contentDescription = null,
              tint = WastelandGold,
              modifier = Modifier.size(40.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "APOCALIPSIS TRIP",
          color = WastelandGold,
          fontSize = 26.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          textAlign = TextAlign.Center
        )

        Text(
          text = "SIMULADOR 3D DE SUPERVIVENCIA EN CARRETERA",
          color = WastelandOrange,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feature Pills
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          FeatureRow(
            icon = Icons.Default.DirectionsCar,
            title = "V8 Destartalado & Maletero Abierto",
            desc = "Cabina en 1ª persona con agujas funcionales, volante móvil y maletero trasero sin compuerta."
          )
          FeatureRow(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            title = "Salida a Pie [S] y Agarre a Mano",
            desc = "Bájate a explorar casas y museos en ruinas. Coge objetos con la mano y arrástralos al maletero."
          )
          FeatureRow(
            icon = Icons.Default.Explore,
            title = "30 Reliquias & Tienda cada 100 KM",
            desc = "Vende tus hallazgos, compra gasolina y mejora 5 piezas del motor en los talleres de la ruta."
          )
          FeatureRow(
            icon = Icons.Default.Security,
            title = "Zombies Progresivos según Distancia",
            desc = "Los caminantes se vuelven más fuertes y peligrosos conforme avanzas kilómetros."
          )
          FeatureRow(
            icon = Icons.Default.FlashOn,
            title = "Ciclo Dinámico Día / Noche & Faros",
            desc = "Transición suave de amanecer, mediodía, atardecer ardiente y noche oscura iluminada por faros."
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Control mode selector
        Surface(
          color = WastelandSurfaceVariant,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, WastelandGold.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Modo de Controles", color = WastelandText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              Text(
                text = if (isMobileControlMode) "Móvil: Joystick + Botones" else "PC: Teclas W/A/S/D + Ratón",
                color = WastelandTextSecondary,
                fontSize = 10.sp
              )
            }
            Button(
              onClick = onToggleControlMode,
              colors = ButtonDefaults.buttonColors(containerColor = WastelandOrange),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Text(
                text = if (isMobileControlMode) "Cambiar a PC" else "Cambiar a Móvil",
                color = WastelandDarkBg,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start Button
        Button(
          onClick = onStartGame,
          colors = ButtonDefaults.buttonColors(containerColor = WastelandOrange),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("start_game_button")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = WastelandDarkBg)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "ARRANCAR MOTOR & PARTIR",
            color = WastelandDarkBg,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
          )
        }
      }
    }
  }
}

@Composable
fun FeatureRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  desc: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(WastelandSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = WastelandOrange,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(text = title, color = WastelandText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
      Text(text = desc, color = WastelandTextSecondary, fontSize = 9.5.sp)
    }
  }
}
