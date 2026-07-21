package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.graphics.Color
import com.woshiwangnima.healthdietpro.common.range.NumericRangeBand
import com.woshiwangnima.healthdietpro.common.range.findRangeBand
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.bodyRecordEpochMillis
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDisplayDateTime

object BmiUtil {

    data class BmiBand(val min: Float, val max: Float, val label: String, val color: Int)

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
                NumericRangeBand(
                    min = band.min.takeIf { it >= 0f }?.toDouble(),
                    max = band.max.takeUnless { it == Float.MAX_VALUE }?.toDouble(),
                    value = band,
                )
            },
        )?.value

    fun buildBmiDataPoints(weightRecords: List<BodyRecord>, heightRecords: List<BodyRecord>): List<DataPoint> {
        if (weightRecords.isEmpty() || heightRecords.isEmpty()) return emptyList()

        val sortedW = weightRecords.sortedBy { bodyRecordEpochMillis(it.date) }
        val sortedH = heightRecords.sortedBy { bodyRecordEpochMillis(it.date) }

        val allDates = (sortedW.map { it.date } + sortedH.map { it.date })
            .distinct()
            .sortedBy { bodyRecordEpochMillis(it) }

        val result = mutableListOf<DataPoint>()
        var wi = 0
        var hi = 0

        for (date in allDates) {
            val timestamp = bodyRecordEpochMillis(date)
            while (wi < sortedW.size && bodyRecordEpochMillis(sortedW[wi].date) <= timestamp) wi++
            while (hi < sortedH.size && bodyRecordEpochMillis(sortedH[hi].date) <= timestamp) hi++

            val w = if (wi > 0) sortedW[wi - 1] else null
            val h = if (hi > 0) sortedH[hi - 1] else null

            if (w != null && h != null) {
                val bmi = computeBmi(w.value, h.value)
                if (bmi > 0f) {
                    result.add(
                        DataPoint(
                            timestamp = timestamp,
                            value = bmi,
                            dateLabel = formatBodyRecordDisplayDateTime(date),
                        ),
                    )
                }
            }
        }
        return result
    }
}
