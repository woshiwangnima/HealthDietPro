package com.woshiwangnima.healthdietpro.model.profile

import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import java.io.Serializable

/**
 * 体征记录实体（user 存档模块，DESIGN §3.7.2）。
 *
 * **不变量**：[value] 始终以基准单位存储（体重 kg / 身高 cm）。
 * UI 边界经 [com.woshiwangnima.healthdietpro.util.UnitConverter.toBase] / [fromBase]
 * 换算；[unit] 字段仅记录用户用过的显示单位 id，**不可信为值的单位**，
 * 仅为 UI 提示。
 */
data class BodyRecord(
    val date: String,
    val value: Float,
    val unit: String? = null
) : Serializable {
    fun getUnit(isWeight: Boolean): String =
        unit?.takeIf { it.isNotEmpty() } ?: if (isWeight) UnitCategoryType.Weight.defaultUnitId else UnitCategoryType.Length.defaultUnitId
}
