package com.woshiwangnima.healthdietpro.ui.sleep

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.common.timer.AppTimerController
import com.woshiwangnima.healthdietpro.common.timer.TimerController
import com.woshiwangnima.healthdietpro.common.timer.TimerInstance
import com.woshiwangnima.healthdietpro.common.timer.TimerState
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.SleepRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SleepRepository.fromContext(application)
    private val timerController: TimerController = AppTimerController(application, viewModelScope)

    private val _uiState = MutableStateFlow(SleepUiState())
    internal val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val records = repository.load().records.sortedByDescending(SleepRecord::sleepStartAt)
        val timers = timerController.list().associateBy(TimerInstance::id)
        _uiState.value = SleepUiState(records = records, timers = timers)
    }

    internal fun save(record: SleepRecord) {
        repository.upsert(record)
        refresh()
    }

    internal fun delete(id: String) {
        repository.delete(id)
        refresh()
    }

    internal fun wakeUpNow(id: String) {
        val record = _uiState.value.records.firstOrNull { it.id == id } ?: return
        if (record.wakeUpAt != null) return
        repository.upsert(record.copy(wakeUpAt = System.currentTimeMillis()))
        refresh()
    }

    internal fun createTimerAndStart(label: String, totalMinutes: Int, notifyViaSystem: Boolean): TimerInstance {
        val instance = timerController.createAndStart(label, totalMinutes, notifyViaSystem)
        refresh()
        return instance
    }

    internal fun deleteTimer(timerId: String) {
        timerController.delete(timerId)
        refresh()
    }
}

internal data class SleepUiState(
    val records: List<SleepRecord> = emptyList(),
    val timers: Map<String, TimerInstance> = emptyMap(),
) {
    fun timerFor(record: SleepRecord): TimerInstance? = record.timerId?.let(timers::get)
}

internal fun newSleepId(): String = UUID.randomUUID().toString()

internal fun TimerInstance.isRunning(): Boolean = state == TimerState.RUNNING
internal fun TimerInstance.isFinished(): Boolean = state == TimerState.FINISHED