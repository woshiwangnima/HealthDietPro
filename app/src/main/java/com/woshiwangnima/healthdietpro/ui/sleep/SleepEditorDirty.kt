package com.woshiwangnima.healthdietpro.ui.sleep

import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord

/**
 * 「这张表单有未保存的修改吗」——返回键该不该问一句「要放弃吗」、编辑页的【保存】亮不亮，都取这里的答案。
 *
 * 判定对象是 [baseline]（进页面那一下的表单），不是「数据库里那条记录」：
 * 新增时没有任何记录可比，若拿 null 比，`current != null` 恒真，于是刚点开【记夜间睡眠】、
 * 一个字都没改，按返回也会被拦下来问「放弃未保存的修改」——默认起止时间是系统给的建议值，
 * 不算用户改动。
 *
 * 计时器单独算：它不在 [SleepRecord] 里（只在 [SleepRecord.timerId] 留一个 id），
 * 而「在编辑页把开关拨开」并不改记录内容。只在计时器真的会出现（[timerVisible]：
 * 醒来时间在未来）时才算数——否则开关拨了也存不下来一个计时器。
 */
internal fun sleepFormHasChanges(
    current: SleepRecord,
    baseline: SleepRecord,
    timerVisible: Boolean,
    timerEnabled: Boolean,
): Boolean {
    val timerBaseline = baseline.timerId != null
    return current != baseline || (timerVisible && timerEnabled != timerBaseline)
}

/** 醒来时间在未来时才有计时器可配。 */
internal fun sleepTimerVisible(
    wakeUpAt: Long?,
    nowMillis: Long,
): Boolean = wakeUpAt != null && wakeUpAt > nowMillis
