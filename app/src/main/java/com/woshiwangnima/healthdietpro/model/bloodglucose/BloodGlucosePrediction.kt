package com.woshiwangnima.healthdietpro.model.bloodglucose

import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import kotlinx.serialization.Serializable
import kotlin.math.exp

internal const val PREDICTION_INTERVAL_MILLIS = 5 * 60_000L
internal const val PREDICTION_DURATION_MILLIS = 6 * 60 * 60_000L

@Serializable
internal data class BloodGlucosePredictionPoint(
    val timestamp: Long,
    val valueMmolPerL: Double,
    val unitId: String = "mmol_per_l",
)

@Serializable
internal enum class BloodGlucosePredictionConfidence { HIGH, MEDIUM, LOW }

internal enum class BloodGlucosePredictionGenerationStatus { IDLE, GENERATING, SUCCESS, INSUFFICIENT_DATA, FAILED }

internal data class BloodGlucosePredictionEligibility(
    val historyDays: Long,
    val validGlucoseCount: Int,
    val coveredGlucoseCount: Int,
    val validMealCount: Int,
) {
    val historyReady: Boolean get() = historyDays >= 14
    val glucoseCountReady: Boolean get() = validGlucoseCount >= 30
    val coveredCountReady: Boolean get() = coveredGlucoseCount >= 10
    val ready: Boolean get() = historyReady && glucoseCountReady && coveredCountReady
}

@Serializable
internal data class BloodGlucosePredictionArchive(
    val schemaVersion: Int = 1,
    val points: List<BloodGlucosePredictionPoint> = emptyList(),
    val generatedAt: Long? = null,
    val confidence: BloodGlucosePredictionConfidence? = null,
)

internal data class BloodGlucosePredictionResult(
    val points: List<BloodGlucosePredictionPoint>,
    val confidence: BloodGlucosePredictionConfidence,
)

internal fun isValidPredictionMeal(record: DietRecord): Boolean {
    if (record.entries.any { entry ->
            (entry.foodId == null || entry.foodId.startsWith("custom:")) && listOf("CHO", "FAT").any { code ->
                entry.resolvedNutrients[code]?.value?.isFinite() != true
            }
        }) return false
    val nutrients = record.entries.fold(emptyMap<String, Double>()) { totals, entry ->
        entry.resolvedNutrients.entries.fold(totals) { current, (code, amount) ->
            current + (code to ((current[code] ?: 0.0) + amount.value))
        }
    }
    return listOf("CHO", "FAT").all { code -> nutrients[code]?.isFinite() == true && nutrients.getValue(code) >= 0.0 }
}

/** Pure, user-specific forecast model. It intentionally never consumes saved predictions. */
internal fun predictBloodGlucose(
    records: List<BloodGlucoseRecord>,
    medications: List<MedicationRecord>,
    meals: List<DietRecord>,
    sleepRecords: List<SleepRecord>,
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): BloodGlucosePredictionResult? {
    val observed = records
        .filter { it.timestamp > 0L && it.valueMmolPerL in 1.1..33.3 }
        .distinctBy(BloodGlucoseRecord::timestamp)
        .sortedBy(BloodGlucoseRecord::timestamp)
    val eligibility = evaluatePredictionEligibility(records, medications, meals, sleepRecords)
    if (!eligibility.ready) return null
    val latest = observed.last()
    val start = nextPredictionGrid(nowMillis, zoneId)
    val end = start + PREDICTION_DURATION_MILLIS
    val recent = observed.filter { it.timestamp >= nowMillis - 72 * 60 * 60_000L }.ifEmpty { observed.takeLast(12) }
    val nearbyObservation = observed
        .filter { kotlin.math.abs(it.timestamp - nowMillis) <= RECENT_OBSERVATION_WINDOW_MILLIS }
        .maxByOrNull { it.timestamp }
    val baseline = nearbyObservation?.valueMmolPerL
        ?: averageAtCurrentTime(observed, nowMillis, zoneId)
    val validMeals = meals.filter(::isValidPredictionMeal)
    val medicationResponses = medicationResponses(medications, observed)
    val baseConsumptionRate = estimateBaseConsumptionRate(observed, validMeals, medications, sleepRecords)
    var state = baseline
    val points = generateSequence(start) { timestamp -> (timestamp + PREDICTION_INTERVAL_MILLIS).takeIf { it <= end } }
        .map { timestamp ->
            val eventEffect = repeatingMealEffect(validMeals, timestamp, zoneId) +
                repeatingSleepEffect(sleepRecords, timestamp, zoneId) +
                repeatingMedicationEffect(medicationResponses, timestamp, zoneId)
            state = (state - baseConsumptionRate * 5.0 + eventEffect).coerceIn(1.1, 33.3)
            BloodGlucosePredictionPoint(timestamp, state)
        }
        .toList()
    val confidence = when {
        recent.size >= 24 && validMeals.size >= 5 -> BloodGlucosePredictionConfidence.HIGH
        recent.size >= 8 -> BloodGlucosePredictionConfidence.MEDIUM
        else -> BloodGlucosePredictionConfidence.LOW
    }
    return BloodGlucosePredictionResult(points, confidence)
}

