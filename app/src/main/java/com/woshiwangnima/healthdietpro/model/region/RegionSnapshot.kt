package com.woshiwangnima.healthdietpro.model.region

/**
 * 用户存盘的完整地区信息：经纬度 + 三级行政区划。
 * 旧字段 `province` 在 [com.woshiwangnima.healthdietpro.model.profile.UserProfile] 仍存
 * 2 位省代码用于疾病 prevalence；新数据通过本扩展字段补全省/市/县三级 + 坐标。
 */
data class RegionSnapshot(
    val lng: Double = 0.0,
    val lat: Double = 0.0,
    val provinceCode: String = "",
    val provinceName: String = "",
    val cityCode: String = "",
    val cityName: String = "",
    val districtCode: String = "",
    val districtName: String = ""
) {
    /** 三段拼接展示；空段省略。 */
    fun display(): String {
        val parts = listOf(provinceName, cityName, districtName)
            .filter { it.isNotEmpty() }
        return if (parts.isEmpty()) "未设置" else parts.joinToString(" - ")
    }

    fun isEmpty(): Boolean = provinceCode.isEmpty() && cityCode.isEmpty()
        && districtCode.isEmpty() && lng == 0.0 && lat == 0.0
}