package com.woshiwangnima.healthdietpro.model.unit

data class UnitCategory(
    val id: String,
    val categoryCn: String,
    val categoryEn: String,
    val baseUnit: String,
    val units: List<UnitDef>
) {
    companion object {
        const val ID_LENGTH = "length"
        const val ID_WEIGHT = "weight"
        const val ID_VOLUME = "volume"
        const val ID_DENSITY = "density"
        const val ID_TIME = "time"
        const val ID_ENERGY = "energy"

        const val DEFAULT_UNIT_LENGTH = "cm"
        const val DEFAULT_UNIT_WEIGHT = "kg"
        const val DEFAULT_UNIT_VOLUME = "l"
        const val DEFAULT_UNIT_DENSITY = "g_ml"
        const val DEFAULT_UNIT_TIME = "s"
        const val DEFAULT_UNIT_ENERGY = "kcal"
    }
}