private const val RECENT_OBSERVATION_WINDOW_MILLIS = 30 * 60_000L

private fun averageAtCurrentTime(
    records: List<BloodGlucoseRecord>,
    nowMillis: Long,
    zoneId: ZoneId,
): Double {
    val currentBucket = timeOfDayBucket(nowMillis, zoneId)
    val sameTimeValues = records
        .filter { timeOfDayBucket(it.timestamp, zoneId) == currentBucket }
        .map(BloodGlucoseRecord::valueMmolPerL)
    return sameTimeValues.ifEmpty { records.map(BloodGlucoseRecord::valueMmolPerL) }.average()
}

private fun buildDailyProfile(
    records: List<BloodGlucoseRecord>,
    zoneId: ZoneId,
    fallback: Double,
): Map<Int, Double> = records
    .groupBy { timeOfDayBucket(it.timestamp, zoneId) }
    .mapValues { (_, values) -> values.map(BloodGlucoseRecord::valueMmolPerL).sorted().medianOr(fallback) }

private fun estimateBaseConsumptionRate(
    records: List<BloodGlucoseRecord>,
    meals: List<DietRecord>,
    medications: List<MedicationRecord>,
    sleepRecords: List<SleepRecord>,
): Double {
    val rates = records.zipWithNext().mapNotNull { (start, end) ->
        val elapsedMinutes = (end.timestamp - start.timestamp) / 60_000.0
        if (elapsedMinutes <= 0.0 || elapsedMinutes > 180.0) return@mapNotNull null
        val midpoint = start.timestamp + (end.timestamp - start.timestamp) / 2
        val hasEvent = meals.any { isValidPredictionMeal(it) && midpoint in (it.mealStartAt - 30 * 60_000L)..(it.mealEndAt + 4 * 60 * 60_000L) } ||
            medications.any { midpoint in (it.timestamp - 2 * 60 * 60_000L)..(it.timestamp + 12 * 60 * 60_000L) } ||
            sleepRecords.any { it.sleepStartAt <= midpoint && (it.wakeUpAt ?: midpoint) >= midpoint }
        if (hasEvent) return@mapNotNull null
        val declinePerMinute = (start.valueMmolPerL - end.valueMmolPerL) / elapsedMinutes
        declinePerMinute.takeIf { it > 0.0 }
    }
    return rates.sorted().medianOr(0.003).coerceIn(0.001, 0.02)
}

private fun timeOfDayBucket(timestamp: Long, zoneId: ZoneId): Int {
    val local = Instant.ofEpochMilli(timestamp).atZone(zoneId)
    return (local.hour * 60 + local.minute) / 30
}

private fun List<Double>.medianOr(fallback: Double): Double {
    if (isEmpty()) return fallback
    return this[size / 2]
}

private fun repeatingMealEffect(meals: List<DietRecord>, timestamp: Long, zoneId: ZoneId): Double {
    val local = Instant.ofEpochMilli(timestamp).atZone(zoneId)
    val minute = local.hour * 60 + local.minute
    return meals.sumOf { meal ->
        if (!isValidPredictionMeal(meal)) return@sumOf 0.0
        val mealLocal = Instant.ofEpochMilli(meal.mealStartAt).atZone(zoneId)
        val mealMinute = mealLocal.hour * 60 + mealLocal.minute
        val elapsed = minute - mealMinute
        if (elapsed !in 0..240) return@sumOf 0.0
        mealEffect(meal, meal.mealStartAt + elapsed * 60_000L)
    } / meals.size.coerceAtLeast(1)
}

private fun repeatingSleepEffect(sleeps: List<SleepRecord>, timestamp: Long, zoneId: ZoneId): Double {
    if (sleeps.isEmpty()) return 0.0
    val localMinute = Instant.ofEpochMilli(timestamp).atZone(zoneId).let { it.hour * 60 + it.minute }
    return sleeps.sumOf { sleep ->
        val wake = sleep.wakeUpAt ?: return@sumOf 0.0
        val wakeLocal = Instant.ofEpochMilli(wake).atZone(zoneId)
        val elapsed = localMinute - (wakeLocal.hour * 60 + wakeLocal.minute)
        if (elapsed in 0..120) 0.05 * exp(-elapsed / 60.0) else 0.0
    } / sleeps.size
}

