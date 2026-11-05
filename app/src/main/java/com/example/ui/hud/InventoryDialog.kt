package com.example.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.catalog.Catalog
import com.example.game.catalog.ItemCategory
import com.example.game.catalog.RelicItem
import com.example.game.engine.GameEngine
import com.example.ui.theme.WastelandDarkBg
import com.example.ui.theme.WastelandGold
import com.example.ui.theme.WastelandGreen
import com.example.ui.theme.WastelandOrange
import com.example.ui.theme.WastelandSurface
import com.example.ui.theme.WastelandSurfaceVariant
import com.example.ui.theme.WastelandText
import com.example.ui.theme.WastelandTextSecondary

@Composable
fun InventoryDialog(
  engine: GameEngine,
  onDismiss: () -> Unit
) {
  val trunkItems by engine.trunkItems.collectAsState()
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabTitles = listOf("Maletero (${trunkItems.size})", "Enciclopedia 30 Objetos")

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.88f)
        .padding(8.dp)
        .testTag("inventory_modal"),
      colors = CardDefaults.cardColors(containerColor = WastelandDarkBg),
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, WastelandOrange)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Modal Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DirectionsCar,
              contentDescription = null,
              tint = WastelandOrange,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "MALETERO ABIERTO & RELIQUIAS",
                color = WastelandText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Coche V8 sin compuerta de maletero",
                color = WastelandTextSecondary,
                fontSize = 11.sp
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_inventory_button")) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = WastelandText)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = WastelandSurface,
          contentColor = WastelandOrange,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = WastelandOrange
            )
          }
        ) {
          tabTitles.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  color = if (selectedTab == index) WastelandOrange else WastelandTextSecondary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
          // TRUNK CONTENTS (Items loaded in open trunk)
          if (trunkItems.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxWidth().weight(1f),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "El maletero está vacío",
                  color = WastelandTextSecondary,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Bájate del coche [S], agarra objetos del suelo y acércalos a la parte trasera.",
                  color = Color.Gray,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 24.dp)
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(trunkItems) { item ->
                TrunkItemCard(
                  item = item,
                  onUse = { engine.useItemFromTrunk(item) },
                  onSell = { engine.sellItem(item) }
                )
              }
            }
          }
        } else {
          // ALL 30 RELICS ENCYCLOPEDIA
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(Catalog.ITEMS) { relic ->
              val inTrunk = trunkItems.any { it.id == relic.id }
              EncyclopediaItemCard(relic = relic, isOwned = inTrunk)
            }
          }
        }
      }
    }
  }
}

@Composable
fun TrunkItemCard(
  item: RelicItem,
  onUse: () -> Unit,
  onSell: () -> Unit
) {
  Surface(
    color = WastelandSurfaceVariant,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, WastelandOrange.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Color icon marker
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(item.colorHex)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "#${item.id}",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.name,
          color = WastelandText,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${item.category.label} • Valor: $${item.value}",
          color = WastelandGold,
          fontSize = 11.sp
        )
        Text(
          text = item.description,
          color = WastelandTextSecondary,
          fontSize = 10.sp,
          maxLines = 1
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
          onClick = onUse,
          colors = ButtonDefaults.buttonColors(containerColor = WastelandGreen),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Text(text = "USAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WastelandDarkBg)
        }
        Button(
          onClick = onSell,
          colors = ButtonDefaults.buttonColors(containerColor = WastelandOrange),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Text(text = "$${item.value}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WastelandDarkBg)
        }
      }
    }
  }
}

@Composable
fun EncyclopediaItemCard(relic: RelicItem, isOwned: Boolean) {
  Surface(
    color = if (isOwned) WastelandSurfaceVariant else WastelandSurface.copy(alpha = 0.5f),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(
      0.8.dp,
      if (isOwned) WastelandGold else Color.DarkGray
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(Color(relic.colorHex).copy(alpha = if (isOwned) 1f else 0.4f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "${relic.id}",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = relic.name,
            color = if (isOwned) WastelandText else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          if (isOwned) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = WastelandGreen,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "EN MALETERO",
                color = WastelandDarkBg,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = "Hallazgo: ${relic.locationFound} | Tasación: $${relic.value}",
          color = WastelandGold.copy(alpha = if (isOwned) 1f else 0.6f),
          fontSize = 10.sp
        )
        Text(
          text = relic.description,
          color = WastelandTextSecondary.copy(alpha = if (isOwned) 1f else 0.5f),
          fontSize = 9.sp
        )
      }
    }
  }
}
