package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

object FontTokens {
    val headline = 36.sp
    val title = 28.sp
    val subtitle = 22.sp
    val body = 16.sp
    val label = 14.sp
    val caption = 12.sp
    val micro = 11.sp
}

@Composable
fun rememberFontStyleAlphaMid(): Float {
    val context = LocalContext.current
    return remember { AppPrefs.getFontStyleTokenAlphaMid(context) }
}

@Composable
fun appTypography(): Typography {
    return Typography(
        displayLarge = TextStyle(fontSize = FontTokens.headline),
        displayMedium = TextStyle(fontSize = FontTokens.headline),
        displaySmall = TextStyle(fontSize = FontTokens.title),
        headlineLarge = TextStyle(fontSize = FontTokens.headline),
        headlineMedium = TextStyle(fontSize = FontTokens.headline),
        headlineSmall = TextStyle(fontSize = FontTokens.title),
        titleLarge = TextStyle(fontSize = FontTokens.title),
        titleMedium = TextStyle(fontSize = FontTokens.title),
        titleSmall = TextStyle(fontSize = FontTokens.subtitle),
        bodyLarge = TextStyle(fontSize = FontTokens.body),
        bodyMedium = TextStyle(fontSize = FontTokens.body),
        bodySmall = TextStyle(fontSize = FontTokens.caption),
        labelLarge = TextStyle(fontSize = FontTokens.label),
        labelMedium = TextStyle(fontSize = FontTokens.label),
        labelSmall = TextStyle(fontSize = FontTokens.micro),
    )
}
