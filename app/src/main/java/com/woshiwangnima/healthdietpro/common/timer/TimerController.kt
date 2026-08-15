package com.woshiwangnima.healthdietpro.common.timer

/** 计时器控制器抽象（基础设施模块）。实现方持有 Android 依赖，界面经事件调用。 */
internal interface TimerController {
    /** 返回当前用户所有计时实例（含派生剩余秒数）。 */
    fun list(): List<TimerInstance>

    /** 创建并启动一个计时实例，返回实例 id。 */
    fun createAndStart(label: String, totalMinutes: Int, notifyViaSystem: Boolean): TimerInstance

    /** 暂停 / 恢复 / 重置。 */
    fun pause(id: String)

    fun resume(id: String)

    fun reset(id: String)

    fun delete(id: String)
}