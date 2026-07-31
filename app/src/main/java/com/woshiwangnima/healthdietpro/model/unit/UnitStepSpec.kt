package com.woshiwangnima.healthdietpro.model.unit

enum class UnitStepMode { Normal, Fine }

data class UnitStepSpec(
    val normal: Float,
    val fine: Float,
) {
    fun valueFor(mode: UnitStepMode): Float = if (mode == UnitStepMode.Fine) fine else normal
}

fun UnitCategoryType.stepSpec(unitId: String): UnitStepSpec = when (this) {
    UnitCategoryType.Glucose -> if (unitId == "mg_dl") UnitStepSpec(normal = 1f, fine = 0.1f) else UnitStepSpec(normal = 0.1f, fine = 0.01f)
    UnitCategoryType.Pressure -> if (unitId == "kpa") UnitStepSpec(normal = 0.1f, fine = 0.01f) else UnitStepSpec(normal = 1f, fine = 1f)
    UnitCategoryType.Time -> UnitStepSpec(normal = 1f, fine = 1f)
    else -> UnitStepSpec(normal = 1f, fine = 0.1f)
}
