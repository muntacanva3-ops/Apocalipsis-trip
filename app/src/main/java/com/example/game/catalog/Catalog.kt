package com.example.game.catalog

enum class ItemCategory(val label: String) {
  RESOURCES("Recursos"),
  MECHANICS("Mecánica"),
  RELIC("Reliquia"),
  SURVIVAL("Supervivencia"),
  MEDICAL("Médico")
}

enum class ItemUtilityType(val actionLabel: String) {
  FUEL("Repostar Gasolina"),
  OIL("Lubricar Motor"),
  WATER("Beber Agua"),
  FOOD("Comer Ración"),
  HEALTH("Curar Heridas"),
  SCRAP("Reforzar Chasis"),
  VALUABLE("Tasar Reliquia")
}

data class RelicItem(
  val id: Int,
  val name: String,
  val category: ItemCategory,
  val description: String,
  val value: Int,
  val utilityType: ItemUtilityType,
  val utilityAmount: Int,
  val locationFound: String,
  val colorHex: Long = 0xFFFFB300
)

object Catalog {
  val ITEMS: List<RelicItem> = listOf(
    RelicItem(
      id = 1,
      name = "Bidón de Gasolina Oxidado",
      category = ItemCategory.RESOURCES,
      description = "Bidón metálico con 15L de combustible fósil rescatable.",
      value = 35,
      utilityType = ItemUtilityType.FUEL,
      utilityAmount = 15,
      locationFound = "Carretera",
      colorHex = 0xFFFF5722
    ),
    RelicItem(
      id = 2,
      name = "Botella de Aceite Sintético",
      category = ItemCategory.MECHANICS,
      description = "Lubricante de alta viscosidad que reduce el petardeo del V8.",
      value = 25,
      utilityType = ItemUtilityType.OIL,
      utilityAmount = 30,
      locationFound = "Carretera",
      colorHex = 0xFF795548
    ),
    RelicItem(
      id = 3,
      name = "Garrafa de Agua Destilada",
      category = ItemCategory.SURVIVAL,
      description = "Agua pura libre de radiación apta para beber o refrigerante.",
      value = 20,
      utilityType = ItemUtilityType.WATER,
      utilityAmount = 35,
      locationFound = "Casa Abandonada",
      colorHex = 0xFF03A9F4
    ),
    RelicItem(
      id = 4,
      name = "Lata de Ternera Militar",
      category = ItemCategory.SURVIVAL,
      description = "Ración sellada con alto contenido calórico y proteico.",
      value = 18,
      utilityType = ItemUtilityType.FOOD,
      utilityAmount = 30,
      locationFound = "Casa Abandonada",
      colorHex = 0xFF8BC34A
    ),
    RelicItem(
      id = 5,
      name = "Cantimplora de Acero Inox",
      category = ItemCategory.SURVIVAL,
      description = "Contenedor térmico con agua de manantial subterráneo.",
      value = 15,
      utilityType = ItemUtilityType.WATER,
      utilityAmount = 25,
      locationFound = "Carretera",
      colorHex = 0xFF00BCD4
    ),
    RelicItem(
      id = 6,
      name = "Llave Inglesa Pesada",
      category = ItemCategory.MECHANICS,
      description = "Herramienta forjada en cromo para ajustes mecánicos y defensa.",
      value = 22,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 20,
      locationFound = "Carretera",
      colorHex = 0xFF9E9E9E
    ),
    RelicItem(
      id = 7,
      name = "Bujía de Platino Antigua",
      category = ItemCategory.MECHANICS,
      description = "Chispa de encendido de alto rendimiento para el carburador.",
      value = 40,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 25,
      locationFound = "Casa Abandonada",
      colorHex = 0xFFFFC107
    ),
    RelicItem(
      id = 8,
      name = "Batería de Coche Sulfatada",
      category = ItemCategory.MECHANICS,
      description = "Celda de plomo-ácido aún capaz de retener carga eléctrica.",
      value = 60,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 40,
      locationFound = "Carretera",
      colorHex = 0xFFFF9800
    ),
    RelicItem(
      id = 9,
      name = "Cinta Americana Reforzada",
      category = ItemCategory.RESOURCES,
      description = "Adhesivo de grado militar que sella conductos y manguitos.",
      value = 12,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 15,
      locationFound = "Casa Abandonada",
      colorHex = 0xFF607D8B
    ),
    RelicItem(
      id = 10,
      name = "Neumático Todoterreno Usado",
      category = ItemCategory.MECHANICS,
      description = "Goma con dibujo profundo para tracción en dunas y tierra.",
      value = 50,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 35,
      locationFound = "Carretera",
      colorHex = 0xFF212121
    ),
    RelicItem(
      id = 11,
      name = "Filtro de Aire Carburado",
      category = ItemCategory.MECHANICS,
      description = "Malla de filtrado que protege el colector de admisión del polvo.",
      value = 30,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 20,
      locationFound = "Casa Abandonada",
      colorHex = 0xFFCDDC39
    ),
    RelicItem(
      id = 12,
      name = "Botiquín Médico de Emergencia",
      category = ItemCategory.MEDICAL,
      description = "Vendas estériles, antiséptico y analgésicos concentrados.",
      value = 45,
      utilityType = ItemUtilityType.HEALTH,
      utilityAmount = 50,
      locationFound = "Casa Abandonada",
      colorHex = 0xFFE91E63
    ),
    RelicItem(
      id = 13,
      name = "Estatua de Bronce Clásica",
      category = ItemCategory.RELIC,
      description = "Escultura de museo con pátina histórica de inmenso valor.",
      value = 180,
      utilityType = ItemUtilityType.VALUABLE,
      utilityAmount = 180,
      locationFound = "Museo Abandonado",
      colorHex = 0xFFD7CCC8
    ),
    RelicItem(
      id = 14,
      name = "Cuadro al Óleo del Siglo XIX",
      category = ItemCategory.RELIC,
      description = "Retrato barroco enmarcado en madera de roble barnizada.",
      value = 220,
      utilityType = ItemUtilityType.VALUABLE,
      utilityAmount = 220,
      locationFound = "Museo Abandonado",
      colorHex = 0xFFFFD54F
    ),
    RelicItem(
      id = 15,
      name = "Reloj de Bolsillo de Oro",
      category = ItemCategory.RELIC,
      description = "Mecanismo suizo de engranajes intacto con tapa grabada.",
      value = 150,
      utilityType = ItemUtilityType.VALUABLE,
      utilityAmount = 150,
      locationFound = "Museo Abandonado",
      colorHex = 0xFFFFD700
    ),
    RelicItem(
      id = 16,
      name = "Monóculo de Coleccionista",
      category = ItemCategory.RELIC,
      description = "Lente tallada en cristal de cuarzo con montura de plata.",
      value = 90,
      utilityType = ItemUtilityType.VALUABLE,
      utilityAmount = 90,
      locationFound = "Museo Abandonado",
      colorHex = 0xFFB0BEC5
    ),
    RelicItem(
      id = 17,
      name = "Fusible Cerámico Industrial",
      category = ItemCategory.MECHANICS,
      description = "Fusible de alta potencia para el circuito de faros halógenos.",
      value = 16,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 15,
      locationFound = "Carretera",
      colorHex = 0xFFFFCCBC
    ),
    RelicItem(
      id = 18,
      name = "Termostato de Cobre Vintage",
      category = ItemCategory.MECHANICS,
      description = "Válvula térmica que regula el flujo del refrigerante del radiador.",
      value = 28,
      utilityType = ItemUtilityType.OIL,
      utilityAmount = 25,
      locationFound = "Casa Abandonada",
      colorHex = 0xFFBCAAA4
    ),
    RelicItem(
      id = 19,
      name = "Plancha de Chatarra Blindada",
      category = ItemCategory.RESOURCES,
      description = "Placa de acero remachada ideal para reforzar parachoques.",
      value = 14,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 25,
      locationFound = "Carretera",
      colorHex = 0xFF78909C
    ),
    RelicItem(
      id = 20,
      name = "Rollo de Alambre de Espino",
      category = ItemCategory.RESOURCES,
      description = "Cercado defensivo disuasorio contra hordas de mutantes.",
      value = 10,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 15,
      locationFound = "Carretera",
      colorHex = 0xFF546E7A
    ),
    RelicItem(
      id = 21,
      name = "Barra de Remolque Forjada",
      category = ItemCategory.MECHANICS,
      description = "Estructura de tiro capaz de resistir tensiones extremas.",
      value = 55,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 35,
      locationFound = "Carretera",
      colorHex = 0xFF455A64
    ),
    RelicItem(
      id = 22,
      name = "Prismáticos Militares 10x50",
      category = ItemCategory.SURVIVAL,
      description = "Óptica alemana con retícula de telemetría y lentes nocturnas.",
      value = 75,
      utilityType = ItemUtilityType.VALUABLE,
      utilityAmount = 75,
      locationFound = "Museo Abandonado",
      colorHex = 0xFF558B2F
    ),
    RelicItem(
      id = 23,
      name = "Brújula Náutica de Latón",
      category = ItemCategory.RELIC,
      description = "Instrumento de navegación en baño de aceite del siglo XVIII.",
      value = 85,
      utilityType = ItemUtilityType.VALUABLE,
      utilityAmount = 85,
      locationFound = "Museo Abandonado",
      colorHex = 0xFFFFAB00
    ),
    RelicItem(
      id = 24,
      name = "Encendedor Zippo de Trinchera",
      category = ItemCategory.SURVIVAL,
      description = "Mechero a prueba de tormentas de arena con mecha de algodón.",
      value = 32,
      utilityType = ItemUtilityType.FUEL,
      utilityAmount = 5,
      locationFound = "Casa Abandonada",
      colorHex = 0xFFFF6E40
    ),
    RelicItem(
      id = 25,
      name = "Linterna Halógena Pesada",
      category = ItemCategory.SURVIVAL,
      description = "Foco autónomo de 1000 lúmenes con carcasa de goma antichoques.",
      value = 26,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 20,
      locationFound = "Casa Abandonada",
      colorHex = 0xFFFFEE58
    ),
    RelicItem(
      id = 26,
      name = "Manguera de Sifón Reforzada",
      category = ItemCategory.RESOURCES,
      description = "Tubo con válvula antiretorno para trasvasar gasolina entre tanques.",
      value = 18,
      utilityType = ItemUtilityType.FUEL,
      utilityAmount = 10,
      locationFound = "Carretera",
      colorHex = 0xFF26A69A
    ),
    RelicItem(
      id = 27,
      name = "Bolsa de Frutos Secos Envasados",
      category = ItemCategory.SURVIVAL,
      description = "Mezcla de nueces y pasas preservadas en atmósfera protectora.",
      value = 14,
      utilityType = ItemUtilityType.FOOD,
      utilityAmount = 20,
      locationFound = "Casa Abandonada",
      colorHex = 0xFF8D6E63
    ),
    RelicItem(
      id = 28,
      name = "Frasco de Pastillas Potabilizadoras",
      category = ItemCategory.MEDICAL,
      description = "Pastillas de cloro desinfectante para 50 litros de agua turbia.",
      value = 20,
      utilityType = ItemUtilityType.WATER,
      utilityAmount = 30,
      locationFound = "Casa Abandonada",
      colorHex = 0xFF80DEEA
    ),
    RelicItem(
      id = 29,
      name = "Amortiguador Hidráulico Reforzado",
      category = ItemCategory.MECHANICS,
      description = "Pistón de suspensión para baches severos y saltos en la cuneta.",
      value = 65,
      utilityType = ItemUtilityType.SCRAP,
      utilityAmount = 40,
      locationFound = "Carretera",
      colorHex = 0xFF009688
    ),
    RelicItem(
      id = 30,
      name = "Medalla Militar de Honor en Oro",
      category = ItemCategory.RELIC,
      description = "Condecoración bélica suprema incrustada con rubíes y oro puro.",
      value = 260,
      utilityType = ItemUtilityType.VALUABLE,
      utilityAmount = 260,
      locationFound = "Museo Abandonado",
      colorHex = 0xFFFFD700
    )
  )

  fun getItemById(id: Int): RelicItem? = ITEMS.find { it.id == id }
}
