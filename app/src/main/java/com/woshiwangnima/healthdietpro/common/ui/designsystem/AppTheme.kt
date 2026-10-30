package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) DarkAppTokens else LightAppTokens
    CompositionLocalProvider(LocalAppTokens provides tokens) {
        MaterialTheme(
            colorScheme = tokens.colors.toMaterialColorScheme(darkTheme),
            typography = tokens.type.toMaterialTypography(),
            content = content,
        )
    }
}

@Composable
fun rememberAppDarkTheme(): Boolean {
    val darkMode by AppPrefs.darkMode.collectAsState()
    return when (darkMode) {
        "YES" -> true
        "NO" -> false
        else -> isSystemInDarkTheme()
    }
}
