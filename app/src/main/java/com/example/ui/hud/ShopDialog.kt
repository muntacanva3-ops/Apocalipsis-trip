package com.example.ui.hud

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.engine.GameEngine
import com.example.game.model.UpgradeInfo
import com.example.game.model.UpgradePricing
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
fun ShopDialog(
  engine: GameEngine,
  onDismiss: () -> Unit
) {
  val money by engine.money.collectAsState()
  val trunkItems by engine.trunkItems.collectAsState()
  val distanceKm by engine.distanceKm.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  val outpostKm = ((distanceKm / 100f).toInt() * 100).coerceAtLeast(100)

  val upgradesList = listOf(
    UpgradeInfo(
      key = "engine",
      name = "Bloque Motor V8 Pesado",
      description = "Aumenta la velocidad punta y par motor (actual: ${engine.upgrades.maxSpeedKmh.toInt()} km/h).",
      currentLevel = engine.upgrades.engineBlockLevel,
      nextCost = UpgradePricing.getCost(engine.upgrades.engineBlockLevel)
    ),
    UpgradeInfo(
      key = "carburetor",
      name = "Carburador Doble Cuerpo",
      description = "Mayor respuesta al pisar el acelerador y mejor aceleración.",
      currentLevel = engine.upgrades.carburetorLevel,
      nextCost = UpgradePricing.getCost(engine.upgrades.carburetorLevel)
    ),
    UpgradeInfo(
      key = "radiator",
      name = "Radiador de Cobre & Bomba de Aceite",
      description = "Reduce sobrecalentamiento y fricción de componentes mecánicos.",
      currentLevel = engine.upgrades.radiatorLevel,
      nextCost = UpgradePricing.getCost(engine.upgrades.radiatorLevel)
    ),
    UpgradeInfo(
      key = "fuelTank",
      name = "Depósito Blindado Extendido",
      description = "Expande la capacidad del tanque de gasolina (actual: ${engine.upgrades.maxFuelCapacity.toInt()}L).",
      currentLevel = engine.upgrades.fuelTankLevel,
      nextCost = UpgradePricing.getCost(engine.upgrades.fuelTankLevel)
    ),
    UpgradeInfo(
      key = "tires",
      name = "Neumáticos Todoterreno Reforzados",
      description = "Mejora sustancial de tracción y velocidad en la tierra de la cuneta.",
      currentLevel = engine.upgrades.tiresLevel,
      nextCost = UpgradePricing.getCost(engine.upgrades.tiresLevel)
    )
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.90f)
        .padding(8.dp)
        .testTag("shop_modal"),
      colors = CardDefaults.cardColors(containerColor = WastelandDarkBg),
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, WastelandGold)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, tint = WastelandGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "TALLER & TIENDA • KM $outpostKm",
                color = WastelandGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Puesto de intercambio postapocalíptico",
                color = WastelandTextSecondary,
                fontSize = 11.sp
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = WastelandSurface,
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, WastelandGreen)
            ) {
              Text(
                text = "$$money",
                color = WastelandGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_shop_button")) {
              Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = WastelandText)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        val tabTitles = listOf("Mejoras de Motor", "Vender Botín (${trunkItems.size})", "Suministros")
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = WastelandSurface,
          contentColor = WastelandGold,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = WastelandGold
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
                  color = if (selectedTab == index) WastelandGold else WastelandTextSecondary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
          0 -> {
            // MOTOR UPGRADES
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(upgradesList) { upgrade ->
                UpgradeCard(
                  upgrade = upgrade,
                  canAfford = (upgrade.nextCost != null) && (money >= upgrade.nextCost),
                  onBuy = { engine.buyUpgrade(upgrade.key) }
                )
              }
            }
          }
          1 -> {
            // SELL ITEMS FROM TRUNK
            if (trunkItems.isEmpty()) {
              Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(text = "No tienes objetos en el maletero para vender.", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Explora ruinas y casas para recolectar reliquias.", color = Color.DarkGray, fontSize = 11.sp)
              }
            } else {
              // Sell all button
              Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val totalValue = trunkItems.sumOf { it.value }
                Text(
                  text = "Valor Total: $$totalValue",
                  color = WastelandGold,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Button(
                  onClick = {
                    trunkItems.toList().forEach { engine.sellItem(it) }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = WastelandGold),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text(text = "VENDER TODO", color = WastelandDarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
              }

              LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(trunkItems) { item ->
                  Surface(
                    color = WastelandSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, color = WastelandText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Hallado en: ${item.locationFound}", color = WastelandTextSecondary, fontSize = 11.sp)
                      }
                      Button(
                        onClick = { engine.sellItem(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = WastelandGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                      ) {
                        Text(text = "+$${item.value}", color = WastelandDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      }
                    }
                  }
                }
              }
            }
          }
          2 -> {
            // BUY SURVIVAL RESOURCES
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              item {
                ResourceShopCard(
                  name = "Bidón de Combustible (20 Litros)",
                  description = "Restaura 20L al depósito del viejo V8.",
                  cost = 30,
                  canAfford = money >= 30,
                  onBuy = { engine.buyResource("gasolina", 30, 20f) }
                )
              }
              item {
                ResourceShopCard(
                  name = "Bidón de Agua Purificada (35L)",
                  description = "Satisface la sed del conductor (+35%).",
                  cost = 18,
                  canAfford = money >= 18,
                  onBuy = { engine.buyResource("agua", 18, 35f) }
                )
              }
              item {
                ResourceShopCard(
                  name = "Ración Militar Completa",
                  description = "Satisface el hambre del conductor (+35%).",
                  cost = 22,
                  canAfford = money >= 22,
                  onBuy = { engine.buyResource("comida", 22, 35f) }
                )
              }
              item {
                ResourceShopCard(
                  name = "Botiquín Médico Avanzado",
                  description = "Cura heridas y restaura salud (+50 HP).",
                  cost = 40,
                  canAfford = money >= 40,
                  onBuy = { engine.buyResource("botiquin", 40, 50f) }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun UpgradeCard(
  upgrade: UpgradeInfo,
  canAfford: Boolean,
  onBuy: () -> Unit
) {
  Surface(
    color = WastelandSurfaceVariant,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, WastelandGold.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = upgrade.name, color = WastelandText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = WastelandOrangeDark,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = "Nivel ${upgrade.currentLevel}/${upgrade.maxLevel}",
              color = WastelandText,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }
        }
        Text(text = upgrade.description, color = WastelandTextSecondary, fontSize = 11.sp)
      }

      Spacer(modifier = Modifier.width(8.dp))

      if (upgrade.nextCost != null) {
        Button(
          onClick = onBuy,
          enabled = canAfford,
          colors = ButtonDefaults.buttonColors(
            containerColor = WastelandGold,
            disabledContainerColor = Color.DarkGray
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Text(
            text = "$${upgrade.nextCost}",
            color = if (canAfford) WastelandDarkBg else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      } else {
        Text(text = "MÁXIMO", color = WastelandGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
      }
    }
  }
}

@Composable
fun ResourceShopCard(
  name: String,
  description: String,
  cost: Int,
  canAfford: Boolean,
  onBuy: () -> Unit
) {
  Surface(
    color = WastelandSurfaceVariant,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = name, color = WastelandText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text = description, color = WastelandTextSecondary, fontSize = 11.sp)
      }
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = onBuy,
        enabled = canAfford,
        colors = ButtonDefaults.buttonColors(
          containerColor = WastelandGreen,
          disabledContainerColor = Color.DarkGray
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(34.dp)
      ) {
        Text(
          text = "$$cost",
          color = if (canAfford) WastelandDarkBg else Color.Gray,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }
    }
  }
}
