package com.woshiwangnima.healthdietpro.model.bloodglucose

import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.exp
import kotlin.math.ln

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
    val formula: BloodGlucosePredictionFormula,
)

/** The exact model parameters used to generate one prediction batch. */
internal data class BloodGlucosePredictionFormula(
    val baseMmolPerL: Double,
    val startMmolPerL: Double,
    val baselineDecayPerHour: Double,
    val carbsCoefficientPerGram: Double,
    val mealPeakMinutes: Double,
    val drugFormulas: List<BloodGlucoseDrugFormula>,
    val sleepFormula: BloodGlucoseSleepFormula,
)

internal data class BloodGlucoseDrugFormula(
    val medicationId: String,
    val medicationName: String,
    val coefficientPerDose: Double,
    val peakMinutes: Double,
    val doseValues: List<Double>,
)

internal data class BloodGlucoseSleepFormula(
    val slowMmolPerL: Double,
    val stressCoefficientMmolPerL: Double,
    val stressDecayPerHour: Double,
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
    val start = nextPredictionGrid(nowMillis, zoneId)
    val end = start + PREDICTION_DURATION_MILLIS
    val recent = observed.filter { it.timestamp >= nowMillis - 72 * 60 * 60_000L }.ifEmpty { observed.takeLast(12) }
    val base = estimateBaselineLevel(observed, nowMillis, zoneId)
    val startValue = estimatePredictionStart(observed, nowMillis, zoneId, base)
    val validMeals = meals.filter(::isValidPredictionMeal)
    val baselineRate = estimateBaselineDecayRate(observed, base, validMeals, medications, sleepRecords)
    val mealFormula = fitMealFormula(observed, validMeals)
    val drugFormulas = fitDrugFormulas(observed, medications)
    val sleepFormula = fitSleepFormula(observed, sleepRecords, base)
    val points = generateSequence(start) { timestamp -> (timestamp + PREDICTION_INTERVAL_MILLIS).takeIf { it <= end } }
        .map { timestamp ->
            val elapsedHours = (timestamp - nowMillis).coerceAtLeast(0L) / 3_600_000.0
            val value = base + (startValue - base) * kotlin.math.exp(-baselineRate * elapsedHours)
            val mealEffect = validMeals.sumOf { meal -> mealGlucoseEffect(meal, timestamp, mealFormula) }
            val drugEffect = medications.sumOf { medication -> drugGlucoseEffect(medication, timestamp, drugFormulas) }
            val sleepEffect = sleepGlucoseEffect(timestamp, sleepRecords, sleepFormula)
            BloodGlucosePredictionPoint(timestamp, (value + mealEffect + drugEffect + sleepEffect).coerceIn(1.1, 33.3))
        }
        .toList()
    val confidence = when {
        recent.size >= 24 && validMeals.size >= 5 -> BloodGlucosePredictionConfidence.HIGH
        recent.size >= 8 -> BloodGlucosePredictionConfidence.MEDIUM
        else -> BloodGlucosePredictionConfidence.LOW
    }
    return BloodGlucosePredictionResult(
        points = points,
        confidence = confidence,
        formula = BloodGlucosePredictionFormula(
            baseMmolPerL = base,
            startMmolPerL = startValue,
            baselineDecayPerHour = baselineRate,
            carbsCoefficientPerGram = mealFormula.carbsCoefficientPerGram,
            mealPeakMinutes = mealFormula.peakMinutes,
            drugFormulas = drugFormulas,
            sleepFormula = sleepFormula,
        ),
    )
}

private const val BASELINE_DECAY_DEFAULT_PER_HOUR = 0.15
private const val BASELINE_DECAY_MIN_PER_HOUR = 0.01
private const val BASELINE_DECAY_MAX_PER_HOUR = 1.50
private const val BASELINE_RATE_MIN_GAP_MINUTES = 5.0
private const val BASELINE_RATE_MAX_GAP_MINUTES = 180.0
private const val SAME_TIME_BUCKET_MINUTES = 30
private const val BASELINE_NIGHT_START_MINUTE = 3 * 60
private const val BASELINE_NIGHT_END_MINUTE = 5 * 60
private const val MEAL_RESPONSE_WINDOW_MINUTES = 240.0
private const val MEAL_DEFAULT_CARBS_COEFFICIENT = 0.025
private const val MEAL_DEFAULT_PEAK_MINUTES = 45.0
private const val DRUG_DEFAULT_COEFFICIENT_PER_DOSE = 0.15
private const val DRUG_DEFAULT_PEAK_MINUTES = 120.0
private const val DRUG_RESPONSE_START_MINUTES = 20.0
private const val DRUG_RESPONSE_END_MINUTES = 480.0
private const val SLEEP_WAKE_RESPONSE_MINUTES = 360.0
private const val SLEEP_DEFAULT_SLOW_MG_DL = 0.0
private const val SLEEP_DEFAULT_STRESS_COEFFICIENT_MG_DL = 0.15
private const val SLEEP_DEFAULT_STRESS_DECAY_PER_HOUR = 0.50
private const val MG_DL_TO_MMOL_PER_L = 18.0

