package com.woshiwangnima.healthdietpro.model.unit

enum class UnitCategoryType(val id: String, val defaultUnitId: String) {
    Weight("weight", "kg"),
    Length("length", "cm"),
    Volume("volume", "l"),
    Density("density", "g_ml"),
    Time("time", "s"),
    Energy("energy", "kcal"),
    Storage("storage", "b");

    companion object {
        private val byId by lazy { entries.associateBy { it.id } }
        fun fromId(id: String): UnitCategoryType =
            byId[id] ?: throw IllegalArgumentException("Unknown UnitCategoryType: $id")
    }
}
