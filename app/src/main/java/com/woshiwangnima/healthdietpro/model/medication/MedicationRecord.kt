package com.woshiwangnima.healthdietpro.model.medication

/**
 * 单条用药记录。所有字段经 Gson 序列化存入 SharedPreferences。
 *
 * @param id          记录主键（生成时间毫秒 + 随机数）
 * @param timestamp   用药时刻（epoch millis），覆盖年月日时分秒
 * @param medicationName 药品名称（自由文本，与历史记录关联）
 * @param doseValue   用药计量（浮点数值，例如 1 表示「1 粒」中的 1）
 * @param doseUnit    用药计量文字单位（自由文本，例如「粒」、「片」）
 * @param specValue   用药规格数值（浮点）
 * @param specUnitCategory 用药规格单位分类 id（取自 [com.woshiwangnima.healthdietpro.model.unit.UnitCategory] 的 ID_*）
 * @param specUnitId  用药规格单位 id（同上 UnitCategory.units[].id）
 * @param method      用药方式（如「口服」「注射」「冲泡」）
 * @param feelings    多选 tab 选择的感受标签列表
 * @param feelingNote 用户在感受标签后追加的自由备注
 * @param photoPath   本地照片文件相对路径（相对 app filesDir），可空
 */
data class MedicationRecord(
    val id: String,
    val timestamp: Long,
    val medicationName: String,
    val doseValue: Float,
    val doseUnit: String,
    val specValue: Float,
    val specUnitCategory: String,
    val specUnitId: String,
    val method: String,
    val feelings: List<String> = emptyList(),
    val feelingNote: String = "",
    val photoPath: String? = null
)