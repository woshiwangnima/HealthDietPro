package com.woshiwangnima.healthdietpro.ui.sleep

import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 判脏规则的单测。
 *
 * 这块逻辑在真机上的失败方式很隐蔽：不崩、不报错，只是「按返回直接退走」或
 * 「什么都没改也弹放弃确认」，靠点屏幕很难每次都复现。抽成纯函数之后，
 * 三个场景（新增未改 / 新增改字段 / 编辑改计时器）各自有断言。
 */
class SleepEditorDirtyTest {

    private val hour = 60L * 60 * 1000

    /** 新增页：默认值刚填好的那张表单（夜 22:00 → 次日 06:00）。 */
    private fun baselineForm() = SleepRecord(
        id = "",
        kind = SleepKind.NIGHT_SLEEP,
        sleepStartAt = 100 * hour,
        wakeUpAt = 108 * hour,
        recordedAt = 108 * hour,
    )

    @Test
    fun `新增页什么都没改不算修改`() {
        assertFalse(
            sleepFormHasChanges(
                current = baselineForm(),
                baseline = baselineForm(),
                timerVisible = true,
                timerEnabled = false,
            ),
        )
    }

    @Test
    fun `新增页换类别算修改`() {
        val current = baselineForm().copy(kind = SleepKind.NAP)
        assertTrue(sleepFormHasChanges(current, baselineForm(), timerVisible = true, timerEnabled = false))
    }

    @Test
    fun `新增页只改时间算修改`() {
        val current = baselineForm().copy(sleepStartAt = 101 * hour, wakeUpAt = 109 * hour)
        assertTrue(sleepFormHasChanges(current, baselineForm(), timerVisible = true, timerEnabled = false))
    }

    @Test
    fun `新增页只改备注算修改`() {
        val current = baselineForm().copy(note = "睡得还行")
        assertTrue(sleepFormHasChanges(current, baselineForm(), timerVisible = true, timerEnabled = false))
    }

    @Test
    fun `编辑页未改动不算修改`() {
        val existing = baselineForm().copy(id = "s1")
        // 开关初值就是「这条记录本来没有计时器」，也就是 false；两边一致才叫没改。
        assertFalse(sleepFormHasChanges(existing, existing, timerVisible = true, timerEnabled = false))
    }

    @Test
    fun `编辑页关掉已有计时器算修改`() {
        val existing = baselineForm().copy(id = "s1", timerId = "t1")
        assertTrue(
            sleepFormHasChanges(
                current = existing,
                baseline = existing,
                timerVisible = true,
                timerEnabled = false,
            ),
        )
    }

    @Test
    fun `醒来时间在过去时拨开计时器开关不算修改`() {
        // timerVisible=false：这时候开关拨了也存不下计时器，不该因此拦着用户离开。
        assertFalse(
            sleepFormHasChanges(
                current = baselineForm(),
                baseline = baselineForm(),
                timerVisible = false,
                timerEnabled = true,
            ),
        )
    }

    @Test
    fun `醒来时间在未来时拨开计时器开关算修改`() {
        assertTrue(
            sleepFormHasChanges(
                current = baselineForm(),
                baseline = baselineForm(),
                timerVisible = true,
                timerEnabled = true,
            ),
        )
    }

    @Test
    fun `进行中的记录只有醒来时间在未来才算有计时器可配`() {
        val now = 110 * hour
        assertTrue(sleepTimerVisible(wakeUpAt = 112 * hour, nowMillis = now))
        assertFalse(sleepTimerVisible(wakeUpAt = 109 * hour, nowMillis = now))
        assertFalse(sleepTimerVisible(wakeUpAt = null, nowMillis = now))
    }
}
