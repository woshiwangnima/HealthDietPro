package com.woshiwangnima.healthdietpro.model.region

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 三级行政区划库：
 *  - 省级：仍用 [ProvinceRepository] 的简化多边形 + 射线法（精度高）
 *  - 市级、县级：分层「质心最近」法。加载 [Region] 列表后按 parentCode 索引，
 *    反查流程 = 省(射线) → 市内质心最近 → 县内质心最近。
 *
 * 质心法在行政边界附近会有 1-3 公里量级误差，但相比多边形数据体积优势巨大
 * （~3000 条质心 < 1MB，完整多边形要 50+MB）。
 */
class RegionRepository private constructor(
    private val _regions: List<Region>
) {
    private val byCode: Map<String, Region> = _regions.associateBy { it.code }
    private val citiesByProvince: Map<String, List<Region>> =
        _regions.filter { it.level == "city" }.groupBy { it.parentCode }
    private val districtsByCity: Map<String, List<Region>> =
        _regions.filter { it.level == "district" }.groupBy { it.parentCode }

    fun all(): List<Region> = _regions
    fun findByCode(code: String): Region? = byCode[code]
    fun citiesOf(provinceCode: String): List<Region> =
        citiesByProvince[provinceCode].orEmpty().sortedBy { it.code }
    fun districtsOf(cityCode: String): List<Region> =
        districtsByCity[cityCode].orEmpty().sortedBy { it.code }

    /** 在指定上一级代码列表里找质心最近的，找不到返回 null。 */
    private fun nearest(child: List<Region>, lng: Double, lat: Double): Region? {
        if (child.isEmpty()) return null
        var best: Region? = null
        var bestD = Double.MAX_VALUE
        for (r in child) {
            val d = (r.lng - lng) * (r.lng - lng) + (r.lat - lat) * (r.lat - lat)
            if (d < bestD) { bestD = d; best = r }
        }
        return best
    }

    /**
     * 给定 GPS 坐标，反查省/市/县三级 + 省 Name，返回完整 [RegionSnapshot]。
     * [provinceRepo] 由调用方提供（保留现有 34 省多边形数据 + 射线法）。
     * 越外层命中失败时返回空 snapshot。
     */
    fun resolve(lng: Double, lat: Double, provinceRepo: ProvinceRepository): RegionSnapshot {
        val prov = provinceRepo.findByPoint(lng, lat) ?: return RegionSnapshot(lng = lng, lat = lat)
        val city = nearest(citiesOf(prov.code), lng, lat)
        val district = city?.let { nearest(districtsOf(it.code), lng, lat) }
        return RegionSnapshot(
            lng = lng, lat = lat,
            provinceCode = prov.code, provinceName = prov.name,
            cityCode = city?.code ?: "", cityName = city?.name ?: "",
            districtCode = district?.code ?: "", districtName = district?.name ?: ""
        )
    }

    companion object {
        fun fromAsset(path: String): RegionRepository {
            val raw = File(path).readText()
            return parse(raw)
        }

        fun fromContext(context: Context): RegionRepository {
            val raw = context.assets.open("location/regions.json").bufferedReader().use { it.readText() }
            return parse(raw)
        }

        private fun parse(raw: String): RegionRepository {
            val type = object : TypeToken<List<RegionJsonDto>>() {}.type
            val dtos: List<RegionJsonDto> = Gson().fromJson(raw, type)
            return RegionRepository(dtos.map {
                Region(it.level, it.code, it.name, it.parentCode, it.lng, it.lat)
            })
        }
    }
}

internal data class RegionJsonDto(
    val level: String,
    val code: String,
    val name: String,
    val parentCode: String,
    val lng: Double,
    val lat: Double
)