private fun repeatingMedicationEffect(
    responses: List<MedicationResponse>,
    timestamp: Long,
    zoneId: ZoneId,
): Double {
    if (responses.isEmpty()) return 0.0
    val localMinute = Instant.ofEpochMilli(timestamp).atZone(zoneId).let { it.hour * 60 + it.minute }
    return responses.sumOf { response ->
        val eventMinute = Instant.ofEpochMilli(response.timestamp).atZone(zoneId).let { it.hour * 60 + it.minute }
        val elapsed = localMinute - eventMinute
        if (elapsed in 0..360) medicationEffect(response, response.timestamp + elapsed * 60_000L) else 0.0
    } / responses.size
}

internal fun evaluatePredictionEligibility(
    records: List<BloodGlucoseRecord>,
    medications: List<MedicationRecord>,
    meals: List<DietRecord>,
    sleepRecords: List<SleepRecord>,
): BloodGlucosePredictionEligibility {
    val observed = records
        .filter { it.timestamp > 0L && it.valueMmolPerL in 1.1..33.3 }
        .distinctBy(BloodGlucoseRecord::timestamp)
        .sortedBy(BloodGlucoseRecord::timestamp)
    val validMeals = meals.filter(::isValidPredictionMeal)
    val covered = observed.count { record ->
        validMeals.any { record.timestamp in (it.mealStartAt - 30 * 60_000L)..(it.mealEndAt + 4 * 60 * 60_000L) } ||
            medications.any { record.timestamp in (it.timestamp - 2 * 60 * 60_000L)..(it.timestamp + 12 * 60 * 60_000L) } ||
            sleepRecords.any { sleep -> record.timestamp in sleep.sleepStartAt..((sleep.wakeUpAt ?: sleep.sleepStartAt) + 2 * 60 * 60_000L) }
    }
    val historyDays = if (observed.size < 2) 0L else
        (observed.last().timestamp - observed.first().timestamp) / (24 * 60 * 60_000L)
    return BloodGlucosePredictionEligibility(historyDays, observed.size, covered, validMeals.size)
}

internal fun nextPredictionGrid(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val local = Instant.ofEpochMilli(nowMillis).atZone(zoneId).truncatedTo(ChronoUnit.MINUTES)
    val minute = local.minute
    val offsetMinutes = 5 - minute % 5
    return local.plusMinutes(offsetMinutes.toLong()).toInstant().toEpochMilli()
}

private fun mealEffect(record: DietRecord, timestamp: Long): Double {
    val minutes = (timestamp - record.mealStartAt) / 60_000.0
    if (minutes !in 0.0..240.0) return 0.0
    val nutrients = record.entries.flatMap { it.resolvedNutrients.entries }.groupBy({ it.key }, { it.value.value }).mapValues { it.value.sum() }
    val carbs = nutrients["CHO"] ?: return 0.0
    val fat = nutrients["FAT"] ?: return 0.0
    val shape = (minutes / 45.0) * exp(1.0 - minutes / 45.0)
    return (carbs * 0.025 + fat * 0.004) * shape
}

private data class MedicationResponse(val timestamp: Long, val change: Double)

private fun medicationResponses(records: List<MedicationRecord>, observed: List<BloodGlucoseRecord>): List<MedicationResponse> =
    records.mapNotNull { record ->
        if (record.medicationId == null) return@mapNotNull null
        val responses = observed.filter { it.timestamp in (record.timestamp + 30 * 60_000L)..(record.timestamp + 6 * 60 * 60_000L) }
        if (responses.size < 2) null else MedicationResponse(record.timestamp, responses.last().valueMmolPerL - responses.first().valueMmolPerL)
    }

private fun medicationEffect(response: MedicationResponse, timestamp: Long): Double {
    val minutes = (timestamp - response.timestamp) / 60_000.0
    if (minutes !in 0.0..360.0) return 0.0
    val shape = (minutes / 90.0) * exp(1.0 - minutes / 90.0)
    return response.change.coerceIn(-4.0, 4.0) * shape
}

private fun sleepEffect(record: SleepRecord, timestamp: Long): Double {
    val wake = record.wakeUpAt ?: return 0.0
    val minutesAfterWake = (timestamp - wake) / 60_000.0
    return if (minutesAfterWake in 0.0..120.0) 0.05 * exp(-minutesAfterWake / 60.0) else 0.0
}
