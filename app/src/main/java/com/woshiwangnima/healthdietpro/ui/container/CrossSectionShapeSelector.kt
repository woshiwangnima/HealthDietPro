package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woshiwangnima.healthdietpro.model.container.ShapeKind

private val ShapeKind.chineseLabel: String
    get() = when (this) {
        ShapeKind.CIRCLE -> "○ 圆形"
        ShapeKind.SQUARE -> "□ 正方形"
        ShapeKind.RECTANGLE -> "▭ 长方形"
        ShapeKind.IRREGULAR -> "✧ 异形"
    }

/** Mutually exclusive cross-section shape switcher. */
@Composable
internal fun CrossSectionShapeSelector(
    selected: ShapeKind,
    onSelected: (ShapeKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        ShapeKind.entries.forEachIndexed { index, kind ->
            SegmentedButton(
                selected = selected == kind,
                onClick = { onSelected(kind) },
                shape = SegmentedButtonDefaults.itemShape(index, ShapeKind.entries.size),
                label = { Text(kind.chineseLabel, style = MaterialTheme.typography.bodySmall) },
            )
        }
    }
}
