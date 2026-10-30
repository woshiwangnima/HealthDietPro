package com.woshiwangnima.healthdietpro.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.cache.AppCacheKind
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ConfirmDialog
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadioRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.archive.appVersion
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

/**
 * 软件设置（应用级）。
 *
 * 分组只在代码里存在（`AppSectionGroup` 一张卡 = 一组），**不画分组标题**：
 * 用户看到的是一张张被间距分开的卡，而不是「常规 / 显示 / 通知与权限 / 存储 / 关于」这类需要先读一遍的目录。
 * 卡与卡之间用 `space.4` 拉开，卡内行与行之间只隔一条描边色的细线。
 *
 * 每行只留「标题 + 当前值」，说明文字全部收进长按浮层（`AppListRow(tooltip = …)`）：
 * 设置列表的价值在于一眼扫完所有选项，每行挂一句解释会把行高翻倍、把选项挤出屏幕。
 *
 * 语言 / 深色模式 / 每周起始日三者是同一类「从固定选项里挑一个」的设置，
 * 表现形式必须一致：都是「行显示当前值 → 点开弹层一次性列全所有选项 → 选中即生效并关闭」。
 * 语言此前单独开一页，是唯一的不一致项，现已并入同一个弹层。
 */
@Composable
internal fun AppSettingsScreen(
    onBack: () -> Unit,
    onOpenTextDisplay: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: AppSettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val tokens = LocalAppTokens.current
    val context = LocalContext.current
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var openPicker by remember { mutableStateOf<SettingsPicker?>(null) }
    var language by remember { mutableStateOf(AppPrefs.getAppLanguage(context)) }
    var darkMode by remember { mutableStateOf(AppPrefs.getDarkMode(context)) }
    var firstDayOfWeek by remember { mutableStateOf(AppPrefs.getFirstDayOfWeek(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val toastMbTemplate = stringResource(R.string.settings_cache_cleared_mb)
    val toastKbTemplate = stringResource(R.string.settings_cache_cleared_kb)
    val cacheLabels = mapOf(
        AppCacheKind.AppFiles to stringResource(R.string.settings_cache_entry_app_files),
        AppCacheKind.CodeFiles to stringResource(R.string.settings_cache_entry_code_files),
        AppCacheKind.ExternalFiles to stringResource(R.string.settings_cache_entry_external_files),
        AppCacheKind.FoodImages to stringResource(R.string.settings_cache_entry_food_images),
        AppCacheKind.FoodCardMetadata to stringResource(R.string.settings_cache_entry_food_card_metadata),
        AppCacheKind.ProfileAvatars to stringResource(R.string.settings_cache_entry_profile_avatars),
    )
    val cacheEntryTemplate = stringResource(R.string.settings_cache_entry)
    val cacheEntryWithCountTemplate = stringResource(R.string.settings_cache_entry_with_count)
    val cacheDetails = uiState.cacheEntries.joinToString(separator = "\n") { entry ->
        val label = requireNotNull(cacheLabels[entry.kind])
        if (entry.itemCount > 0) {
            cacheEntryWithCountTemplate.format(label, entry.sizeText, entry.itemCount)
        } else {
            cacheEntryTemplate.format(label, entry.sizeText)
        }
    }
    val clearCacheMessage = listOf(
        stringResource(R.string.settings_clear_cache_confirm_message),
        stringResource(R.string.settings_clear_cache_confirm_contents, cacheDetails),
    ).joinToString(separator = "\n\n")

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { toast ->
            val msg = when (toast) {
                is AppSettingsViewModel.Toast.Mb -> toastMbTemplate.format(toast.value)
                is AppSettingsViewModel.Toast.Kb -> toastKbTemplate.format(toast.value)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    if (showClearCacheConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_cache_confirm_title),
            message = clearCacheMessage,
            confirmText = stringResource(R.string.settings_clear_cache_confirm_ok),
            cancelText = stringResource(R.string.settings_clear_cache_cancel),
            onConfirm = {
                showClearCacheConfirm = false
                viewModel.clearCache()
            },
            onDismiss = { showClearCacheConfirm = false },
        )
    }

    // 三种「从固定选项里挑一个」的设置共用同一个弹层，连标题副标题的位置都一致。
    val picker = openPicker
    if (picker != null) {
        val options = when (picker) {
            SettingsPicker.Language -> LanguageOption.entries.map {
                SettingsOption(it.value, stringResource(it.labelRes))
            }
            SettingsPicker.DarkMode -> DarkModeOption.entries.map {
                SettingsOption(it.value, stringResource(it.labelRes))
            }
            SettingsPicker.FirstDayOfWeek -> FirstDayOption.entries.map {
                SettingsOption(it.value, stringResource(it.labelRes))
            }
        }
        val selected = when (picker) {
            SettingsPicker.Language -> language
            SettingsPicker.DarkMode -> darkMode
            SettingsPicker.FirstDayOfWeek -> firstDayOfWeek
        }
        SettingsOptionDialog(
            title = stringResource(picker.titleRes),
            description = stringResource(picker.descRes),
            options = options,
            selectedValue = selected,
            onDismiss = { openPicker = null },
            onSelect = { value ->
                openPicker = null
                when (picker) {
                    SettingsPicker.Language -> if (language != value) {
                        language = value
                        applyLanguage(context, value)
                    }
                    SettingsPicker.DarkMode -> if (darkMode != value) {
                        darkMode = value
                        AppPrefs.setDarkMode(context, value)
                        DarkModeOption.entries
                            .first { it.value == value }
                            .let { AppCompatDelegate.setDefaultNightMode(it.delegateMode) }
                    }
                    SettingsPicker.FirstDayOfWeek -> if (firstDayOfWeek != value) {
                        firstDayOfWeek = value
                        AppPrefs.setFirstDayOfWeek(context, value)
                    }
                }
            },
        )
    }

    BaseScreen(title = stringResource(R.string.settings_app_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s3),
            // 卡与卡之间留 16 dp：没有分组标题时，靠间距读「这里是另一组」。
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            // 常规：语言 / 深色模式 / 每周起始日
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.settings_language),
                    iconRes = R.drawable.ic_language,
                    trailingValue = languageLabel(language),
                    tooltip = stringResource(R.string.settings_language_desc),
                    onClick = { openPicker = SettingsPicker.Language },
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.settings_dark_mode),
                    iconRes = R.drawable.ic_dark_mode,
                    trailingValue = darkModeLabel(darkMode),
                    tooltip = stringResource(R.string.settings_dark_mode_desc),
                    onClick = { openPicker = SettingsPicker.DarkMode },
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.settings_first_day_of_week),
                    iconRes = R.drawable.ic_event,
                    trailingValue = firstDayLabel(firstDayOfWeek),
                    tooltip = stringResource(R.string.settings_first_day_of_week_desc),
                    onClick = { openPicker = SettingsPicker.FirstDayOfWeek },
                )
            }

            // 显示
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.text_display_settings_title),
                    iconRes = R.drawable.ic_font_size,
                    showChevron = true,
                    tooltip = stringResource(R.string.text_display_settings_desc),
                    onClick = onOpenTextDisplay,
                )
            }

            // 通知与权限
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.settings_message_notification),
                    iconRes = R.drawable.ic_bell,
                    showChevron = true,
                    tooltip = stringResource(R.string.settings_message_notification_desc),
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@AppListRow
                        }
                        val uid = try {
                            context.packageManager.getApplicationInfo(context.packageName, 0).uid
                        } catch (_: Exception) {
                            -1
                        }
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                putExtra("android.provider.extra.APP_UID", uid)
                            }
                        )
                    },
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.settings_permission),
                    iconRes = R.drawable.ic_shield,
                    showChevron = true,
                    tooltip = stringResource(R.string.settings_permission_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    },
                )
            }

            // 存储。破坏性操作：只给当前占用，不给箭头（点进去的是一张确认框，不是下一层页面）。
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.settings_clear_cache),
                    iconRes = R.drawable.ic_broom_clean,
                    trailingValue = uiState.cacheSizeText,
                    tooltip = stringResource(R.string.settings_clear_cache_desc),
                    onClick = { showClearCacheConfirm = true },
                )
            }

            // 关于
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.settings_about),
                    iconRes = R.drawable.ic_help,
                    trailingValue = "V" + appVersion(context),
                    showChevron = true,
                    tooltip = stringResource(R.string.settings_about_desc),
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

