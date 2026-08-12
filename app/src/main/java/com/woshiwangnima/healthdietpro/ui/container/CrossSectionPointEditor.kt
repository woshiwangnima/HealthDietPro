package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.NumericInputField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputKind
import com.woshiwangnima.healthdietpro.common.ui.NumericInputSpec
import com.woshiwangnima.healthdietpro.model.container.CircleShape
import com.woshiwangnima.healthdietpro.model.container.CrossSection
import com.woshiwangnima.healthdietpro.model.container.CrossSectionShape
import com.woshiwangnima.healthdietpro.model.container.IrregularShape
import com.woshiwangnima.healthdietpro.model.container.RectangleShape
import com.woshiwangnima.healthdietpro.model.container.ShapeKind
import com.woshiwangnima.healthdietpro.model.container.SquareShape
import kotlin.math.sqrt

/**
 * Editable card for a single cross-section point.
 *
 * 长度类输入（截面高度、半径/直径、边长/对角线、长/宽、周长）统一为「数值在左、单位在右」的同行布局；
 * 单位下拉与容器总高共用同一单位（[lengthUnitId]），默认即容器高度所定义的单位。修改任一处单位都会经
 * [onUnitChange] 换算整个轮廓，保证数值始终以基准 cm 存储。
 */
@Composable
internal fun CrossSectionPointEditor(
    point: CrossSection,
    lengthUnitId: String,
    totalHeightCm: Double,
    canDelete: Boolean,
    onUnitChange: (String) -> Unit,
    onPointChanged: (CrossSection) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val unit = lengthUnitSymbol(lengthUnitId)
    val totalHeightDisplay = fromBaseCm(totalHeightCm, lengthUnitId)

    key(lengthUnitId) {
        var kind by remember(point.heightCm) { mutableStateOf(point.shape.kind) }
        var radiusMode by remember(point.heightCm, kind) { mutableStateOf(false) }
        var diagonalMode by remember(point.heightCm, kind) { mutableStateOf(false) }
        var heightText by remember(point.heightCm) { mutableStateOf("%.1f".format(fromBaseCm(point.heightCm, lengthUnitId))) }
        var diameterText by remember(point.heightCm, kind) { mutableStateOf(seedLengthText(point.shape, kind, "diameter", lengthUnitId)) }
        var sideText by remember(point.heightCm, kind) { mutableStateOf(seedLengthText(point.shape, kind, "side", lengthUnitId)) }
        var lengthText by remember(point.heightCm, kind) { mutableStateOf(seedLengthText(point.shape, kind, "length", lengthUnitId)) }
        var widthText by remember(point.heightCm, kind) { mutableStateOf(seedLengthText(point.shape, kind, "width", lengthUnitId)) }
        var areaText by remember(point.heightCm, kind) { mutableStateOf(seedAreaText(point.shape)) }
        var perimeterText by remember(point.heightCm, kind) { mutableStateOf(seedPerimeterText(point.shape, lengthUnitId)) }

        fun commitShape(shape: CrossSectionShape) {
            onPointChanged(point.copy(shape = shape))
        }

        val positiveSpec = NumericInputSpec(
            kind = NumericInputKind.Decimal,
            range = RangeBand(min = 0.0, value = Unit),
            decimalPlaces = 1,
        )

        Card(modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PointLengthInputRow(
                    label = "截面高度",
                    value = heightText,
                    onValueChange = { newValue ->
                        heightText = newValue
                        newValue.toDoubleOrNull()?.let { display ->
                            val base = toBaseCm(display, lengthUnitId)
                            if (base in 0.0..totalHeightCm) {
                                onPointChanged(point.copy(heightCm = base))
                            }
                        }
                    },
                    spec = NumericInputSpec(
                        kind = NumericInputKind.Decimal,
                        range = RangeBand(min = 0.0, max = totalHeightDisplay, maxInclusive = true, value = Unit),
                        decimalPlaces = 1,
                    ),
                    unitId = lengthUnitId,
                    onUnitChange = onUnitChange,
                )
                Text(
                    text = "转折点：输入容器在某一高度处的截面；顶部（容器口）未填写时按同截面平行延伸。",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )

                CrossSectionShapeSelector(kind, { newKind ->
                    if (newKind != kind) {
                        kind = newKind
                        diameterText = ""
                        sideText = ""
                        lengthText = ""
                        widthText = ""
                        areaText = ""
                        perimeterText = ""
                        radiusMode = false
                        diagonalMode = false
                    }
                })

                when (kind) {
                    ShapeKind.CIRCLE -> {
                        PointLengthInputRow(
                            label = if (radiusMode) "半径" else "直径",
                            value = diameterText,
                            onValueChange = {
                                diameterText = it
                                it.toDoubleOrNull()?.let { value ->
                                    val diameter = if (radiusMode) value * 2.0 else value
                                    commitShape(CircleShape(toBaseCm(diameter, lengthUnitId)))
                                }
                            },
                            spec = positiveSpec,
                            unitId = lengthUnitId,
                            onUnitChange = onUnitChange,
                        )
                        Row {
                            TextButton(onClick = {
                                val current = diameterText.toDoubleOrNull() ?: return@TextButton
                                diameterText = if (radiusMode) "%.1f".format(current * 2.0) else "%.1f".format(current / 2.0)
                                radiusMode = !radiusMode
                            }) {
                                Text(if (radiusMode) "切换为直径" else "切换为半径")
                            }
                        }
                    }
                    ShapeKind.SQUARE -> {
                        PointLengthInputRow(
                            label = if (diagonalMode) "对角线" else "边长",
                            value = sideText,
                            onValueChange = {
                                sideText = it
                                it.toDoubleOrNull()?.let { value ->
                                    val side = if (diagonalMode) value / sqrt(2.0) else value
                                    commitShape(SquareShape(toBaseCm(side, lengthUnitId)))
                                }
                            },
                            spec = positiveSpec,
                            unitId = lengthUnitId,
                            onUnitChange = onUnitChange,
                        )
                        Row {
                            TextButton(onClick = {
                                val current = sideText.toDoubleOrNull() ?: return@TextButton
                                sideText = if (diagonalMode) "%.1f".format(current * sqrt(2.0)) else "%.1f".format(current / sqrt(2.0))
                                diagonalMode = !diagonalMode
                            }) {
                                Text(if (diagonalMode) "切换为边长" else "切换为对角线")
                            }
                        }
                    }
                    ShapeKind.RECTANGLE -> {
                        PointLengthInputRow(
                            label = "长",
                            value = lengthText,
                            onValueChange = {
                                lengthText = it
                                val l = it.toDoubleOrNull()
                                val w = widthText.toDoubleOrNull()
                                if (l != null && w != null) {
                                    commitShape(RectangleShape(toBaseCm(l, lengthUnitId), toBaseCm(w, lengthUnitId)))
                                }
                            },
                            spec = positiveSpec,
                            unitId = lengthUnitId,
                            onUnitChange = onUnitChange,
                        )
                        PointLengthInputRow(
                            label = "宽",
                            value = widthText,
                            onValueChange = {
                                widthText = it
                                val l = lengthText.toDoubleOrNull()
                                val w = it.toDoubleOrNull()
                                if (l != null && w != null) {
                                    commitShape(RectangleShape(toBaseCm(l, lengthUnitId), toBaseCm(w, lengthUnitId)))
                                }
                            },
                            spec = positiveSpec,
                            unitId = lengthUnitId,
                            onUnitChange = onUnitChange,
                        )
                    }
                    ShapeKind.IRREGULAR -> {
                        NumericInputField(
                            label = "横截面积 (cm²)",
                            value = areaText,
                            onValueChange = {
                                areaText = it
                                it.toDoubleOrNull()?.let { area -> commitShape(IrregularShape(area, perimeterText.toDoubleOrNull())) }
                            },
                            spec = NumericInputSpec(
                                kind = NumericInputKind.Decimal,
                                range = RangeBand(min = 0.0, minInclusive = false, value = Unit),
                                decimalPlaces = 1,
                            ),
                        )
                        PointLengthInputRow(
                            label = "周长（选填）",
                            value = perimeterText,
                            onValueChange = {
                                perimeterText = it
                                areaText.toDoubleOrNull()?.let { area ->
                                    commitShape(IrregularShape(area, it.toDoubleOrNull()?.let { p -> toBaseCm(p, lengthUnitId) }))
                                }
                            },
                            spec = NumericInputSpec(kind = NumericInputKind.Decimal, decimalPlaces = 1),
                            unitId = lengthUnitId,
                            onUnitChange = onUnitChange,
                        )
                        Text(
                            text = "请将容器水平切开，测量并填入切面实际面积。",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CrossSectionPreview(
                            shape = point.shape,
                            showDimensions = false,
                            annotation = when (kind) {
                                ShapeKind.CIRCLE -> if (radiusMode) PreviewAnnotation.RADIUS else PreviewAnnotation.DIAMETER
                                ShapeKind.SQUARE -> if (diagonalMode) PreviewAnnotation.DIAGONAL else PreviewAnnotation.SIDE
                                ShapeKind.RECTANGLE, ShapeKind.IRREGULAR -> PreviewAnnotation.NONE
                            },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("面积: %.2f cm²".format(point.shape.area), style = MaterialTheme.typography.bodySmall)
                            Text(
                                "周长: ${point.shape.perimeter?.let { "%.2f $unit".format(fromBaseCm(it, lengthUnitId)) } ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (canDelete) {
                        TextButton(onClick = onDelete) {
                            Text("删除", color = scheme.error)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One length input as a single row: numeric value on the left, unit selector on the right.
 * The unit mirrors the profile's shared [unitId] (defaulting to the container-height unit).
 */
@Composable
private fun PointLengthInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    spec: NumericInputSpec,
    unitId: String,
    onUnitChange: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumericInputField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            spec = spec,
            modifier = Modifier.weight(1f),
        )
        AppDropdownField(
            label = "单位",
            value = lengthUnitSymbol(unitId),
            options = PRACTICAL_LENGTH_UNITS.map { id -> AppDropdownOption(id, lengthUnitSymbol(id)) },
            onSelect = { onUnitChange(it.id) },
            modifier = Modifier.width(100.dp),
        )
    }
}

private fun seedLengthText(shape: CrossSectionShape, kind: ShapeKind, field: String, unitId: String): String {
    fun fmt(baseCm: Double?): String = baseCm?.let { "%.1f".format(fromBaseCm(it, unitId)) }.orEmpty()
    return when (kind) {
        ShapeKind.CIRCLE -> if (field == "diameter") fmt((shape as? CircleShape)?.diameterCm) else ""
        ShapeKind.SQUARE -> if (field == "side") fmt((shape as? SquareShape)?.sideLengthCm) else ""
        ShapeKind.RECTANGLE -> when (field) {
            "length" -> fmt((shape as? RectangleShape)?.lengthCm)
            "width" -> fmt((shape as? RectangleShape)?.widthCm)
            else -> ""
        }
        ShapeKind.IRREGULAR -> ""
    }
}

private fun seedAreaText(shape: CrossSectionShape): String =
    (shape as? IrregularShape)?.area?.let { "%.1f".format(it) }.orEmpty()

private fun seedPerimeterText(shape: CrossSectionShape, unitId: String): String =
    (shape as? IrregularShape)?.perimeter?.let { "%.1f".format(fromBaseCm(it, unitId)) }.orEmpty()
