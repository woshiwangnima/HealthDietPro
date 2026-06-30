package com.woshiwangnima.healthdietpro.model.region

/**
 * 单一省份/直辖市/自治区/特别行政区。
 * @param code  GB/T 2260 二位代码（如 "11"、"13"、"54"）；用作存盘 ID 与 diseases.json prevalence key。
 * @param name  显示用中文名（如 "北京市"、"河北省"、"西藏自治区"）。
 * @param polygons 一个省可由多个外环组成（如海岛/飞地），每个外环是 [[lng, lat], ...] 的点列表。
 */
data class Province(
    val code: String,
    val name: String,
    val polygons: List<List<DoubleArray>>,
    /** 省级质心坐标，用于多边形多命中时择近。 */
    val lng: Double = 0.0,
    val lat: Double = 0.0
)