private data class MealFormula(
    val carbsCoefficientPerGram: Double,
    val peakMinutes: Double,
)

private fun estimateBaselineLevel(
    records: List<BloodGlucoseRecord>,
    nowMillis: Long,
    zoneId: ZoneId,
): Double {
    val windows = listOf(7L, 30L, 60L, 90L, Long.MAX_VALUE)
    val now = Instant.ofEpochMilli(nowMillis)
    return windows.firstNotNullOfOrNull { days ->
        val from = if (days == Long.MAX_VALUE) Long.MIN_VALUE else now.minus(days, ChronoUnit.DAYS).toEpochMilli()
        val windowRecords = records.filter { it.timestamp >= from && it.timestamp <= nowMillis }
        val nightValues = windowRecords.filter { isBaselineNightTime(it.timestamp, zoneId) }
            .map(BloodGlucoseRecord::valueMmolPerL)
        nightValues.averageOrNull() ?: windowRecords.map(BloodGlucoseRecord::valueMmolPerL).averageOrNull()
    } ?: records.map(BloodGlucoseRecord::valueMmolPerL).average()
}

private fun estimatePredictionStart(
    records: List<BloodGlucoseRecord>,
    nowMillis: Long,
    zoneId: ZoneId,
    base: Double,
): Double {
    val now = Instant.ofEpochMilli(nowMillis)
    val windows = listOf(7L, 30L, 60L, 90L, Long.MAX_VALUE)
    return windows.firstNotNullOfOrNull { days ->
        val from = if (days == Long.MAX_VALUE) Long.MIN_VALUE else now.minus(days, ChronoUnit.DAYS).toEpochMilli()
        val sameTimeValues = records
            .filter {
                it.timestamp in from..nowMillis &&
                    timeOfDayBucket(it.timestamp, zoneId) == timeOfDayBucket(nowMillis, zoneId)
            }
            .map(BloodGlucoseRecord::valueMmolPerL)
        sameTimeValues.takeIf { it.size >= 2 }?.averageOrNull()
            ?: sameTimeValues.sorted().medianOrNull()
    } ?: base
}

private fun estimateBaselineDecayRate(
    records: List<BloodGlucoseRecord>,
    base: Double,
    meals: List<DietRecord>,
    medications: List<MedicationRecord>,
    sleepRecords: List<SleepRecord>,
): Double {
    val rates = records.zipWithNext().mapNotNull { (start, end) ->
        val elapsedMinutes = (end.timestamp - start.timestamp) / 60_000.0
        if (elapsedMinutes !in BASELINE_RATE_MIN_GAP_MINUTES..BASELINE_RATE_MAX_GAP_MINUTES) return@mapNotNull null
        if (hasPredictionEventBetween(start.timestamp, end.timestamp, meals, medications, sleepRecords)) {
            return@mapNotNull null
        }
        val startDistance = start.valueMmolPerL - base
        val endDistance = end.valueMmolPerL - base
        if (startDistance <= 0.25 || endDistance <= 0.0 || endDistance >= startDistance) return@mapNotNull null
        (-ln(endDistance / startDistance) / (elapsedMinutes / 60.0))
            .takeIf { it.isFinite() && it > 0.0 }
    }
    return rates.sorted().medianOr(BASELINE_DECAY_DEFAULT_PER_HOUR)
        .coerceIn(BASELINE_DECAY_MIN_PER_HOUR, BASELINE_DECAY_MAX_PER_HOUR)
}

