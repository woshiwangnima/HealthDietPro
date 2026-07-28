package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.common.ui.SettingRadioRow
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.archive.appVersion

@Composable
internal fun LanguageSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(AppPrefs.getAppLanguage(context)) }
    BaseScreen(title = stringResource(R.string.settings_language), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                LanguageOption(
                    label = stringResource(R.string.settings_language_system),
                    selected = language == "SYSTEM",
                ) {
                    language = "SYSTEM"
                    AppPrefs.setAppLanguage(context, language)
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            }
            item {
                LanguageOption(
                    label = stringResource(R.string.settings_language_zh),
                    selected = language == "ZH",
                ) {
                    language = "ZH"
                    AppPrefs.setAppLanguage(context, language)
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
                }
            }
            item {
                LanguageOption(
                    label = stringResource(R.string.settings_language_en),
                    selected = language == "EN",
                ) {
                    language = "EN"
                    AppPrefs.setAppLanguage(context, language)
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    SettingRadioRow(label, "", selected, onClick)
}

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showDisclaimer by remember { mutableStateOf(false) }
    var showReferences by remember { mutableStateOf(false) }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.settings_disclaimer)) },
            text = { Text(stringResource(R.string.settings_disclaimer_content)) },
            confirmButton = {
                Button(onClick = { showDisclaimer = false }) {
                    Text(stringResource(R.string.settings_confirm))
                }
            },
        )
    }

    if (showReferences) {
        BackHandler { showReferences = false }
        ReferencesScreen(
            onBack = { showReferences = false },
            onOpenUrl = { url -> openUrl(context, url) },
        )
        return
    }

    BaseScreen(title = stringResource(R.string.settings_about), onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.settings_about_app_icon),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp)
                    .height(84.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_about_name),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(
                text = stringResource(R.string.settings_about_version, appVersion(context)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Button(
                onClick = { openUrl(context, context.getString(R.string.settings_update_url)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.settings_check_update))
            }
            SettingRow(
                title = stringResource(R.string.settings_source_code),
                subtitle = stringResource(R.string.settings_source_url),
                leadingIconRes = R.drawable.ic_description,
                onClick = { openUrl(context, context.getString(R.string.settings_source_url)) },
            )
            SettingRow(
                title = stringResource(R.string.settings_disclaimer),
                subtitle = stringResource(R.string.settings_disclaimer_desc),
                leadingIconRes = R.drawable.ic_description,
                onClick = { showDisclaimer = true },
            )
            SettingRow(
                title = stringResource(R.string.settings_sources_and_references),
                subtitle = stringResource(R.string.settings_sources_and_references_desc),
                leadingIconRes = R.drawable.ic_info,
                onClick = { showReferences = true },
            )
        }
    }
}

@Composable
private fun ReferencesScreen(onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    val drisUrl = stringResource(R.string.settings_dris_url)
    val icd11Url = stringResource(R.string.settings_icd11_url)
    BaseScreen(title = stringResource(R.string.settings_sources_and_references), onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            SettingRow(
                title = stringResource(R.string.settings_dris),
                subtitle = drisUrl,
                leadingIconRes = R.drawable.ic_description,
                onClick = { onOpenUrl(drisUrl) },
            )
            SettingRow(
                title = stringResource(R.string.settings_icd11),
                subtitle = icd11Url,
                leadingIconRes = R.drawable.ic_info,
                onClick = { onOpenUrl(icd11Url) },
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