/** 同一套弹层里的三个单选组。分组方式只在代码里，不给用户看。 */
private enum class SettingsPicker(val titleRes: Int, val descRes: Int) {
    Language(R.string.settings_language, R.string.settings_language_desc),
    DarkMode(R.string.settings_dark_mode, R.string.settings_dark_mode_desc),
    FirstDayOfWeek(R.string.settings_first_day_of_week, R.string.settings_first_day_of_week_desc),
}

private data class SettingsOption(val value: String, val label: String)

private enum class DarkModeOption(val value: String, val labelRes: Int, val delegateMode: Int) {
    System("FOLLOW_SYSTEM", R.string.settings_dark_mode_system, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    Dark("YES", R.string.settings_dark_mode_on, AppCompatDelegate.MODE_NIGHT_YES),
    Light("NO", R.string.settings_dark_mode_off, AppCompatDelegate.MODE_NIGHT_NO),
}

/** 语言项：值与系统语言标签一一对应，`SYSTEM` 交回空列表让系统自己选。 */
internal enum class LanguageOption(val value: String, val labelRes: Int, val tags: String?) {
    System("SYSTEM", R.string.settings_language_system, null),
    Chinese("ZH", R.string.settings_language_zh, "zh-CN"),
    English("EN", R.string.settings_language_en, "en"),
}

private enum class FirstDayOption(val value: String, val labelRes: Int) {
    Monday("MONDAY", R.string.settings_monday),
    Sunday("SUNDAY", R.string.settings_sunday),
}

internal fun applyLanguage(context: Context, value: String) {
    AppPrefs.setAppLanguage(context, value)
    val option = LanguageOption.entries.first { it.value == value }
    AppCompatDelegate.setApplicationLocales(
        option.tags?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList(),
    )
}

/** 与 `AlertDialog` 默认（Material 3 `shape.extraLarge`）一致的圆角，写死以免主题换 shapes 后与【免责声明】走样。 */
private val SettingsDialogCornerRadius = 28.dp

/**
 * 单项设置的选择对话框。
 *
 * 直接复用【免责声明】用的 `AlertDialog`：圆角 28 dp、屏幕居中、标准遮罩与「点外部关闭」全部自动一致，
 * 不再出现「设置页的选项弹层是方角、贴边宽卡，免责声明是圆角居中对话框」两套观感。
 * 选项用 `AppRadioRow` 一次列全，选中即生效并关闭。
 */
@Composable
private fun SettingsOptionDialog(
    title: String,
    description: String,
    options: List<SettingsOption>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val tokens = LocalAppTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(SettingsDialogCornerRadius),
        title = {
            Text(
                text = title,
                style = tokens.type.titleS,
                color = tokens.colors.text,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = description,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(bottom = tokens.spacing.s2),
                )
                options.forEachIndexed { index, option ->
                    if (index > 0) AppListRowDivider(leadingInset = false)
                    AppRadioRow(
                        title = option.label,
                        selected = selectedValue == option.value,
                        // 对话框内容区自带 24 dp 左内边距，行内再补 12 dp 会让选项比标题多缩一截。
                        horizontalPadding = 0.dp,
                        onSelect = { onSelect(option.value) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = tokens.colors.primary)
            }
        },
    )
}

@Composable
private fun darkModeLabel(mode: String): String =
    stringResource(DarkModeOption.entries.firstOrNull { it.value == mode }?.labelRes ?: R.string.settings_dark_mode_system)

@Composable
private fun languageLabel(language: String): String =
    stringResource(LanguageOption.entries.firstOrNull { it.value == language }?.labelRes ?: R.string.settings_language_system)

@Composable
private fun firstDayLabel(value: String): String =
    stringResource(FirstDayOption.entries.firstOrNull { it.value == value }?.labelRes ?: R.string.settings_monday)
