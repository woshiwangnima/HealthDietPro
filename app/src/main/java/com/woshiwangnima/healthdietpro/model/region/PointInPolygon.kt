package com.woshiwangnima.healthdietpro.model.region

/**
 * 射线交叉法（ray casting）判断点是否落在闭合多边形内。
 * 经度 lng 当 x、纬度 lat 当 y。环上的点须自闭合（首尾重复）以利边界处理。
 */
object PointInPolygon {
    fun contains(ring: List<DoubleArray>, lng: Double, lat: Double): Boolean {
        if (ring.size < 3) return false
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val xi = ring[i][0]; val yi = ring[i][1]
            val xj = ring[j][0]; val yj = ring[j][1]
            val intersect = ((yi > lat) != (yj > lat)) &&
                (lng < (xj - xi) * (lat - yi) / (yj - yi + 1e-12) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}