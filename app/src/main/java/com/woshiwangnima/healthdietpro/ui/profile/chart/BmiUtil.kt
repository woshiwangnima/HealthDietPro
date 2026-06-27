package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.graphics.Color
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import java.time.LocalDate
import java.time.ZoneId

object BmiUtil {

    data class BmiBand(val min: Float, val max: Float, val label: String, val color: Int)

    fun loadBmiBands(): List<BmiBand> {
        return listOf(
            BmiBand(-1f, 18.5f, "体重过低", Color.parseColor("#269E9E9E")),
            BmiBand(18.5f, 24f, "体重正常", Color.parseColor("#264CAF50")),
            BmiBand(24f, 28f, "超重", Color.parseColor("#26FFEB3B")),
            BmiBand(28f, Float.MAX_VALUE, "肥胖", Color.parseColor("#26FF5722"))
        )
    }

    fun computeBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0f || weightKg <= 0f) return 0f
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }

    fun getBmiLabel(bmi: Float, bands: List<BmiBand> = loadBmiBands()): String {
        return bands.find {
            (it.min < 0f || bmi >= it.min) && (it.max < 0f || it.max == Float.MAX_VALUE || bmi < it.max)
        }?.label ?: "未知"
    }

    fun buildBmiDataPoints(weightRecords: List<BodyRecord>, heightRecords: List<BodyRecord>): List<DataPoint> {
        if (weightRecords.isEmpty() || heightRecords.isEmpty()) return emptyList()

        val sortedW = weightRecords.sortedBy { it.date }
        val sortedH = heightRecords.sortedBy { it.date }

        // Collect all unique dates from both records
        val allDates = sortedSetOf<String>()
        for (w in sortedW) allDates.add(w.date)
        for (h in sortedH) allDates.add(h.date)

        val result = mutableListOf<DataPoint>()
        var wi = 0; var hi = 0

        for (date in allDates) {
            // Advance weight pointer to the latest record at or before this date
            while (wi < sortedW.size && sortedW[wi].date <= date) wi++
            // Advance height pointer to the latest record at or before this date
            while (hi < sortedH.size && sortedH[hi].date <= date) hi++

            val w = if (wi > 0) sortedW[wi - 1] else null
            val h = if (hi > 0) sortedH[hi - 1] else null

            if (w != null && h != null) {
                val bmi = computeBmi(w.value, h.value)
                if (bmi > 0f) {
                    val localDate = LocalDate.parse(date)
                    val ts = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    result.add(DataPoint(timestamp = ts, value = bmi, dateLabel = date.takeLast(5)))
                }
            }
        }
        return result
    }
}
