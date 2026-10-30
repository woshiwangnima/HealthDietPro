package com.woshiwangnima.healthdietpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppFontScaleState
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.FontScaleSlider
import com.woshiwangnima.healthdietpro.common.ui.FontTokens
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadioRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

/**
 * 文字显示：字号 + 溢出处理两张卡。
 *
 * 旧版是一列没有分组的标题与控件，字号预览和溢出选项之间只有一条默认分隔线；
 * 现在每组各一张 `AppSectionCard`，组内选项用 `AppRadioRow`，与「用户设置」的单位偏好同一套行。
 */
@Composable
fun TextDisplaySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokens = LocalAppTokens.current
    val fontScale by AppFontScaleState.scale.collectAsState()
    var overflowMode by remember { mutableStateOf(AppPrefs.getTextOverflowMode(context)) }
    var marqueeSpeed by remember { mutableStateOf(AppPrefs.getMarqueeSpeed(context)) }
    var marqueeIterations by remember { mutableStateOf(AppPrefs.getMarqueeIterations(context).toString()) }

    BaseScreen(title = stringResource(R.string.text_display_settings_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s3),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
        ) {
            AppSectionCard(
                iconRes = R.drawable.ic_font_size,
                title = stringResource(R.string.settings_default_font_size),
                contentPadding = PaddingValues(bottom = tokens.spacing.s2),
            ) {
                FontScaleSlider(fontScale, { AppFontScaleState.update(context, it) })
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = tokens.spacing.s3),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
                ) {
                    FontPreview(stringResource(R.string.text_display_font_headline), FontTokens.headline)
                    FontPreview(stringResource(R.string.text_display_font_title), FontTokens.title)
                    FontPreview(stringResource(R.string.text_display_font_subtitle), FontTokens.subtitle)
                    FontPreview(stringResource(R.string.text_display_font_body), FontTokens.body)
                    FontPreview(stringResource(R.string.text_display_font_label), FontTokens.label)
                    FontPreview(stringResource(R.string.text_display_font_caption), FontTokens.caption)
                    FontPreview(stringResource(R.string.text_display_font_micro), FontTokens.micro)
                }
            }

            AppSectionCard(
                iconRes = R.drawable.ic_text_overflow,
                title = stringResource(R.string.text_display_overflow_title),
                contentPadding = PaddingValues(bottom = tokens.spacing.s2),
            ) {
                AppRadioRow(
                    title = stringResource(R.string.text_overflow_ellipsis),
                    subtitle = stringResource(R.string.text_overflow_ellipsis_desc),
                    selected = overflowMode == "ellipsis",
                    onSelect = {
                        overflowMode = "ellipsis"
                        AppPrefs.setTextOverflowMode(context, overflowMode)
                    },
                )
                AppListRowDivider(leadingInset = false)
                AppRadioRow(
                    title = stringResource(R.string.text_overflow_marquee),
                    subtitle = stringResource(R.string.text_overflow_marquee_desc),
                    selected = overflowMode == "marquee",
                    onSelect = {
                        overflowMode = "marquee"
                        AppPrefs.setTextOverflowMode(context, overflowMode)
                    },
                )
                if (overflowMode == "marquee") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                    ) {
                        Text(
                            text = "${stringResource(R.string.text_overflow_marquee_speed)}: $marqueeSpeed",
                            style = tokens.type.caption,
                            color = tokens.colors.textMuted,
                        )
                        Slider(
                            value = marqueeSpeed.toFloat(),
                            onValueChange = {
                                marqueeSpeed = it.toInt()
                                AppPrefs.setMarqueeSpeed(context, marqueeSpeed)
                            },
                            valueRange = 50f..2000f,
                        )
                        OutlinedTextField(
                            value = marqueeIterations,
                            onValueChange = { input ->
                                if (input.all(Char::isDigit)) {
                                    marqueeIterations = input
                                    AppPrefs.setMarqueeIterations(context, input.toIntOrNull() ?: 0)
                                }
                            },
                            label = { Text(stringResource(R.string.text_overflow_marquee_iterations)) },
                            supportingText = { Text(stringResource(R.string.text_overflow_marquee_iterations_minimum_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                TextOverflowText(
                    text = stringResource(R.string.text_overflow_preview_sample),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2)
                        .background(tokens.colors.panel)
                        .padding(tokens.spacing.s3),
                    overflowMode = overflowMode,
                    marqueeSpeedMs = marqueeSpeed,
                    marqueeIterations = marqueeIterations.toIntOrNull() ?: 0,
                )
            }
        }
    }
}

@Composable
private fun FontPreview(name: String, size: TextUnit) {
    Text("$name  AaBbCc 0123", fontSize = size)
}
