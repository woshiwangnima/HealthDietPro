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
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord

/** 记睡眠：夜间睡眠 / 小憩记录（入睡/醒来/记录时间、备注、可选计时器）。 */
class SleepRecordActivity : BaseActivity() {
    companion object { const val EXTRA_OPEN_EDITOR = "open_editor" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 外层 HealthDietProTheme 只负责两件事：按 AppPrefs 解析深浅、把用户字号倍率塞进 LocalDensity。
            // 内层 AppTheme 提供设计令牌。两层必须嵌套：单独用 AppTheme 会丢掉字号倍率，
            // 单独用 HealthDietProTheme 则拿不到 tokens（§8.14 已在软件设置上验证过这个组合）。
            HealthDietProTheme {
                AppTheme(darkTheme = rememberAppDarkTheme()) {
                    SleepRoute(::finish, intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false))
                }
            }
        }
    }
}

private enum class SleepRoute { LIST, EDITOR, SETTINGS, DEFAULT_DURATION }

@Composable
private fun SleepRoute(onFinish: () -> Unit, openEditorInitially: Boolean) {
    val viewModel: SleepViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(if (openEditorInitially) SleepRoute.EDITOR else SleepRoute.LIST) }
    var editingRecord by remember { mutableStateOf<SleepRecord?>(null) }

    BackHandler(enabled = route != SleepRoute.LIST) {
        when {
            openEditorInitially && route == SleepRoute.EDITOR -> onFinish()
            route == SleepRoute.SETTINGS -> route = SleepRoute.LIST
            route == SleepRoute.DEFAULT_DURATION -> route = SleepRoute.SETTINGS
            else -> route = SleepRoute.LIST
        }
    }

    when (route) {
        SleepRoute.LIST -> SleepHomeScreen(
            uiState = uiState,
            onAdd = { editingRecord = null; route = SleepRoute.EDITOR },
            onEdit = { editingRecord = it; route = SleepRoute.EDITOR },
            onDelete = viewModel::delete,
            onWakeUp = viewModel::wakeUpNow,
            onDeleteTimer = viewModel::deleteTimer,
            onSettings = { route = SleepRoute.SETTINGS },
            onBack = onFinish,
            modifier = Modifier,
        )
        SleepRoute.EDITOR -> SleepEditorScreen(
            existing = editingRecord,
            prefs = prefs,
            onBack = { route = SleepRoute.LIST },
            onSave = { record -> viewModel.save(record); route = SleepRoute.LIST },
            onCreateTimer = viewModel::createTimerAndStart,
            modifier = Modifier,
        )
        SleepRoute.SETTINGS -> SleepSettingsScreen(
            prefs = prefs,
            onBack = { route = SleepRoute.LIST },
            onDefaultDuration = { route = SleepRoute.DEFAULT_DURATION },
        )
        SleepRoute.DEFAULT_DURATION -> SleepDefaultDurationScreen(
            prefs = prefs,
            onBack = { route = SleepRoute.SETTINGS },
            onSave = { updated -> viewModel.savePrefs(updated); route = SleepRoute.SETTINGS },
        )
    }
}