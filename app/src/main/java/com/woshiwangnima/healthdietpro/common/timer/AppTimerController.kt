package com.woshiwangnima.healthdietpro.common.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 计时器控制器实现：持有 [TimerArchiveStore] 持久化与计时协程。
 * 计时结束发通知；`notifyViaSystem` 时联动系统闹钟/计时器（显式声明，UI 层须二次确认）。
 */
internal class AppTimerController(
    private val context: Context,
    private val scope: CoroutineScope,
) : TimerController {
    private val store = TimerArchiveStore.current(context)
    private val jobs = mutableMapOf<String, Job>()

    init { ensureFinishedTransitions() }

    override fun list(): List<TimerInstance> = synchronized(lock) {
        val now = System.currentTimeMillis()
        store.load().map { it.advance(now) }
    }

    override fun createAndStart(label: String, totalMinutes: Int, notifyViaSystem: Boolean): TimerInstance {
        require(totalMinutes > 0) { "Timer duration must be positive" }
        val instance = TimerInstance(
            id = UUID.randomUUID().toString(),
            label = label.trim().takeIf(String::isNotBlank) ?: label,
            totalMinutes = totalMinutes,
            startedAtMillis = System.currentTimeMillis(),
            remainingSeconds = totalMinutes.toLong() * 60L,
            state = TimerState.RUNNING,
            notifyViaSystem = notifyViaSystem,
        )
        store.update { it + instance }
        scheduleTick(instance.id)
        return instance
    }

    override fun pause(id: String) = mutate(id) { it.pause(System.currentTimeMillis()) }

    override fun resume(id: String) = mutate(id) { it.resume(System.currentTimeMillis()) }

    override fun reset(id: String) {
        store.update { instances ->
            instances.map { instance ->
                if (instance.id == id) instance.reset() else instance
            }
        }
        jobs.remove(id)?.cancel()
    }

    override fun delete(id: String) {
        jobs.remove(id)?.cancel()
        store.update { instances -> instances.filterNot { it.id == id } }
    }

    private fun mutate(id: String, transform: (TimerInstance) -> TimerInstance) {
        store.update { instances ->
            instances.map { instance -> if (instance.id == id) transform(instance) else instance }
        }
        jobs.remove(id)?.cancel()
        if (list().firstOrNull { it.id == id }?.state == TimerState.RUNNING) scheduleTick(id)
    }

    private fun scheduleTick(id: String) {
        jobs[id]?.cancel()
        jobs[id] = scope.launch {
            while (isActive) {
                delay(1_000L)
                val updated = store.update { instances ->
                    instances.map { instance ->
                        if (instance.id == id) instance.advance(System.currentTimeMillis()) else instance
                    }
                }
                val finished = updated?.firstOrNull { it.id == id }?.state == TimerState.FINISHED
                if (finished) {
                    onFinished(id)
                    return@launch
                }
            }
        }
    }

    private fun onFinished(id: String) {
        val instance = list().firstOrNull { it.id == id } ?: return
        jobs.remove(id)?.cancel()
        if (instance.notifyViaSystem) {
            launchSystemTimer(instance)
        } else {
            notifyInApp(instance)
        }
    }

    private fun ensureFinishedTransitions() {
        // Re-run advance once on load so a timer that expired while the process was dead is finished.
        store.update { instances -> instances.map { it.advance(System.currentTimeMillis()) } }
    }

    private fun launchSystemTimer(instance: TimerInstance) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, instance.totalMinutes * 60)
            putExtra(AlarmClock.EXTRA_MESSAGE, instance.label)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }.onFailure { notifyInApp(instance) }
    }

    private fun notifyInApp(instance: TimerInstance) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel()
        NotificationManagerCompat.from(context).notify(
            instance.id.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_time)
                .setContentTitle(context.getString(R.string.timer_finished_title))
                .setContentText(context.getString(R.string.timer_finished_message, instance.label))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.timer_channel_name), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    private companion object {
        const val CHANNEL_ID = "countdown_timer"
        val lock = Any()
    }
}