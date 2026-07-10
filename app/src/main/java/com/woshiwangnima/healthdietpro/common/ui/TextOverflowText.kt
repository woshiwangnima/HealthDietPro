package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import kotlin.math.max

@Composable
fun TextOverflowText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = LocalContentColor.current,
    maxLines: Int = 1,
    overflowMode: String? = null,
    autoShrinkEnabled: Boolean? = null,
    autoShrinkMinSize: Int? = null,
    marqueeSpeedMs: Int? = null,
) {
    val context = LocalContext.current
    val resolvedOverflowMode = overflowMode ?: remember { AppPrefs.getTextOverflowMode(context) }
    val resolvedAutoShrinkEnabled = autoShrinkEnabled ?: remember { AppPrefs.isAutoShrinkEnabled(context) }
    val resolvedAutoShrinkMinSize = autoShrinkMinSize ?: remember { AppPrefs.getAutoShrinkMinSize(context) }
    val resolvedMarqueeSpeedMs = marqueeSpeedMs ?: remember { AppPrefs.getMarqueeSpeed(context) }

    if (!resolvedAutoShrinkEnabled) {
        if (resolvedOverflowMode == "marquee") {
            val density = LocalDensity.current
            val velocity = maxOf(10f, 60000f / resolvedMarqueeSpeedMs)
            Text(
                text = text,
                modifier = modifier.then(
                    Modifier.basicMarquee(
                        velocity = with(density) { velocity.dp },
                    )
                ),
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
            )
        } else {
            Text(
                text = text,
                modifier = modifier,
                style = style,
                color = color,
                overflow = TextOverflow.Ellipsis,
                maxLines = maxLines,
            )
        }
    } else {
        AutoShrinkText(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            minSizeSp = resolvedAutoShrinkMinSize,
            degradeMode = resolvedOverflowMode,
            marqueeSpeedMs = resolvedMarqueeSpeedMs,
        )
    }
}

@Composable
private fun AutoShrinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    minSizeSp: Int,
    degradeMode: String,
    marqueeSpeedMs: Int,
) {
    val density = LocalDensity.current
    val initialFontSizeSp = if (style.fontSize.isSpecified) style.fontSize else 16.sp
    val minSizePx = with(density) { minSizeSp.sp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        if (containerWidthPx <= 0f) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = style,
                color = color,
            )
            return@BoxWithConstraints
        }

        var currentFontSize by remember(text) { mutableFloatStateOf(initialFontSizeSp.value) }
        var measuredWidthPx by remember(text) { mutableFloatStateOf(Float.MAX_VALUE) }

        if (measuredWidthPx == Float.MAX_VALUE) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                fontSize = initialFontSizeSp,
                color = color,
                fontWeight = style.fontWeight,
                fontFamily = style.fontFamily,
                letterSpacing = style.letterSpacing,
                textDecoration = style.textDecoration,
                textAlign = style.textAlign,
                lineHeight = style.lineHeight,
                overflow = TextOverflow.Clip,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { measuredWidthPx = it.size.width.toFloat() },
            )
            return@BoxWithConstraints
        }

        if (measuredWidthPx <= containerWidthPx) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                fontSize = currentFontSize.sp,
                color = color,
                fontWeight = style.fontWeight,
                fontFamily = style.fontFamily,
                letterSpacing = style.letterSpacing,
                textDecoration = style.textDecoration,
                textAlign = style.textAlign,
                lineHeight = style.lineHeight,
                overflow = TextOverflow.Clip,
                maxLines = 1,
                softWrap = false,
            )
            return@BoxWithConstraints
        }

        val initialSizePx = with(density) { initialFontSizeSp.toPx() }
        val shrinkRatio = containerWidthPx / measuredWidthPx
        val shrunkSizePx = (initialSizePx * shrinkRatio).coerceAtLeast(minSizePx)
        val shrunkSizeSp = with(density) { shrunkSizePx.toSp() }

        if (shrunkSizeSp.value < minSizeSp + 0.01f) {
            if (degradeMode == "marquee") {
                val velocity = max(10f, 60000f / marqueeSpeedMs)
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(Modifier.basicMarquee(velocity = with(density) { velocity.dp })),
                    fontSize = minSizeSp.sp,
                    color = color,
                    fontWeight = style.fontWeight,
                    fontFamily = style.fontFamily,
                    letterSpacing = style.letterSpacing,
                    textDecoration = style.textDecoration,
                    textAlign = style.textAlign,
                    lineHeight = style.lineHeight,
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                    softWrap = false,
                )
            } else {
                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = minSizeSp.sp,
                    color = color,
                    fontWeight = style.fontWeight,
                    fontFamily = style.fontFamily,
                    letterSpacing = style.letterSpacing,
                    textDecoration = style.textDecoration,
                    textAlign = style.textAlign,
                    lineHeight = style.lineHeight,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        } else {
            currentFontSize = shrunkSizeSp.value
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                fontSize = shrunkSizeSp,
                color = color,
                fontWeight = style.fontWeight,
                fontFamily = style.fontFamily,
                letterSpacing = style.letterSpacing,
                textDecoration = style.textDecoration,
                textAlign = style.textAlign,
                lineHeight = style.lineHeight,
                overflow = TextOverflow.Clip,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
