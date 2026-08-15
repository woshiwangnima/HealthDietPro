package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.graphics.Color
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.range.findRangeBand
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.bodyRecordEpochMillis
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDisplayDateTime

object BmiUtil {

    data class BmiBand(val min: Float, val max: Float, val label: String, val color: Int)

    /** BMI 数据点：同时携带当时使用的身高/体重（基准单位）与显示单位 id，供图表与数据列表复用。 */
    data class BmiDataPoint(
        val timestamp: Long,
        val bmi: Float,
        val heightCm: Float,
        val weightKg: Float,
        val heightUnitId: String,
        val weightUnitId: String,
        val dateLabel: String,
    ) {
        fun toChartPoint(): DataPoint = DataPoint(timestamp = timestamp, value = bmi, dateLabel = dateLabel)
    }

    fun loadBmiBands(): List<BmiBand> {
        return listOf(
            BmiBand(-1f, 18.5f, "体重过低", Color.parseColor("#269E9E9E")),
            BmiBand(18.5f, 24f, "体重正常", Color.parseColor("#264CAF50")),
            BmiBand(24f, 28f, "超重", Color.parseColor("#26FFEB3B")),
            BmiBand(28f, Float.MAX_VALUE, "肥胖", Color.parseColor("#26FF5722")),
        )
    }

    fun computeBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0f || weightKg <= 0f) return 0f
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }

    fun getBmiLabel(bmi: Float, bands: List<BmiBand> = loadBmiBands()): String {
        return findBmiBand(bmi, bands)?.label ?: "未知"
    }

    fun getBmiColor(bmi: Float, bands: List<BmiBand> = loadBmiBands()): Int =
        findBmiBand(bmi, bands)?.color ?: Color.TRANSPARENT

    fun findBmiBand(bmi: Float, bands: List<BmiBand> = loadBmiBands()): BmiBand? =
        bmi.toDouble().findRangeBand(
            bands.map { band ->
                RangeBand(
                    min = band.min.takeIf { it >= 0f }?.toDouble(),
                    max = band.max.takeUnless { it == Float.MAX_VALUE }?.toDouble(),
                    value = band,
                )
            },
        )?.value

    /**
     * 以事件驱动合并身高/体重记录：每条记录都是一次「身高或体重变动」事件，
     * 每次事件后若身高与体重均已存在即产生一个新的 BMI 数据点（同一时刻的
     * 身高+体重事件合并为一个点）。返回按时间升序的 [BmiDataPoint]。
     */
    fun buildBmiDataPoints(weightRecords: List<BodyRecord>, heightRecords: List<BodyRecord>): List<BmiDataPoint> {
        if (weightRecords.isEmpty() || heightRecords.isEmpty()) return emptyList()

        data class Event(val at: Long, val date: String, val weight: BodyRecord?, val height: BodyRecord?)

        val events = buildList {
            weightRecords.forEach { add(Event(bodyRecordEpochMillis(it.date), it.date, it, null)) }
            heightRecords.forEach { add(Event(bodyRecordEpochMillis(it.date), it.date, null, it)) }
        }.sortedBy { it.at }

        val result = mutableListOf<BmiDataPoint>()
        var lastWeight: BodyRecord? = null
        var lastHeight: BodyRecord? = null

        var i = 0
        while (i < events.size) {
            val at = events[i].at
            val date = events[i].date
            while (i < events.size && events[i].at == at) {
                val event = events[i]
                if (event.weight != null) lastWeight = event.weight
                if (event.height != null) lastHeight = event.height
                i++
            }
            val w = lastWeight ?: continue
            val h = lastHeight ?: continue
            val bmi = computeBmi(w.value, h.value)
            if (bmi > 0f) {
                result.add(
                    BmiDataPoint(
                        timestamp = at,
                        bmi = bmi,
                        heightCm = h.value,
                        weightKg = w.value,
                        heightUnitId = h.getUnit(isWeight = false),
                        weightUnitId = w.getUnit(isWeight = true),
                        dateLabel = formatBodyRecordDisplayDateTime(date),
                    ),
                )
            }
        }
        return result
    }
}