private fun fitMealFormula(
    records: List<BloodGlucoseRecord>,
    meals: List<DietRecord>,
): MealFormula {
    val samples = meals.mapNotNull { meal ->
        val carbs = meal.entries.sumOf { it.resolvedNutrients["CHO"]?.value ?: 0.0 }
        if (carbs <= 0.0 || !carbs.isFinite()) return@mapNotNull null
        val preMeal = records.lastOrNull {
            it.timestamp in (meal.mealStartAt - 2 * 60 * 60_000L)..meal.mealStartAt
        } ?: return@mapNotNull null
        val peak = records
            .filter { it.timestamp in meal.mealStartAt..(meal.mealStartAt + MEAL_RESPONSE_WINDOW_MINUTES.toLong() * 60_000L) }
            .maxByOrNull(BloodGlucoseRecord::valueMmolPerL)
            ?: return@mapNotNull null
        val peakMinutes = (peak.timestamp - meal.mealStartAt) / 60_000.0
        val rise = peak.valueMmolPerL - preMeal.valueMmolPerL
        if (rise <= 0.0 || peakMinutes <= 0.0) return@mapNotNull null
        rise / carbs to peakMinutes
    }
    return MealFormula(
        carbsCoefficientPerGram = samples.map { it.first }.medianOr(MEAL_DEFAULT_CARBS_COEFFICIENT)
            .coerceIn(0.0001, 0.25),
        peakMinutes = samples.map { it.second }.medianOr(MEAL_DEFAULT_PEAK_MINUTES)
            .coerceIn(15.0, MEAL_RESPONSE_WINDOW_MINUTES),
    )
}

private fun mealGlucoseEffect(
    meal: DietRecord,
    timestamp: Long,
    formula: MealFormula,
): Double {
    val elapsedMinutes = (timestamp - meal.mealStartAt) / 60_000.0
    if (elapsedMinutes < 0.0) return 0.0
    val carbs = meal.entries.sumOf { it.resolvedNutrients["CHO"]?.value ?: 0.0 }
    if (carbs <= 0.0 || !carbs.isFinite()) return 0.0
    val normalizedTime = elapsedMinutes / formula.peakMinutes
    return formula.carbsCoefficientPerGram * carbs * normalizedTime * exp(1.0 - normalizedTime)
}

private fun fitDrugFormulas(
    records: List<BloodGlucoseRecord>,
    medications: List<MedicationRecord>,
): List<BloodGlucoseDrugFormula> = medications
    .filter { !it.medicationId.isNullOrBlank() && it.doseValue.isFinite() && it.doseValue > 0f }
    .groupBy { it.medicationId!! }
    .map { (medicationId, medicationRecords) ->
        val samples = medicationRecords.mapNotNull { medication ->
            val dose = medication.doseValue.toDouble()
            val before = records.lastOrNull {
                it.timestamp in (medication.timestamp - 2 * 60 * 60_000L)..medication.timestamp
            } ?: return@mapNotNull null
            val response = records
                .filter {
                    it.timestamp in (medication.timestamp + DRUG_RESPONSE_START_MINUTES.toLong() * 60_000L)..(medication.timestamp + DRUG_RESPONSE_END_MINUTES.toLong() * 60_000L) &&
                        !hasPredictionEventBetween(medication.timestamp, it.timestamp, emptyList(), emptyList(), emptyList())
                }
                .minByOrNull(BloodGlucoseRecord::valueMmolPerL)
                ?: return@mapNotNull null
            val peakMinutes = (response.timestamp - medication.timestamp) / 60_000.0
            val fall = before.valueMmolPerL - response.valueMmolPerL
            if (fall <= 0.0 || peakMinutes !in DRUG_RESPONSE_START_MINUTES..DRUG_RESPONSE_END_MINUTES) return@mapNotNull null
            fall / dose to peakMinutes
        }
        BloodGlucoseDrugFormula(
            medicationId = medicationId,
            medicationName = medicationRecords.first().medicationName,
            coefficientPerDose = samples.map { it.first }.medianOr(DRUG_DEFAULT_COEFFICIENT_PER_DOSE)
                .coerceIn(0.0001, 4.0),
            peakMinutes = samples.map { it.second }.medianOr(DRUG_DEFAULT_PEAK_MINUTES)
                .coerceIn(DRUG_RESPONSE_START_MINUTES, DRUG_RESPONSE_END_MINUTES),
            doseValues = medicationRecords.map { it.doseValue.toDouble() },
        )
    }

private fun drugGlucoseEffect(
    medication: MedicationRecord,
    timestamp: Long,
    formulas: List<BloodGlucoseDrugFormula>,
): Double {
    val formula = formulas.firstOrNull { it.medicationId == medication.medicationId } ?: return 0.0
    val elapsedMinutes = (timestamp - medication.timestamp) / 60_000.0
    if (elapsedMinutes < 0.0) return 0.0
    val dose = medication.doseValue.toDouble()
    if (!dose.isFinite() || dose <= 0.0) return 0.0
    val normalizedTime = elapsedMinutes / formula.peakMinutes
    return -formula.coefficientPerDose * dose * normalizedTime * exp(1.0 - normalizedTime)
}

