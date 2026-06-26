package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.graphics.Color
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId

object BmiUtil {

    data class BmiBand(val min: Float, val max: Float, val label: String, val color: Int)

    fun loadBmiBands(): List<BmiBand> {
        return listOf(
            BmiBand(-1f, 18.5f, "体重过低", Color.parseColor("#4D9E9E9E")),
            BmiBand(18.5f, 24f, "体重正常", Color.parseColor("#4D4CAF50")),
            BmiBand(24f, 28f, "超重", Color.parseColor("#4DFFEB3B")),
            BmiBand(28f, Float.MAX_VALUE, "肥胖", Color.parseColor("#4DFF5722"))
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

    fun buildBmiDataPoints(weightRecords: List<BodyRecord>, heightRecords: List<BodyRecord>): List<com.woshiwangnima.healthdietpro.model.profile.DataPoint> {
        if (weightRecords.isEmpty() || heightRecords.isEmpty()) return emptyList()

        val heightByDate = heightRecords.sortedBy { it.date }.associateBy { it.date }
        val sortedWeights = weightRecords.sortedBy { it.date }

        val result = mutableListOf<com.woshiwangnima.healthdietpro.model.profile.DataPoint>()
        val heightDates = heightByDate.keys.sorted()

        for (w in sortedWeights) {
            // Find the nearest height record on or before this weight date
            val hDate = heightDates.lastOrNull { it <= w.date } ?: heightDates.first()
            val h = heightByDate[hDate] ?: continue
            val bmi = computeBmi(w.value, h.value)
            if (bmi > 0f) {
                val localDate = LocalDate.parse(w.date)
                val ts = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                result.add(com.woshiwangnima.healthdietpro.model.profile.DataPoint(
                    timestamp = ts,
                    value = bmi,
                    dateLabel = w.date.takeLast(5)
                ))
            }
        }
        return result
    }
}
