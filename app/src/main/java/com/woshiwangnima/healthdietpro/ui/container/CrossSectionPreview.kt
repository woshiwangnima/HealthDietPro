package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.model.container.CircleShape
import com.woshiwangnima.healthdietpro.model.container.CrossSectionShape
import com.woshiwangnima.healthdietpro.model.container.IrregularShape
import com.woshiwangnima.healthdietpro.model.container.RectangleShape
import com.woshiwangnima.healthdietpro.model.container.SquareShape

/** How the dimension annotation is drawn on the top-view thumbnail. */
internal enum class PreviewAnnotation { NONE, DIAMETER, RADIUS, SIDE, DIAGONAL }

/**
 * Top-view (俯视) thumbnail of a cross-section shape with dimension readout.
 *
 * - CIRCLE: DIAMETER draws a full-diameter line, RADIUS a center-to-edge line
 * - SQUARE: SIDE draws the edge, DIAGONAL draws a corner-to-corner line
 * - RECTANGLE: plain outline (no diagonal)
 * - IRREGULAR: blobby water-spill outline
 */
@Composable
internal fun CrossSectionPreview(
    shape: CrossSectionShape,
    modifier: Modifier = Modifier,
    showDimensions: Boolean = true,
    annotation: PreviewAnnotation = PreviewAnnotation.NONE,
) {
    val scheme = MaterialTheme.colorScheme
    val fill = scheme.primary.copy(alpha = 0.22f)
    val stroke = scheme.primary
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 72.dp)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize().padding(10.dp)) {
                when (shape) {
                    is CircleShape -> drawCirclePreview(shape, fill, stroke, annotation)
                    is SquareShape -> drawSquarePreview(shape, fill, stroke, annotation)
                    is RectangleShape -> drawRectanglePreview(shape, fill, stroke)
                    is IrregularShape -> drawIrregularPreview(fill, stroke)
                }
            }
        }
        if (showDimensions) {
            Text(
                text = shape.paramSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

private fun DrawScope.drawCirclePreview(shape: CircleShape, fill: Color, stroke: Color, annotation: PreviewAnnotation) {
    val radiusPx = (size.minDimension / 2f) * (shape.diameterCm / 12.0).toFloat().coerceIn(0.1f, 1f)
    drawCircle(fill, radiusPx, center)
    drawCircle(stroke, radiusPx, center, style = Stroke(1.5.dp.toPx()))
    when (annotation) {
        PreviewAnnotation.DIAMETER -> {
            drawDimLine(Offset(center.x - radiusPx, center.y), Offset(center.x + radiusPx, center.y), stroke)
        }
        PreviewAnnotation.RADIUS -> {
            drawDimLine(center, Offset(center.x + radiusPx, center.y), stroke)
        }
        else -> Unit
    }
}

private fun DrawScope.drawSquarePreview(shape: SquareShape, fill: Color, stroke: Color, annotation: PreviewAnnotation) {
    val sidePx = size.minDimension * (shape.sideLengthCm / 12.0).toFloat().coerceIn(0.1f, 1f)
    val topLeft = Offset(center.x - sidePx / 2f, center.y - sidePx / 2f)
    val rect = androidx.compose.ui.geometry.Rect(topLeft, Size(sidePx, sidePx))
    drawRect(fill, topLeft = rect.topLeft, size = rect.size)
    drawRect(stroke, topLeft = rect.topLeft, size = rect.size, style = Stroke(1.5.dp.toPx()))
    when (annotation) {
        PreviewAnnotation.SIDE -> drawDimLine(rect.topLeft, Offset(rect.right, rect.top), stroke)
        PreviewAnnotation.DIAGONAL -> drawDimLine(rect.topLeft, Offset(rect.right, rect.bottom), stroke)
        else -> Unit
    }
}

private fun DrawScope.drawRectanglePreview(shape: RectangleShape, fill: Color, stroke: Color) {
    val scale = (12.0 / maxOf(shape.lengthCm, shape.widthCm)).coerceIn(0.0, 1.0)
    val wPx = (shape.lengthCm / 12.0 * scale).toFloat() * size.width
    val hPx = (shape.widthCm / 12.0 * scale).toFloat() * size.height
    val topLeft = Offset(center.x - wPx / 2f, center.y - hPx / 2f)
    val rect = androidx.compose.ui.geometry.Rect(topLeft, Size(wPx, hPx))
    drawRect(fill, topLeft = rect.topLeft, size = rect.size)
    drawRect(stroke, topLeft = rect.topLeft, size = rect.size, style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawIrregularPreview(fill: Color, stroke: Color) {
    val w = size.width
    val h = size.height
    val pts = listOf(
        Offset(w * 0.18f, h * 0.30f),
        Offset(w * 0.42f, h * 0.14f),
        Offset(w * 0.72f, h * 0.24f),
        Offset(w * 0.86f, h * 0.48f),
        Offset(w * 0.68f, h * 0.78f),
        Offset(w * 0.38f, h * 0.86f),
        Offset(w * 0.16f, h * 0.62f),
    )
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in pts.indices) {
            val curr = pts[i]
            val next = pts[(i + 1) % pts.size]
            val midX = (curr.x + next.x) / 2f
            val midY = (curr.y + next.y) / 2f
            quadraticTo(curr.x, curr.y, midX, midY)
        }
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(1.5.dp.toPx()))
}

/** Dimension line with short perpendicular ticks at both ends. */
private fun DrawScope.drawDimLine(from: Offset, to: Offset, color: Color) {
    drawLine(color, from, to, strokeWidth = 1.5.dp.toPx())
    val tick = 4.dp.toPx()
    drawLine(color, Offset(from.x, from.y - tick), Offset(from.x, from.y + tick), strokeWidth = 1.5.dp.toPx())
    drawLine(color, Offset(to.x, to.y - tick), Offset(to.x, to.y + tick), strokeWidth = 1.5.dp.toPx())
}
