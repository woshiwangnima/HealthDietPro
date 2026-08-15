package com.woshiwangnima.healthdietpro.ui.sleep

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord

/** 记睡眠：夜间睡眠 / 小憩记录（入睡/醒来/记录时间、备注、可选计时器）。 */
class SleepRecordActivity : BaseActivity() {
    companion object { const val EXTRA_OPEN_EDITOR = "open_editor" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HealthDietProTheme { SleepRoute(::finish, intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false)) } }
    }
}

private enum class SleepRoute { LIST, EDITOR }

@Composable
private fun SleepRoute(onFinish: () -> Unit, openEditorInitially: Boolean) {
    val viewModel: SleepViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(if (openEditorInitially) SleepRoute.EDITOR else SleepRoute.LIST) }
    var editingRecord by remember { mutableStateOf<SleepRecord?>(null) }

    BackHandler(enabled = route != SleepRoute.LIST) {
        if (openEditorInitially && route == SleepRoute.EDITOR) onFinish() else route = SleepRoute.LIST
    }

    when (route) {
        SleepRoute.LIST -> SleepListScreen(
            uiState = uiState,
            onAdd = { editingRecord = null; route = SleepRoute.EDITOR },
            onEdit = { editingRecord = it; route = SleepRoute.EDITOR },
            onDelete = viewModel::delete,
            onWakeUp = viewModel::wakeUpNow,
            onDeleteTimer = viewModel::deleteTimer,
            onBack = onFinish,
            modifier = Modifier,
        )
        SleepRoute.EDITOR -> SleepEditorScreen(
            existing = editingRecord,
            onBack = { route = SleepRoute.LIST },
            onSave = { record -> viewModel.save(record); route = SleepRoute.LIST },
            onCreateTimer = viewModel::createTimerAndStart,
            modifier = Modifier,
        )
    }
}