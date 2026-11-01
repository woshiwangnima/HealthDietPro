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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
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

// 页签 id 与 `AppPrefs` 里存的序号：统计在左（0）、记录在右（1），与物价 / 饮水页一致。
private const val TAB_STATISTICS = "statistics"
private const val TAB_RECORDS = "records"
private const val SLEEP_TAB_STATISTICS = 0
private const val SLEEP_TAB_RECORDS = 1

@Composable
private fun SleepRoute(onFinish: () -> Unit, openEditorInitially: Boolean) {
    val viewModel: SleepViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var route by rememberSaveable { mutableStateOf(if (openEditorInitially) SleepRoute.EDITOR else SleepRoute.LIST) }
    var editingRecord by remember { mutableStateOf<SleepRecord?>(null) }
    // 新增时记下用户点的是【记夜间睡眠】还是【记小憩】；编辑已存在的记录时以记录自身类别为准。
    var newRecordKind by rememberSaveable { mutableStateOf(SleepKind.NIGHT_SLEEP) }
    // 停在哪个页签写进存档（0=统计 / 1=记录），退出应用再进来仍是上次那一页。
    var sleepTabId by rememberSaveable {
        mutableStateOf(if (AppPrefs.getSleepTab(context) == SLEEP_TAB_STATISTICS) TAB_STATISTICS else TAB_RECORDS)
    }

    fun selectTab(id: String) {
        sleepTabId = id
        AppPrefs.setSleepTab(
            context,
            if (id == TAB_STATISTICS) SLEEP_TAB_STATISTICS else SLEEP_TAB_RECORDS,
        )
    }

    // 只接手「列表以外、但编辑器不自己管」的那两层（设置 / 默认时长）。
    // 编辑页**不在这里**：编辑器自己要问一句「要放弃未保存的修改吗」，而两个 BackHandler 同时
    // 开着时，谁先收到事件取决于注册顺序——结果是顶栏箭头会弹确认、系统返回键直接退走，
    // 同一个动作两种后果。返回键归编辑器，这里就退回一级。
    BackHandler(enabled = route == SleepRoute.SETTINGS || route == SleepRoute.DEFAULT_DURATION) {
        route = if (route == SleepRoute.DEFAULT_DURATION) SleepRoute.SETTINGS else SleepRoute.LIST
    }

    when (route) {
        SleepRoute.LIST -> SleepHomeScreen(
            uiState = uiState,
            selectedTabId = sleepTabId,
            onSelectTab = ::selectTab,
            onAdd = { kind ->
                editingRecord = null
                newRecordKind = kind
                route = SleepRoute.EDITOR
            },
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
            initialKind = newRecordKind,
            // 带 EXTRA_OPEN_EDITOR 进来时，编辑器就是这一屏的全部：没改动直接退出去，
            // 而不是落到记录列表（用户是从别处点「记一笔」过来的，不是来翻历史的）。
            onBack = { if (openEditorInitially) onFinish() else route = SleepRoute.LIST },
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