package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.util.UnitConverter

class UserSettingsActivity : BaseBackActivity() {
    override fun getTitleText(): String = "用户设置"

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
    var page by remember { mutableStateOf("home") }
    var drinkReminder by remember { mutableStateOf(AppPrefs.getReminderDrinkWater(context)) }
    var medicationReminder by remember { mutableStateOf(AppPrefs.getReminderMedication(context)) }
    var periodReminder by remember { mutableStateOf(AppPrefs.getReminderPeriod(context)) }
    var fastingReminder by remember { mutableStateOf(AppPrefs.getReminderFasting(context)) }
    BackHandler(enabled = page != "home") { page = "home" }
    BaseScreen(title = when (page) { "preferences" -> "偏好设置"; "reminders" -> "提醒设置"; else -> "用户设置" }, onBack = { if (page == "home") onBack() else page = "home" }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (page == "home") {
                item { SettingRow("提醒设置", "", R.drawable.ic_notification, onClick = { page = "reminders" }) }
                item { SettingRow("偏好设置", "", R.drawable.ic_preferences, onClick = { page = "preferences" }) }
            } else if (page == "reminders") {
                item { ReminderToggle("饮水提醒", drinkReminder) { drinkReminder = it; AppPrefs.setReminderDrinkWater(context, it) } }
                item { ReminderToggle("用药提醒", medicationReminder) { medicationReminder = it; AppPrefs.setReminderMedication(context, it) } }
                item { ReminderToggle("经期提醒", periodReminder) { periodReminder = it; AppPrefs.setReminderPeriod(context, it) } }
                item { ReminderToggle("空腹提醒", fastingReminder) { fastingReminder = it; AppPrefs.setReminderFasting(context, it) } }
            } else {
                item { Text("默认单位偏好") }
                items(categories, key = { it.id }) { category ->
                val units = category.units.filter { !it.hidden }
                val currentId = AppPrefs.getUnit(context, category.id, category.baseUnit)
                SettingRow(
                    title = category.displayName(),
                    subtitle = "",
                    leadingIconRes = R.drawable.ic_preferences,
                    trailingValue = units.find { it.id == currentId }?.symbol().orEmpty(),
                    onClick = { selectedCategory = category },
                )
                androidx.compose.material3.HorizontalDivider()
                }
            }
        }
    }
    selectedCategory?.let { category ->
        val units = category.units.filter { !it.hidden }
        AppDropdownField(
            label = category.displayName(),
            value = units.find { it.id == AppPrefs.getUnit(context, category.id, category.baseUnit) }?.symbol().orEmpty(),
            options = units.map { AppDropdownOption(it.id, it.symbol()) },
            onSelect = { AppPrefs.setUnit(context, category.id, it.id); selectedCategory = null },
        )
    }
}

@Composable
private fun ReminderToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
