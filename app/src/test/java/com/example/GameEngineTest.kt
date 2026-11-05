package com.example

import com.example.game.catalog.Catalog
import com.example.game.engine.GameEngine
import com.example.game.model.CarUpgrades
import com.example.game.model.PlayerMode
import com.example.game.model.UpgradePricing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

  @Test
  fun testCatalogHasExactly30UniqueRelics() {
    assertEquals(30, Catalog.ITEMS.size)
    val uniqueIds = Catalog.ITEMS.map { it.id }.toSet()
    assertEquals(30, uniqueIds.size)

    // Check all have positive valuation ($)
    for (item in Catalog.ITEMS) {
      assertTrue("Item ${item.name} must have positive value", item.value > 0)
      assertTrue("Item ${item.name} must have non-empty description", item.description.isNotBlank())
      assertNotNull("Item ${item.name} must have valid category", item.category)
    }
  }

  @Test
  fun testEngineUpgradesPricingAndEffects() {
    val upgrades = CarUpgrades()
    assertEquals(90f, upgrades.maxSpeedKmh, 0.1f)
    assertEquals(50f, upgrades.maxFuelCapacity, 0.1f)

    assertEquals(60, UpgradePricing.getCost(1))
    assertEquals(130, UpgradePricing.getCost(2))
    assertEquals(240, UpgradePricing.getCost(3))

    upgrades.engineBlockLevel = 2
    assertEquals(120f, upgrades.maxSpeedKmh, 0.1f)

    upgrades.fuelTankLevel = 3
    assertEquals(100f, upgrades.maxFuelCapacity, 0.1f)
  }

  @Test
  fun testTrunkStorageAndSelling() {
    val engine = GameEngine()
    val initialMoney = engine.money.value
    val testRelic = Catalog.ITEMS.first() // Bidon gasolina ($35)

    // Put item in trunk directly
    engine.addItemToTrunk(testRelic)
    assertEquals(1, engine.trunkItems.value.size)

    // Sell item
    engine.sellItem(testRelic)
    assertEquals(initialMoney + testRelic.value, engine.money.value)
    assertEquals(0, engine.trunkItems.value.size)
  }

  @Test
  fun testPlayerModeSwitching() {
    val engine = GameEngine()
    assertEquals(PlayerMode.DRIVING, engine.playerMode.value)

    engine.carState.speedKmh = 0f
    engine.togglePlayerMode()
    assertEquals(PlayerMode.ON_FOOT, engine.playerMode.value)
  }
}
