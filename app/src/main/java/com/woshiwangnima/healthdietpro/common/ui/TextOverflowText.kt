package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

@Composable
fun TextOverflowText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge, color: Color = LocalContentColor.current, maxLines: Int = 1, textAlign: TextAlign? = null, overflowMode: String? = null, marqueeSpeedMs: Int? = null, marqueeIterations: Int? = null) {
    val context = LocalContext.current
    val mode = overflowMode ?: remember { AppPrefs.getTextOverflowMode(context) }
    val speed = marqueeSpeedMs ?: remember { AppPrefs.getMarqueeSpeed(context) }
    val iterations = marqueeIterations ?: remember { AppPrefs.getMarqueeIterations(context) }
    if (mode == "marquee") {
        val density = LocalDensity.current
        Text(text, modifier.basicMarquee(iterations = if (iterations == 0) Int.MAX_VALUE else iterations, velocity = with(density) { (60000f / speed).coerceAtLeast(10f).dp }), style = style, color = color, maxLines = 1, textAlign = textAlign, overflow = TextOverflow.Clip, softWrap = false)
    } else Text(text = text, modifier = modifier, style = style, color = color, maxLines = maxLines, textAlign = textAlign, overflow = TextOverflow.Ellipsis)
}

@Composable
fun TextOverflowText(text: AnnotatedString, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge, maxLines: Int = 1, textAlign: TextAlign? = null, overflowMode: String? = null, marqueeSpeedMs: Int? = null, marqueeIterations: Int? = null) {
    val context = LocalContext.current
    val mode = overflowMode ?: remember { AppPrefs.getTextOverflowMode(context) }
    val speed = marqueeSpeedMs ?: remember { AppPrefs.getMarqueeSpeed(context) }
    val iterations = marqueeIterations ?: remember { AppPrefs.getMarqueeIterations(context) }
    if (mode == "marquee") {
        val density = LocalDensity.current
        Text(text, modifier.basicMarquee(iterations = if (iterations == 0) Int.MAX_VALUE else iterations, velocity = with(density) { (60000f / speed).coerceAtLeast(10f).dp }), style = style, maxLines = 1, textAlign = textAlign, overflow = TextOverflow.Clip, softWrap = false)
    } else Text(text = text, modifier = modifier, style = style, maxLines = maxLines, textAlign = textAlign, overflow = TextOverflow.Ellipsis)
}
