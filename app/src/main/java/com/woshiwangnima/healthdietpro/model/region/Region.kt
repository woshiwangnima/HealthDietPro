package com.woshiwangnima.healthdietpro.model.region

/**
 * 三级层级行政区划单位：省 / 市 / 县区。省级仍保留 [Province] 的多边形数据，
 * 这里 [Region] 仅承载质心数据（市/县），用于「分层最近质心」反查。
 *
 * @param level       "province" / "city" / "district"
 * @param code        GB/T 2260 全位代码：省 2 位、市 4 位、县 6 位
 * @param name        显示名（如 "河北省" / "石家庄市" / "长安区"）
 * @param parentCode  上一级 code；省级为空
 * @param lng         质心经度
 * @param lat         质心纬度
 */
data class Region(
    val level: String,
    val code: String,
    val name: String,
    val parentCode: String,
    val lng: Double,
    val lat: Double
)