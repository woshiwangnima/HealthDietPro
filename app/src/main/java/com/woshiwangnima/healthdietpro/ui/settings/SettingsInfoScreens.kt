package com.woshiwangnima.healthdietpro.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
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
internal fun DisclaimerScreen(onBack: () -> Unit) {
    BaseScreen(title = stringResource(R.string.settings_disclaimer), onBack = onBack) { padding ->
        Text(
            text = stringResource(R.string.settings_disclaimer_content),
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    BaseScreen(title = stringResource(R.string.settings_about), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
            item { Text(stringResource(R.string.settings_about_version, appVersion(LocalContext.current)), style = MaterialTheme.typography.bodyLarge) }
            item { Text(stringResource(R.string.settings_about_content), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
