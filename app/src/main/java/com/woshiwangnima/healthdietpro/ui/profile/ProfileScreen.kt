package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.SettingRow

@Composable
internal fun ProfileScreen(
    state: ProfileUserInfoUiState,
    onOpenAppSettings: () -> Unit,
    onOpenBmi: () -> Unit,
    onOpenUserSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenUserSwitch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpenAppSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_app_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ProfileUserInfoCard(
            state = state,
            onEditProfile = onEditProfile,
            onSwitchUser = onOpenUserSwitch,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column {
                SettingRow(
                    title = stringResource(R.string.bmi_title),
                    subtitle = stringResource(R.string.bmi_entry_desc),
                    leadingIconRes = R.drawable.ic_chart,
                    onClick = onOpenBmi,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.profile_user_settings),
                    subtitle = stringResource(R.string.profile_user_settings_desc),
                    leadingIconRes = R.drawable.ic_preferences,
                    onClick = onOpenUserSettings,
                )
            }
        }
    }
}
