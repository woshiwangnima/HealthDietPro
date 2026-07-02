package com.woshiwangnima.healthdietpro.model.unit

sealed class UnitCategoryType(
    val id: String
) {
    object Weight : UnitCategoryType("weight")
    object Length : UnitCategoryType("length")
    object Volume : UnitCategoryType("volume")
    object Density : UnitCategoryType("density")
    object Time : UnitCategoryType("time")
    object Energy : UnitCategoryType("energy")
    object Storage : UnitCategoryType("storage")
}