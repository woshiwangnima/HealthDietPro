package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

class ReminderSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                ReminderSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun ReminderSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var drinkEnabled by remember { mutableStateOf(AppPrefs.getReminderDrinkWater(context)) }
    var medicationEnabled by remember { mutableStateOf(AppPrefs.getReminderMedication(context)) }
    var periodEnabled by remember { mutableStateOf(AppPrefs.getReminderPeriod(context)) }
    var fastingEnabled by remember { mutableStateOf(AppPrefs.getReminderFasting(context)) }

    BaseScreen(title = stringResource(R.string.reminder_settings_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ReminderRow(
                title = stringResource(R.string.reminder_settings_drink_water),
                checked = drinkEnabled,
                onCheckedChange = {
                    drinkEnabled = it
                    AppPrefs.setReminderDrinkWater(context, it)
                },
            )
            HorizontalDivider()
            ReminderRow(
                title = stringResource(R.string.reminder_settings_medication),
                checked = medicationEnabled,
                onCheckedChange = {
                    medicationEnabled = it
                    AppPrefs.setReminderMedication(context, it)
                },
            )
            HorizontalDivider()
            ReminderRow(
                title = stringResource(R.string.reminder_settings_period),
                checked = periodEnabled,
                onCheckedChange = {
                    periodEnabled = it
                    AppPrefs.setReminderPeriod(context, it)
                },
            )
            HorizontalDivider()
            ReminderRow(
                title = stringResource(R.string.reminder_settings_fasting),
                checked = fastingEnabled,
                onCheckedChange = {
                    fastingEnabled = it
                    AppPrefs.setReminderFasting(context, it)
                },
            )
        }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(if (checked) 1f else 0.3f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
