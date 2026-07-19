package com.woshiwangnima.healthdietpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppFontScaleState
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.FontScaleSlider
import com.woshiwangnima.healthdietpro.common.ui.FontTokens
import com.woshiwangnima.healthdietpro.common.ui.SettingRadioRow
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

@Composable
fun TextDisplaySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val fontScale by AppFontScaleState.scale.collectAsState()
    var overflowMode by remember { mutableStateOf(AppPrefs.getTextOverflowMode(context)) }
    var marqueeSpeed by remember { mutableStateOf(AppPrefs.getMarqueeSpeed(context)) }
    BaseScreen(title = stringResource(R.string.text_display_settings_title), onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.settings_default_font_size), style = MaterialTheme.typography.titleLarge)
                FontScaleSlider(fontScale, { AppFontScaleState.update(context, it) })
                FontPreview(stringResource(R.string.text_display_font_headline), FontTokens.headline)
                FontPreview(stringResource(R.string.text_display_font_title), FontTokens.title)
                FontPreview(stringResource(R.string.text_display_font_subtitle), FontTokens.subtitle)
                FontPreview(stringResource(R.string.text_display_font_body), FontTokens.body)
                FontPreview(stringResource(R.string.text_display_font_label), FontTokens.label)
                FontPreview(stringResource(R.string.text_display_font_caption), FontTokens.caption)
                FontPreview(stringResource(R.string.text_display_font_micro), FontTokens.micro)
            }
            item { HorizontalDivider() }
            item {
                Text(stringResource(R.string.text_display_overflow_title), style = MaterialTheme.typography.titleLarge)
                SettingRadioRow(stringResource(R.string.text_overflow_ellipsis), stringResource(R.string.text_overflow_ellipsis_desc), overflowMode == "ellipsis", onClick = { overflowMode = "ellipsis"; AppPrefs.setTextOverflowMode(context, overflowMode) })
                SettingRadioRow(stringResource(R.string.text_overflow_marquee), stringResource(R.string.text_overflow_marquee_desc), overflowMode == "marquee", onClick = { overflowMode = "marquee"; AppPrefs.setTextOverflowMode(context, overflowMode) })
                if (overflowMode == "marquee") {
                    Text("${stringResource(R.string.text_overflow_marquee_speed)}: $marqueeSpeed")
                    Slider(marqueeSpeed.toFloat(), { marqueeSpeed = it.toInt(); AppPrefs.setMarqueeSpeed(context, marqueeSpeed) }, valueRange = 50f..2000f)
                }
                TextOverflowText(stringResource(R.string.text_overflow_preview_sample), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp), overflowMode = overflowMode, marqueeSpeedMs = marqueeSpeed)
            }
        }
    }
}

@Composable
private fun FontPreview(name: String, size: androidx.compose.ui.unit.TextUnit) {
    Text("$name  AaBbCc 0123", fontSize = size)
}
