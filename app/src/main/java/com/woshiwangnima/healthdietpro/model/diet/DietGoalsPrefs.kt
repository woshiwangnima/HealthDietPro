package com.woshiwangnima.healthdietpro.model.diet

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.Serializable

/** 每日推荐摄入目标（按用户隔离）。能量为千卡，其余营养素为克。 */
@Serializable
internal data class DietGoalsPrefs(
    val energyKcal: Int = DEFAULT_ENERGY_KCAL,
    val carbsGrams: Int = DEFAULT_CARBS_GRAMS,
    val proteinGrams: Int = DEFAULT_PROTEIN_GRAMS,
    val fatGrams: Int = DEFAULT_FAT_GRAMS,
)

internal const val DEFAULT_ENERGY_KCAL = 2000
internal const val DEFAULT_CARBS_GRAMS = 250
internal const val DEFAULT_PROTEIN_GRAMS = 65
internal const val DEFAULT_FAT_GRAMS = 65

internal fun loadDietGoalsPrefs(context: Context): DietGoalsPrefs {
    val scope = UserPrefs.current(context)
    return DietGoalsPrefs(
        energyKcal = scope.getInt("diet_goals_energy_kcal", DEFAULT_ENERGY_KCAL),
        carbsGrams = scope.getInt("diet_goals_carbs_grams", DEFAULT_CARBS_GRAMS),
        proteinGrams = scope.getInt("diet_goals_protein_grams", DEFAULT_PROTEIN_GRAMS),
        fatGrams = scope.getInt("diet_goals_fat_grams", DEFAULT_FAT_GRAMS),
    )
}

internal fun saveDietGoalsPrefs(context: Context, goals: DietGoalsPrefs) {
    val scope = UserPrefs.current(context)
    scope.putInt("diet_goals_energy_kcal", goals.energyKcal.coerceAtLeast(0))
    scope.putInt("diet_goals_carbs_grams", goals.carbsGrams.coerceAtLeast(0))
    scope.putInt("diet_goals_protein_grams", goals.proteinGrams.coerceAtLeast(0))
    scope.putInt("diet_goals_fat_grams", goals.fatGrams.coerceAtLeast(0))
}

/** 依据体重/身高/年龄/性别按 Mifflin-St Jeor 估算每日推荐摄入。缺失体重时回退默认值。 */
internal fun recommendedDietGoals(
    weightKg: Double?,
    heightCm: Double?,
    age: Int?,
    isMale: Boolean?,
): DietGoalsPrefs {
    if (weightKg == null || weightKg <= 0.0) return DietGoalsPrefs()
    val bmr = 10.0 * weightKg +
        6.25 * (heightCm ?: 170.0) -
        5.0 * (age ?: 30) +
        if (isMale == true) 5.0 else -161.0
    val energy = (bmr * LIGHT_ACTIVITY_FACTOR).toInt().coerceIn(MIN_ENERGY_KCAL, MAX_ENERGY_KCAL)
    val protein = (weightKg * 0.8).toInt().coerceIn(MIN_PROTEIN_GRAMS, MAX_PROTEIN_GRAMS)
    val fat = (energy * 0.25 / 9.0).toInt().coerceAtLeast(MIN_FAT_GRAMS)
    val carbs = ((energy - protein * 4 - fat * 9) / 4.0).toInt().coerceAtLeast(MIN_CARBS_GRAMS)
    return DietGoalsPrefs(energyKcal = energy, carbsGrams = carbs, proteinGrams = protein, fatGrams = fat)
}

private const val LIGHT_ACTIVITY_FACTOR = 1.375
private const val MIN_ENERGY_KCAL = 1200
private const val MAX_ENERGY_KCAL = 5000
private const val MIN_PROTEIN_GRAMS = 40
private const val MAX_PROTEIN_GRAMS = 200
private const val MIN_FAT_GRAMS = 30
private const val MIN_CARBS_GRAMS = 50
