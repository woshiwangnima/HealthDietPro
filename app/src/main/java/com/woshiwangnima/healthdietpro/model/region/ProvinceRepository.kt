package com.woshiwangnima.healthdietpro.model.region

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ProvinceRepository private constructor(
    private val provinces: List<Province>
) {
    fun all(): List<Province> = provinces
    fun findByCode(code: String): Province? = provinces.firstOrNull { it.code == code }
    fun findByName(name: String): Province? = provinces.firstOrNull { it.name == name }

    /**
     * 反查入口：射线法 + 多边形命中集合上的质心最近择一。
     * 之所以补"质心择近"层，是因为简化多边形（矩形拟合）相邻省会重叠，
     * 此时 `firstOrNull` 会按 JSON 顺序抢答，结果不稳。先用射线法收集
     * 全部命中的省，再在其中按质心距离最小的那一个返回，精度更好。
     */
    fun findByPoint(lng: Double, lat: Double): Province? {
        val hits = provinces.filter { runCasting(it, lng, lat) }
        if (hits.isEmpty()) return null
        if (hits.size == 1) return hits.first()
        // 多命中：取质心最近的
        var best: Province? = null
        var bestD = Double.MAX_VALUE
        for (p in hits) {
            val d = (p.lng - lng) * (p.lng - lng) + (p.lat - lat) * (p.lat - lat)
            if (d < bestD) { bestD = d; best = p }
        }
        return best
    }

    private fun runCasting(p: Province, lng: Double, lat: Double): Boolean {
        for (ring in p.polygons) if (PointInPolygon.contains(ring, lng, lat)) return true
        return false
    }

    companion object {
        /** 用于单测与离线场景：直接从文件路径读 JSON。 */
        fun fromAsset(path: String): ProvinceRepository {
            val raw = File(path).readText()
            val type = object : TypeToken<List<ProvinceJsonDto>>() {}.type
            val dtos: List<ProvinceJsonDto> = Gson().fromJson(raw, type)
            return ProvinceRepository(dtos.map { Province(it.code, it.name, it.polygons, it.lng, it.lat) })
        }

        /** 用于运行时 (Activity / Fragment)：通过 Android AssetManager 加载。 */
        fun fromContext(context: Context): ProvinceRepository {
            val raw = context.assets.open("provinces.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<ProvinceJsonDto>>() {}.type
            val dtos: List<ProvinceJsonDto> = Gson().fromJson(raw, type)
            return ProvinceRepository(dtos.map { Province(it.code, it.name, it.polygons, it.lng, it.lat) })
        }
    }
}

internal data class ProvinceJsonDto(
    val code: String,
    val name: String,
    val polygons: List<List<DoubleArray>>,
    val lng: Double = 0.0,
    val lat: Double = 0.0
)