private fun fitSleepFormula(
    records: List<BloodGlucoseRecord>,
    sleepRecords: List<SleepRecord>,
    base: Double,
): BloodGlucoseSleepFormula {
    val samples = sleepRecords.mapNotNull { sleep ->
        val wake = sleep.wakeUpAt ?: return@mapNotNull null
        val durationHours = (wake - sleep.sleepStartAt) / 3_600_000.0
        if (durationHours <= 0.0) return@mapNotNull null
        val during = records.filter { it.timestamp in sleep.sleepStartAt..wake }.map(BloodGlucoseRecord::valueMmolPerL).averageOrNull()
        val afterWake = records.filter { it.timestamp in wake..(wake + SLEEP_WAKE_RESPONSE_MINUTES.toLong() * 60_000L) }
            .map(BloodGlucoseRecord::valueMmolPerL).averageOrNull()
        (during?.minus(base)?.let { it to durationHours }) to afterWake?.minus(base)
    }
    val slow = samples.mapNotNull { it.first?.first }
        .medianOr(SLEEP_DEFAULT_SLOW_MG_DL / MG_DL_TO_MMOL_PER_L)
    val stress = samples.mapNotNull { (during, after) ->
        val durationHours = during?.second ?: return@mapNotNull null
        val deficit = max(0.0, 8.0 - durationHours)
        if (deficit == 0.0 || after == null) null else max(0.0, after - slow) / deficit
    }.medianOr(SLEEP_DEFAULT_STRESS_COEFFICIENT_MG_DL / MG_DL_TO_MMOL_PER_L)
    return BloodGlucoseSleepFormula(
        slowMmolPerL = slow.coerceIn(-2.0, 2.0),
        stressCoefficientMmolPerL = stress.coerceIn(0.0, 1.0),
        stressDecayPerHour = SLEEP_DEFAULT_STRESS_DECAY_PER_HOUR,
    )
}

private fun sleepGlucoseEffect(
    timestamp: Long,
    sleepRecords: List<SleepRecord>,
    formula: BloodGlucoseSleepFormula,
): Double = sleepRecords.sumOf { sleep ->
    when {
        timestamp in sleep.sleepStartAt..(sleep.wakeUpAt ?: Long.MAX_VALUE) -> formula.slowMmolPerL
        sleep.wakeUpAt?.let { wake -> timestamp in wake..(wake + SLEEP_WAKE_RESPONSE_MINUTES.toLong() * 60_000L) } == true -> {
            val wake = sleep.wakeUpAt ?: return@sumOf 0.0
            val durationHours = ((wake - sleep.sleepStartAt) / 3_600_000.0).coerceAtLeast(0.0)
            val elapsedHours = (timestamp - wake) / 3_600_000.0
            formula.stressCoefficientMmolPerL * max(0.0, 8.0 - durationHours) * exp(-formula.stressDecayPerHour * elapsedHours)
        }
        else -> 0.0
    }
}

private fun hasPredictionEventBetween(
    startMillis: Long,
    endMillis: Long,
    meals: List<DietRecord>,
    medications: List<MedicationRecord>,
    sleepRecords: List<SleepRecord>,
): Boolean = meals.any { meal ->
    isValidPredictionMeal(meal) && meal.mealStartAt - 30 * 60_000L <= endMillis && meal.mealEndAt + 4 * 60 * 60_000L >= startMillis
} || medications.any { medication ->
    medication.timestamp - 2 * 60 * 60_000L <= endMillis && medication.timestamp + 12 * 60 * 60_000L >= startMillis
} || sleepRecords.any { sleep ->
    sleep.sleepStartAt <= endMillis && (sleep.wakeUpAt ?: sleep.sleepStartAt) + 2 * 60 * 60_000L >= startMillis
}

private fun timeOfDayBucket(timestamp: Long, zoneId: ZoneId): Int {
    val local = Instant.ofEpochMilli(timestamp).atZone(zoneId)
    return (local.hour * 60 + local.minute) / SAME_TIME_BUCKET_MINUTES
}

private fun List<Double>.medianOr(fallback: Double): Double {
    return medianOrNull() ?: fallback
}

private fun List<Double>.medianOrNull(): Double? {
    if (isEmpty()) return null
    val sorted = sorted()
    return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
    else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
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

private fun isBaselineNightTime(timestamp: Long, zoneId: ZoneId): Boolean {
    val local = Instant.ofEpochMilli(timestamp).atZone(zoneId)
    val minute = local.hour * 60 + local.minute
    return minute in BASELINE_NIGHT_START_MINUTE until BASELINE_NIGHT_END_MINUTE
}

private fun List<Double>.averageOrNull(): Double? =
    takeIf { isNotEmpty() }?.average()?.takeIf { it.isFinite() }
