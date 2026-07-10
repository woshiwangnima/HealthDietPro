package com.woshiwangnima.healthdietpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.FontTokens
import com.woshiwangnima.healthdietpro.common.ui.SettingRadioRow
import com.woshiwangnima.healthdietpro.common.ui.SettingSwitchRow
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.rememberFontStyleAlphaMid

@Composable
fun TextOverflowScreen(
    onBack: () -> Unit,
    viewModel: TextOverflowViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val alphaMid = rememberFontStyleAlphaMid()

    BaseScreen(title = stringResource(R.string.text_overflow_section_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                SettingSwitchRow(
                    title = stringResource(R.string.text_overflow_auto_shrink),
                    subtitle = stringResource(R.string.text_overflow_auto_shrink_desc),
                    checked = uiState.autoShrinkEnabled,
                    onCheckedChange = viewModel::setAutoShrinkEnabled,
                )
            }

            if (uiState.autoShrinkEnabled) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.text_overflow_auto_shrink_min) + ": ${uiState.autoShrinkMinSize}sp",
                            style = TextStyle(fontSize = FontTokens.caption),
                            color = onSurfaceVariant,
                        )
                        Slider(
                            value = uiState.autoShrinkMinSize.toFloat(),
                            onValueChange = { viewModel.setAutoShrinkMinSize(it.toInt()) },
                            valueRange = 4f..16f,
                            steps = 11,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.text_overflow_auto_shrink_min_desc),
                            style = TextStyle(fontSize = FontTokens.caption),
                            color = onSurfaceVariant,
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Text(
                    text = stringResource(R.string.text_overflow_degrade_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            item {
                SettingRadioRow(
                    title = stringResource(R.string.text_overflow_ellipsis),
                    subtitle = stringResource(R.string.text_overflow_ellipsis_desc),
                    selected = uiState.overflowMode == "ellipsis",
                    onClick = { viewModel.setOverflowMode("ellipsis") },
                )
            }

            item { HorizontalDivider() }

            item {
                SettingRadioRow(
                    title = stringResource(R.string.text_overflow_marquee),
                    subtitle = stringResource(R.string.text_overflow_marquee_desc),
                    selected = uiState.overflowMode == "marquee",
                    onClick = { viewModel.setOverflowMode("marquee") },
                )
            }

            if (uiState.overflowMode == "marquee") {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.text_overflow_marquee_speed) + ": ${uiState.marqueeSpeed}ms",
                            style = TextStyle(fontSize = FontTokens.caption),
                            color = onSurfaceVariant,
                        )
                        Slider(
                            value = uiState.marqueeSpeed.toFloat(),
                            onValueChange = { viewModel.setMarqueeSpeed(it.toInt()) },
                            valueRange = 50f..2000f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.text_overflow_marquee_speed_desc),
                            style = TextStyle(fontSize = FontTokens.caption),
                            color = onSurfaceVariant,
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.compose_text_overflow_section),
                        style = TextStyle(fontSize = FontTokens.caption),
                        color = onSurfaceVariant,
                        modifier = Modifier.alpha(alphaMid).padding(bottom = 8.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        TextOverflowText(
                            text = stringResource(R.string.text_overflow_preview_sample),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            overflowMode = uiState.overflowMode,
                            autoShrinkEnabled = uiState.autoShrinkEnabled,
                            autoShrinkMinSize = uiState.autoShrinkMinSize,
                            marqueeSpeedMs = uiState.marqueeSpeed,
                        )
                    }
                }
            }
        }
    }
}
