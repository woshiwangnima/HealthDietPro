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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.util.UnitConverter

class UserSettingsActivity : BaseBackActivity() {
    override fun getTitleText(): String = getString(R.string.user_settings_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UnitConverter.init(this)
        setContent { HealthDietProTheme { UserSettingsScreen(::finish) } }
    }
}

@Composable
private fun UserSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val categories = UnitConverter.getRepository()?.getCategories().orEmpty()
    var selectedCategory by remember { mutableStateOf<UnitCategory?>(null) }
    var savedUnitIds by remember(categories) {
        mutableStateOf(categories.associate { category ->
            category.id to AppPrefs.getUnit(context, category.id, category.baseUnit)
        })
    }
    var page by remember { mutableStateOf(UserSettingsPage.Home) }
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
        },
        onBack = { if (page == UserSettingsPage.Home) onBack() else page = UserSettingsPage.Home },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (page) {
                UserSettingsPage.Home -> {
                    item { SettingRow(stringResource(R.string.user_settings_reminders), "", R.drawable.ic_notification, onClick = { page = UserSettingsPage.Reminders }) }
                    item { SettingRow(stringResource(R.string.user_settings_preferences), "", R.drawable.ic_preferences, onClick = { page = UserSettingsPage.Preferences }) }
                }
                UserSettingsPage.Reminders -> {
                    item { ReminderToggle(stringResource(R.string.user_settings_reminder_water), drinkReminder) { drinkReminder = it; AppPrefs.setReminderDrinkWater(context, it) } }
                    item { ReminderToggle(stringResource(R.string.user_settings_reminder_medication), medicationReminder) { medicationReminder = it; AppPrefs.setReminderMedication(context, it) } }
                    item { ReminderToggle(stringResource(R.string.user_settings_reminder_period), periodReminder) { periodReminder = it; AppPrefs.setReminderPeriod(context, it) } }
                    item { ReminderToggle(stringResource(R.string.user_settings_reminder_fasting), fastingReminder) { fastingReminder = it; AppPrefs.setReminderFasting(context, it) } }
                }
                UserSettingsPage.Preferences -> {
                    item { Text(stringResource(R.string.user_settings_unit_preferences)) }
                    val orderedCategories = categories.sortedByDescending { it.id == "storage" }
                    items(orderedCategories, key = { it.id }) { category ->
                        UnitPreferenceRow(category, savedUnitIds.getValue(category.id)) { selectedCategory = category }
                        HorizontalDivider()
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
    SettingRow(
        title = category.displayName(),
        subtitle = if (category.id == "storage") stringResource(R.string.user_settings_storage_unit_desc) else "",
        leadingIconRes = categoryIconRes(category.id),
        trailingValue = units.find { it.id == currentId }?.symbol().orEmpty(),
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
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_check),
                    contentDescription = null,
                )
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
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private enum class UserSettingsPage { Home, Preferences, Reminders }
