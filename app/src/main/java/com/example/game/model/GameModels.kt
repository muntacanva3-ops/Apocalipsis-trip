package com.example.game.model

import com.example.game.catalog.RelicItem

enum class PlayerMode {
  DRIVING,
  ON_FOOT
}

enum class CameraMode {
  FIRST_PERSON_COCKPIT,
  THIRD_PERSON
}

enum class StructureType {
  HOUSE,
  MUSEUM,
  OUTPOST_100KM
}

data class PropData(
  val type: Int, // 0: rock, 1: saguaro cactus, 2: wrecked car without tailgate
  val x: Float,
  val y: Float,
  val z: Float,
  val rotation: Float,
  val scale: Float
)

data class WorldStructure(
  val type: StructureType,
  val x: Float,
  val y: Float,
  val z: Float,
  val rotation: Float
)

data class WorldItemInstance(
  val instanceId: Long,
  val item: RelicItem,
  var x: Float,
  var y: Float,
  var z: Float,
  var isPickedUp: Boolean = false,
  var isCarriedInHand: Boolean = false,
  var isInTrunk: Boolean = false
)

data class ZombieInstance(
  val id: Long,
  var x: Float,
  var y: Float,
  var z: Float,
  val level: Int,
  val maxHp: Float,
  var hp: Float,
  val speed: Float,
  val damage: Float,
  val bounty: Int,
  val scale: Float,
  var isAlive: Boolean = true,
  var walkCycle: Float = 0f,
  var hitStun: Float = 0f
)

data class CarUpgrades(
  var engineBlockLevel: Int = 1,
  var carburetorLevel: Int = 1,
  var radiatorLevel: Int = 1,
  var fuelTankLevel: Int = 1,
  var tiresLevel: Int = 1
) {
  val maxSpeedKmh: Float
    get() = when (engineBlockLevel) {
      1 -> 90f
      2 -> 120f
      3 -> 150f
      else -> 180f
    }

  val accelerationRate: Float
    get() = when (carburetorLevel) {
      1 -> 25f
      2 -> 35f
      3 -> 48f
      else -> 65f
    }

  val maxFuelCapacity: Float
    get() = when (fuelTankLevel) {
      1 -> 50f
      2 -> 75f
      3 -> 100f
      else -> 140f
    }

  val heatDissipationFactor: Float
    get() = when (radiatorLevel) {
      1 -> 1.0f
      2 -> 0.75f
      3 -> 0.5f
      else -> 0.3f
    }

  val offroadTraction: Float
    get() = when (tiresLevel) {
      1 -> 0.65f
      2 -> 0.80f
      3 -> 0.92f
      else -> 1.0f
    }
}

data class UpgradeInfo(
  val key: String,
  val name: String,
  val description: String,
  val currentLevel: Int,
  val maxLevel: Int = 4,
  val nextCost: Int?
)

object UpgradePricing {
  fun getCost(level: Int): Int? = when (level) {
    1 -> 60
    2 -> 130
    3 -> 240
    else -> null
  }
}
