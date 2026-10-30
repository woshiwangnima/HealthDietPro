package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.prefs.RecordFeaturePrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.ui.record.RecordActionItemUiState
import com.woshiwangnima.healthdietpro.ui.record.defaultRecordSections
import com.woshiwangnima.healthdietpro.util.UnitConverter

class UserSettingsActivity : BaseBackActivity() {
    override fun getTitleText(): String = getString(R.string.user_settings_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UnitConverter.init(this)
        // 设置页整体切到 AppTheme：顶栏、卡片、开关共用同一份 token，不再出现「页面新色、控件旧色」。
        setContent { AppTheme(darkTheme = rememberAppDarkTheme()) { UserSettingsScreen(::finish) } }
    }
}

@Composable
private fun UserSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokens = LocalAppTokens.current
    val categories = UnitConverter.getRepository()?.getCategories().orEmpty()
    var selectedCategory by remember { mutableStateOf<UnitCategory?>(null) }
    var savedUnitIds by remember(categories) {
        mutableStateOf(categories.associate { category ->
            category.id to AppPrefs.getUnit(context, category.id, category.baseUnit)
        })
    }
    var page by remember { mutableStateOf(UserSettingsPage.Home) }
    // 功能启用的开关存用户级存档，切用户各读各的；只在「已实现」的功能里列，未实现的（灰项）不给开关。
    val recordSections = remember {
        defaultRecordSections()
            .map { section -> section.copy(items = section.items.filter { it.enabled }) }
            .filter { section -> section.items.isNotEmpty() }
    }
    var disabledFeatures by remember { mutableStateOf(RecordFeaturePrefs.disabledFeatureIds(context)) }
    var drinkReminder by remember { mutableStateOf(AppPrefs.getReminderDrinkWater(context)) }
    var medicationReminder by remember { mutableStateOf(AppPrefs.getReminderMedication(context)) }
    var periodReminder by remember { mutableStateOf(AppPrefs.getReminderPeriod(context)) }
    var fastingReminder by remember { mutableStateOf(AppPrefs.getReminderFasting(context)) }
    BackHandler(enabled = page != UserSettingsPage.Home) { page = UserSettingsPage.Home }

    BaseScreen(
        title = when (page) {
            UserSettingsPage.Home -> stringResource(R.string.user_settings_title)
            UserSettingsPage.Preferences -> stringResource(R.string.user_settings_preferences)
            UserSettingsPage.Reminders -> stringResource(R.string.user_settings_reminders)
            UserSettingsPage.RecordFeatures -> stringResource(R.string.user_settings_record_features)
        },
        onBack = { if (page == UserSettingsPage.Home) onBack() else page = UserSettingsPage.Home },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(tokens.spacing.page),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        ) {
            when (page) {
                UserSettingsPage.Home -> {
                    item {
                        // 三项目录放在同一张无标题卡里：都是「进下一层」，行高与软件设置页一致。
                        AppSectionGroup {
                            AppListRow(
                                title = stringResource(R.string.user_settings_reminders),
                                iconRes = R.drawable.ic_notification,
                                showChevron = true,
                                onClick = { page = UserSettingsPage.Reminders },
                            )
                            AppListRowDivider()
                            AppListRow(
                                title = stringResource(R.string.user_settings_preferences),
                                iconRes = R.drawable.ic_preferences,
                                showChevron = true,
                                onClick = { page = UserSettingsPage.Preferences },
                            )
                            AppListRowDivider()
                            AppListRow(
                                title = stringResource(R.string.user_settings_record_features),
                                iconRes = R.drawable.ic_list,
                                tooltip = stringResource(R.string.user_settings_record_features_desc),
                                showChevron = true,
                                onClick = { page = UserSettingsPage.RecordFeatures },
                            )
                        }
                    }
                }
                UserSettingsPage.Reminders -> {
                    item {
                        AppSectionGroup {
                            ReminderToggle(stringResource(R.string.user_settings_reminder_water), drinkReminder) { drinkReminder = it; AppPrefs.setReminderDrinkWater(context, it) }
                            AppListRowDivider(leadingInset = false)
                            ReminderToggle(stringResource(R.string.user_settings_reminder_medication), medicationReminder) { medicationReminder = it; AppPrefs.setReminderMedication(context, it) }
                            AppListRowDivider(leadingInset = false)
                            ReminderToggle(stringResource(R.string.user_settings_reminder_period), periodReminder) { periodReminder = it; AppPrefs.setReminderPeriod(context, it) }
                            AppListRowDivider(leadingInset = false)
                            ReminderToggle(stringResource(R.string.user_settings_reminder_fasting), fastingReminder) { fastingReminder = it; AppPrefs.setReminderFasting(context, it) }
                        }
                    }
                }
                UserSettingsPage.RecordFeatures -> {
                    // 说明放在卡片外：它是这一页的规则，不属于任何一张卡的标题下。
                    item {
                        Text(
                            text = stringResource(R.string.record_features_hint),
                            style = tokens.type.caption,
                            color = tokens.colors.textMuted,
                        )
                    }
                    recordSections.forEach { section ->
                        item(key = "section-${section.titleRes}") {
                            // 与记录中心同一套分节（§2.4 区块卡：图标 + 标题在卡内），
                            // 行高 ≥ 56 dp、图标 20 dp、标题 body 级，跟在记录页看到的一致。
                            AppSectionCard(
                                iconRes = section.titleIconRes,
                                title = stringResource(section.titleRes),
                                modifier = Modifier.padding(bottom = tokens.spacing.s2),
                                contentPadding = PaddingValues(bottom = tokens.spacing.s2),
                            ) {
                                section.items.forEachIndexed { index, item ->
                                    if (index > 0) RecordFeatureDivider()
                                    RecordFeatureRow(
                                        item = item,
                                        checked = item.id.name !in disabledFeatures,
                                        onCheckedChange = { enabled ->
                                            val next = if (enabled) {
                                                disabledFeatures - item.id.name
                                            } else {
                                                disabledFeatures + item.id.name
                                            }
                                            disabledFeatures = next
                                            RecordFeaturePrefs.setDisabledFeatureIds(context, next)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                UserSettingsPage.Preferences -> {
                    val orderedCategories = categories.sortedByDescending { it.id == "storage" }
                    item {
                        AppSectionGroup {
                            orderedCategories.forEachIndexed { index, category ->
                                if (index > 0) AppListRowDivider()
                                UnitPreferenceRow(category, savedUnitIds.getValue(category.id)) { selectedCategory = category }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCategory?.let { category ->
        UnitSelectionDialog(category, onDismiss = { selectedCategory = null }) { unitId ->
            AppPrefs.setUnit(context, category.id, unitId)
            savedUnitIds = savedUnitIds + (category.id to unitId)
            selectedCategory = null
        }
    }
}

@Composable
private fun UnitPreferenceRow(category: UnitCategory, currentId: String, onClick: () -> Unit) {
    val units = category.units.filter { !it.hidden }
    AppListRow(
        title = category.displayName(),
        iconRes = categoryIconRes(category.id),
        tooltip = if (category.id == "storage") stringResource(R.string.user_settings_storage_unit_desc) else null,
        trailingValue = units.find { it.id == currentId }?.symbol().orEmpty(),
        showChevron = true,
        onClick = onClick,
    )
}

@Composable
private fun UnitSelectionDialog(category: UnitCategory, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    val units = category.units.filter { !it.hidden }
    val savedUnitId = AppPrefs.getUnit(context, category.id, category.baseUnit)
    var draftUnitId by remember(category.id, savedUnitId) { mutableStateOf(savedUnitId) }
    var showDiscardChanges by remember { mutableStateOf(false) }
    val hasChanges = draftUnitId != savedUnitId
    val requestDismiss = { if (hasChanges) showDiscardChanges = true else onDismiss() }
    BackHandler(enabled = true) { requestDismiss() }
    AlertDialog(
        onDismissRequest = requestDismiss,
        title = { Text(category.displayName()) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(units, key = { it.id }) { unit ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { draftUnitId = unit.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = unit.id == draftUnitId, onClick = null)
                        Text(unit.symbol(), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        dismissButton = {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.user_settings_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = requestDismiss,
            )
        },
        confirmButton = {
            Button(
                onClick = { onSelected(draftUnitId) },
                enabled = hasChanges,
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.user_settings_confirm))
            }
        },
    )
    if (showDiscardChanges) {
        AlertDialog(
            onDismissRequest = { showDiscardChanges = false },
            title = { Text(stringResource(R.string.user_settings_unsaved_title)) },
            text = { Text(stringResource(R.string.user_settings_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardChanges = false
                    onDismiss()
                }) { Text(stringResource(R.string.user_settings_unsaved_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChanges = false }) {
                    Text(stringResource(R.string.user_settings_unsaved_keep))
                }
            },
        )
    }
}

private fun categoryIconRes(categoryId: String): Int = when (categoryId) {
    "weight" -> R.drawable.ic_weight
    "length" -> R.drawable.ic_height
    "volume" -> R.drawable.ic_volume
    "density" -> R.drawable.ic_chart
    "time" -> R.drawable.ic_time
    "energy" -> R.drawable.ic_food_ingredient
    "glucose" -> R.drawable.ic_blood_glucose
    "storage" -> R.drawable.ic_settings
    else -> R.drawable.ic_preferences
}

@Composable
private fun ReminderToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.rowMinHeight)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = tokens.type.body,
            color = tokens.colors.text,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 一个可开关的功能行：图标 + 名称 + 开关，整行可点（热区 ≥ 56 dp，§2.2）。
 *
 * 关闭态是「图标转灰 + 名称转次要色」，不是整行变淡——关闭后仍要能读清是什么功能，
 * 才好在同一页里重新打开。
 */
@Composable
private fun RecordFeatureRow(
    item: RecordActionItemUiState,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.rowMinHeight)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (checked) tokens.colors.primary else tokens.colors.disabled,
        )
        Spacer(Modifier.width(tokens.spacing.s3))
        Text(
            text = stringResource(item.titleRes),
            style = tokens.type.body,
            color = if (checked) tokens.colors.text else tokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 与行内文字左缘对齐：卡片左内边距 + 图标宽 + 间距。 */
@Composable
private fun RecordFeatureDivider() {
    val tokens = LocalAppTokens.current
    HorizontalDivider(
        modifier = Modifier.padding(start = tokens.spacing.s3 + 20.dp + tokens.spacing.s3),
        thickness = tokens.radii.highlight,
        color = tokens.colors.border,
    )
}

private enum class UserSettingsPage { Home, Preferences, Reminders, RecordFeatures }
