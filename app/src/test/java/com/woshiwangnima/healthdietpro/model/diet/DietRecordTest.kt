package com.woshiwangnima.healthdietpro.model.diet

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietRecordTest {

    private fun entry(
        foodName: String = "apple",
        weightValue: Double = 100.0,
        netGrams: Double = 100.0,
        foodId: String? = "f1",
        kind: com.woshiwangnima.healthdietpro.model.food.FoodKind? = com.woshiwangnima.healthdietpro.model.food.FoodKind.INGREDIENT,
    ) = DietFoodEntry(
        foodId = foodId,
        foodName = foodName,
        foodKind = kind,
        weightValue = weightValue,
        weightUnitId = "g",
        netWeightGrams = netGrams,
    )

    private fun record(
        id: String = "d1",
        start: Long = 1_000L * 60 * 60,
        end: Long = 1_000L * 60 * 60 + 30 * 60_000L,
        period: MealPeriod = MealPeriod.BREAKFAST,
        entries: List<DietFoodEntry> = listOf(entry()),
        recorded: Long = 2_000L * 60 * 60,
    ) = DietRecord(
        id = id,
        mealStartAt = start,
        mealEndAt = end,
        mealPeriod = period,
        entries = entries,
        recordedAt = recorded,
    )

    private fun millis(hour: Int, minute: Int, zone: ZoneId = ZoneId.of("Asia/Shanghai")): Long =
        LocalDate.of(2026, 8, 17).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `free name entry has no food id`() {
        assertTrue(entry(foodId = null, kind = null).isFreeName)
        assertFalse(entry().isFreeName)
    }

    @Test
    fun `meal period defaults from local time ranges`() {
        val zone = ZoneId.of("Asia/Shanghai")
        assertEquals(MealPeriod.PRE_BREAKFAST_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(5, 0, zone), zone))
        assertEquals(MealPeriod.PRE_BREAKFAST_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(6, 59, zone), zone))
        assertEquals(MealPeriod.BREAKFAST, MealPeriod.BREAKFAST.defaultForMillis(millis(7, 0, zone), zone))
        assertEquals(MealPeriod.BREAKFAST, MealPeriod.BREAKFAST.defaultForMillis(millis(9, 59, zone), zone))
        assertEquals(MealPeriod.MID_MORNING_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(10, 0, zone), zone))
        assertEquals(MealPeriod.MID_MORNING_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(11, 29, zone), zone))
        assertEquals(MealPeriod.LUNCH, MealPeriod.BREAKFAST.defaultForMillis(millis(11, 30, zone), zone))
        assertEquals(MealPeriod.LUNCH, MealPeriod.BREAKFAST.defaultForMillis(millis(13, 29, zone), zone))
        assertEquals(MealPeriod.MID_AFTERNOON_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(13, 30, zone), zone))
        assertEquals(MealPeriod.MID_AFTERNOON_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(17, 29, zone), zone))
        assertEquals(MealPeriod.DINNER, MealPeriod.BREAKFAST.defaultForMillis(millis(17, 30, zone), zone))
        assertEquals(MealPeriod.DINNER, MealPeriod.BREAKFAST.defaultForMillis(millis(19, 59, zone), zone))
        assertEquals(MealPeriod.POST_DINNER_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(20, 0, zone), zone))
        assertEquals(MealPeriod.POST_DINNER_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(23, 59, zone), zone))
        assertEquals(MealPeriod.POST_DINNER_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(0, 0, zone), zone))
        assertEquals(MealPeriod.POST_DINNER_SNACK, MealPeriod.BREAKFAST.defaultForMillis(millis(4, 59, zone), zone))
    }

    @Test
    fun `migration trims fields and normalizes entries`() {
        val archive = DietArchive(
            schemaVersion = 0,
            records = listOf(
                record(id = " d1 ", start = 1000L, end = 500L, recorded = 1000L)
                    .copy(note = "  hi  "),
                record(
                    id = "d2",
                    start = 2000L,
                    end = 2000L,
                    entries = listOf(
                        entry(foodName = "  banana ", weightValue = -5.0, netGrams = -1.0),
                    ),
                    recorded = 2000L,
                ),
            ),
        )
        val migrated = migrateDietArchive(archive)
        assertEquals(DIET_ARCHIVE_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(1, migrated.records.size)
        assertEquals("d1", migrated.records.first().id)
        assertEquals("hi", migrated.records.first().note)
        assertEquals(migrated.records.first().mealStartAt, migrated.records.first().mealEndAt)
    }

    @Test
    fun `migration deduplicates record ids keeping first`() {
        val archive = DietArchive(
            schemaVersion = 0,
            records = listOf(
                record(id = "dup", start = 1000L, recorded = 1000L),
                record(id = "dup", start = 3000L, recorded = 3000L),
            ),
        )
        val migrated = migrateDietArchive(archive)
        assertEquals(1, migrated.records.size)
        assertEquals(1000L, migrated.records.first().mealStartAt)
    }

    @Test
    fun `validation accepts valid archive`() {
        validateDietArchive(DietArchive(records = listOf(record())))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects duplicate ids`() {
        validateDietArchive(DietArchive(records = listOf(record(), record())))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects end before start`() {
        validateDietArchive(DietArchive(records = listOf(record(start = 5000L, end = 1000L))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects record without entries`() {
        validateDietArchive(DietArchive(records = listOf(record(entries = emptyList()))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects non positive net weight`() {
        validateDietArchive(DietArchive(records = listOf(record(entries = listOf(entry(netGrams = 0.0))))))
    }

    @Test
    fun `serializable nutrient snapshot keeps scaled values`() {
        val snapshot = DietNutrientAmount(value = 12.5, unitCategory = "energy", unitId = "kcal")
        val entry = entry().copy(resolvedNutrients = mapOf("ENERGY" to snapshot))
        assertEquals(12.5, entry.resolvedNutrients.getValue("ENERGY").value, 0.0)
        assertEquals("kcal", entry.resolvedNutrients.getValue("ENERGY").unitId)
    }

    @Test
    fun `default diet times before meal anchors start at now`() {
        val now = millis(8, 30)
        val prefs = DietPrefs(periods = mapOf(MealPeriod.BREAKFAST.name to DietPeriodPrefs(defaultMinutes = 45, timing = DietRecordTiming.BEFORE_MEAL)))
        val (start, end) = defaultDietTimes(prefs, MealPeriod.BREAKFAST, now)
        assertEquals(now, start)
        assertEquals(now + 45 * 60_000L, end)
    }

    @Test
    fun `default diet times after meal anchors end at now`() {
        val now = millis(12, 0)
        val prefs = DietPrefs(periods = mapOf(MealPeriod.LUNCH.name to DietPeriodPrefs(defaultMinutes = 30, timing = DietRecordTiming.AFTER_MEAL)))
        val (start, end) = defaultDietTimes(prefs, MealPeriod.LUNCH, now)
        assertEquals(now - 30 * 60_000L, start)
        assertEquals(now, end)
    }

    @Test
    fun `unconfigured period falls back to defaults`() {
        val prefs = DietPrefs(periods = emptyMap())
        assertEquals(DietPeriodPrefs().defaultMinutes, prefs.forPeriod(MealPeriod.DINNER).defaultMinutes)
        assertEquals(DietRecordTiming.BEFORE_MEAL, prefs.forPeriod(MealPeriod.DINNER).timing)
    }

    @Test
    fun `with period updates only that period`() {
        val base = DietPrefs(periods = mapOf(MealPeriod.BREAKFAST.name to DietPeriodPrefs(defaultMinutes = 45)))
        val updated = base.withPeriod(MealPeriod.LUNCH, DietPeriodPrefs(defaultMinutes = 20, timing = DietRecordTiming.AFTER_MEAL))
        assertEquals(45, updated.forPeriod(MealPeriod.BREAKFAST).defaultMinutes)
        assertEquals(20, updated.forPeriod(MealPeriod.LUNCH).defaultMinutes)
        assertEquals(DietRecordTiming.AFTER_MEAL, updated.forPeriod(MealPeriod.LUNCH).timing)
        assertEquals(DietPeriodPrefs().defaultMinutes, updated.forPeriod(MealPeriod.DINNER).defaultMinutes)
    }

    @Test
    fun `contains minute respects midnight spanning range`() {
        assertTrue(MealPeriod.POST_DINNER_SNACK.containsMinute(22 * 60, 20 * 60, 4 * 60 + 59))
        assertTrue(MealPeriod.POST_DINNER_SNACK.containsMinute(1 * 60 + 30, 20 * 60, 4 * 60 + 59))
        assertFalse(MealPeriod.POST_DINNER_SNACK.containsMinute(12 * 60, 20 * 60, 4 * 60 + 59))
        assertTrue(MealPeriod.BREAKFAST.containsMinute(8 * 60, 7 * 60, 9 * 60 + 59))
        assertFalse(MealPeriod.BREAKFAST.containsMinute(10 * 60, 7 * 60, 9 * 60 + 59))
    }

    @Test
    fun `resolve default prefers custom period range`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val prefs = DietPrefs(
            periods = mapOf(
                MealPeriod.LUNCH.name to DietPeriodPrefs(
                    defaultMinutes = 45,
                    rangeStartMinute = 11 * 60,
                    rangeEndMinute = 13 * 60 + 59,
                ),
            ),
        )
        assertEquals(MealPeriod.LUNCH, MealPeriod.BREAKFAST.resolveDefault(millis(12, 0, zone), prefs, zone))
        assertEquals(MealPeriod.BREAKFAST, MealPeriod.BREAKFAST.resolveDefault(millis(8, 0, zone), prefs, zone))
    }

    @Test
    fun `resolve default falls back to built-in ranges when no custom range`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val prefs = DietPrefs(periods = emptyMap())
        assertEquals(MealPeriod.DINNER, MealPeriod.BREAKFAST.resolveDefault(millis(18, 30, zone), prefs, zone))
    }

    @Test
    fun `recommended goals follow mifflin st jeor with profile data`() {
        val goals = recommendedDietGoals(weightKg = 70.0, heightCm = 175.0, age = 30, isMale = true)
        val bmr = 10.0 * 70 + 6.25 * 175 - 5.0 * 30 + 5
        val energy = (bmr * 1.375).toInt()
        assertEquals(energy, goals.energyKcal)
        assertEquals((70.0 * 0.8).toInt(), goals.proteinGrams)
        assertEquals((energy * 0.25 / 9.0).toInt(), goals.fatGrams)
        assertEquals(((energy - goals.proteinGrams * 4 - goals.fatGrams * 9) / 4.0).toInt(), goals.carbsGrams)
    }

    @Test
    fun `recommended goals fall back to defaults without weight`() {
        assertEquals(DietGoalsPrefs(), recommendedDietGoals(weightKg = null, heightCm = 175.0, age = 30, isMale = true))
    }
}
