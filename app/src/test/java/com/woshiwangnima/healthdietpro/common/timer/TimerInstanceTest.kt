package com.woshiwangnima.healthdietpro.common.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerInstanceTest {

    private fun instance(state: TimerState = TimerState.IDLE) = TimerInstance(
        id = "t1",
        label = "Nap",
        totalMinutes = 10,
        startedAtMillis = 1000L,
        remainingSeconds = 600L,
        state = state,
    )

    @Test
    fun `running remaining derives from start`() {
        val timer = instance(TimerState.RUNNING)
        assertEquals(540L, timer.remainingNow(61_000L))
    }

    @Test
    fun `advance clamps to zero and finishes`() {
        val finished = instance(TimerState.RUNNING).advance(1_000_000L)
        assertEquals(TimerState.FINISHED, finished.state)
        assertEquals(0L, finished.remainingSeconds)
    }

    @Test
    fun `advance preserves remaining while running`() {
        val timer = instance(TimerState.RUNNING).advance(60_000L)
        assertEquals(TimerState.RUNNING, timer.state)
        assertEquals(541L, timer.remainingSeconds)
    }

    /**
     * 每秒 tick 一次推进两次，剩余量必须线性下降。
     *
     * 旧实现只改 `remainingSeconds` 不改 `startedAtMillis`，第二次推进会再减掉同一段流逝，
     * 15 分钟的计时器四十来秒就报「结束」。
     */
    @Test
    fun `advance rebases start so repeated ticks stay linear`() {
        val first = instance(TimerState.RUNNING).advance(11_000L)
        val second = first.advance(21_000L)
        assertEquals(TimerState.RUNNING, second.state)
        assertEquals(590L, first.remainingSeconds)
        assertEquals(580L, second.remainingSeconds)
        assertEquals(21_000L, second.startedAtMillis)
    }

    @Test
    fun `advance counts down to real zero only`() {
        // 从 1 000 ms 起、共 600 s：600 次「过 1 秒」之后才该结束。
        var timer = instance(TimerState.RUNNING)
        var now = 1_000L
        repeat(599) {
            now += 1_000L
            timer = timer.advance(now)
        }
        assertEquals(TimerState.RUNNING, timer.state)
        now += 1_000L
        assertEquals(TimerState.FINISHED, timer.advance(now).state)
    }

    @Test
    fun `pause freezes remaining`() {
        val paused = instance(TimerState.RUNNING).pause(61_000L)
        assertEquals(TimerState.PAUSED, paused.state)
        assertEquals(540L, paused.remainingSeconds)
    }

    @Test
    fun `resume restarts clock`() {
        val resumed = instance(TimerState.PAUSED).resume(5_000L)
        assertEquals(TimerState.RUNNING, resumed.state)
        assertEquals(5_000L, resumed.startedAtMillis)
    }

    @Test
    fun `reset returns to idle`() {
        val reset = instance(TimerState.PAUSED).reset()
        assertEquals(TimerState.IDLE, reset.state)
        assertEquals(0L, reset.remainingSeconds)
    }

    @Test
    fun `idle total seconds equals total minutes`() {
        assertTrue(instance().totalSeconds() == 600L)
    }
}