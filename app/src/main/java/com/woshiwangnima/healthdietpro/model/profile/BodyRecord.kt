package com.woshiwangnima.healthdietpro.model.profile

import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import java.io.Serializable

data class BodyRecord(
    val date: String,
    val value: Float,
    val unit: String? = null
) : Serializable {
    fun getUnit(isWeight: Boolean): String =
        unit?.takeIf { it.isNotEmpty() } ?: if (isWeight) UnitCategory.DEFAULT_UNIT_WEIGHT else UnitCategory.DEFAULT_UNIT_LENGTH
}
