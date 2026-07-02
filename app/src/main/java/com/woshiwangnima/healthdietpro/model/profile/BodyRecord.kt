package com.woshiwangnima.healthdietpro.model.profile

import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import java.io.Serializable

data class BodyRecord(
    val date: String,
    val value: Float,
    val unit: String? = null
) : Serializable {
    fun getUnit(isWeight: Boolean): String =
        unit?.takeIf { it.isNotEmpty() } ?: if (isWeight) UnitCategoryType.Weight.defaultUnitId else UnitCategoryType.Length.defaultUnitId
}
