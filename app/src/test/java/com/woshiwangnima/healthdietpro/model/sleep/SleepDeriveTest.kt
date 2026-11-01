package com.woshiwangnima.healthdietpro.model.sleep

import org.junit.Assert.assertEquals
import org.junit.Test

/** 编辑器里「按默认设置推另一端时间」的判定与算术。 */
class SleepDeriveTest {

    private fun prefs(
        nightMinutes: Int = 8 * 60,
        napMinutes: Int = 90,
        nightTiming: SleepRecordTiming = SleepRecordTiming.BEFORE_SLEEP,
        napTiming: SleepRecordTiming = SleepRecordTiming.BEFORE_SLEEP,
    ) = SleepPrefs(
        nightDefaultMinutes = nightMinutes,
        napDefaultMinutes = napMinutes,
        nightTiming = nightTiming,
        napTiming = napTiming,
    )

    @Test
    fun `睡前记录时醒来时间是被推导的那一端`() {
        assertEquals(
            SleepDerivedField.WAKE,
            sleepDerivedField(prefs(nightTiming = SleepRecordTiming.BEFORE_SLEEP), SleepKind.NIGHT_SLEEP),
        )
        assertEquals(
            SleepDerivedField.START,
            sleepDerivedField(prefs(nightTiming = SleepRecordTiming.AFTER_WAKE), SleepKind.NIGHT_SLEEP),
        )
    }

    @Test
    fun `两个类别各读各的时机设置`() {
        val mixed = prefs(nightTiming = SleepRecordTiming.AFTER_WAKE, napTiming = SleepRecordTiming.BEFORE_SLEEP)
        assertEquals(SleepDerivedField.START, sleepDerivedField(mixed, SleepKind.NIGHT_SLEEP))
        assertEquals(SleepDerivedField.WAKE, sleepDerivedField(mixed, SleepKind.NAP))
    }

    @Test
    fun `睡前八小时：醒来等于入睡加八小时`() {
        val start = 1_700_000_000_000L
        assertEquals(
            start + 8 * 60 * 60_000L,
            deriveSleepCounterpart(prefs(), SleepKind.NIGHT_SLEEP, start, deriveWake = true),
        )
    }

    @Test
    fun `起床八小时：入睡等于醒来减八小时`() {
        val wake = 1_700_000_000_000L
        assertEquals(
            wake - 8 * 60 * 60_000L,
            deriveSleepCounterpart(prefs(), SleepKind.NIGHT_SLEEP, wake, deriveWake = false),
        )
    }

    @Test
    fun `小憩用九十分钟而不是八小时`() {
        val anchor = 1_700_000_000_000L
        assertEquals(
            anchor + 90 * 60_000L,
            deriveSleepCounterpart(prefs(), SleepKind.NAP, anchor, deriveWake = true),
        )
    }

    @Test
    fun `默认时长按类别分别读取`() {
        assertEquals(480, sleepDefaultMinutes(prefs(nightMinutes = 480, napMinutes = 20), SleepKind.NIGHT_SLEEP))
        assertEquals(20, sleepDefaultMinutes(prefs(nightMinutes = 480, napMinutes = 20), SleepKind.NAP))
    }

    @Test
    fun `时长为零也不推成不动的时间`() {
        val anchor = 1_700_000_000_000L
        assertEquals(
            anchor + 60_000L,
            deriveSleepCounterpart(prefs(nightMinutes = 0), SleepKind.NIGHT_SLEEP, anchor, deriveWake = true),
        )
    }
}
