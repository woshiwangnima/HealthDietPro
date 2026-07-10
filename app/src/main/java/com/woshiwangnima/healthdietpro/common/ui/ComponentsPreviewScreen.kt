package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

@Composable
fun ComponentsPreviewScreen(
    onBack: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fontScale by AppFontScaleState.scale.collectAsState()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val alphaMid = rememberFontStyleAlphaMid()

    if (showConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.compose_confirm_dialog_title),
            message = stringResource(R.string.compose_confirm_dialog_message),
            confirmText = stringResource(R.string.compose_confirm_dialog_ok),
            cancelText = stringResource(R.string.compose_confirm_dialog_cancel),
            onConfirm = { showConfirm = false },
            onDismiss = { showConfirm = false },
        )
    }

    BaseScreen(
        title = stringResource(R.string.compose_components_preview_title),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.compose_font_scale_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FontScaleSlider(
                currentScale = fontScale,
                onScaleChangeStopped = { AppFontScaleState.update(context, it) },
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.compose_confirm_dialog_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(onClick = { showConfirm = true }) {
                Text(stringResource(R.string.compose_show_confirm_dialog))
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.compose_text_overflow_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "短文本示例",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "短文本示例2",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "示例 Aa",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(alphaMid),
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
                )
            }
            Text(
                text = stringResource(R.string.text_overflow_marquee_speed_desc),
                style = TextStyle(fontSize = FontTokens.caption),
                color = onSurfaceVariant,
            )
        }
    }
}
