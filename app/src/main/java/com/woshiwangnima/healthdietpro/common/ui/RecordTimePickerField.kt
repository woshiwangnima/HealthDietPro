package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import kotlin.time.Duration

/** Standard clickable entry for choosing a date, minute-precision time, or second-precision time. */
@Composable
internal fun RecordTimePickerField(
    title: String,
    valueMillis: Long?,
    precision: RecordTimePrecision,
    onClick: () -> Unit,
    emptyText: String? = null,
    resetOffset: Duration? = null,
    resetLabel: String? = null,
    onResetClick: (() -> Unit)? = null,
    @DrawableRes assistIconRes: Int? = null,
    assistLabel: String? = null,
    onAssistClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_birthday),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = valueMillis?.let { formatRecordTimestamp(it, precision) } ?: emptyText.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (resetOffset != null && onResetClick != null) {
                RecordTimeFieldAction(label = resetLabel, onClick = onResetClick) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = resetLabel,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            // 没标签就不画：只剩一个图标的按钮说不清它要干什么。
            if (assistIconRes != null && assistLabel != null && onAssistClick != null) {
                RecordTimeFieldAction(label = assistLabel, onClick = onAssistClick) {
                    Icon(
                        painter = painterResource(assistIconRes),
                        contentDescription = assistLabel,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.width(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 时间字段右侧的小按钮（primaryContainer 底 + 短标签）。
 *
 * 它和整块字段的 `onClick`（打开选择器）是两个独立动作：按钮自己是一个 `Surface` 点击目标，
 * 命中优先级高于外层 `Surface`，点按钮不会顺手把时间选择器也弹出来。
 */
@Composable
private fun RecordTimeFieldAction(
    label: String?,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            label?.let {
                TextOverflowText(
                    text = it,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
        }
    }
}
