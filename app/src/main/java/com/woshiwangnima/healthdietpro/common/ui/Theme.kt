package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightPrimary = Color(0xFF4CAF50)
private val LightPrimaryContainer = Color(0xFFC8E6C9)
private val LightOnPrimaryContainer = Color(0xFF1B5E20)
private val LightSecondary = Color(0xFFFF7043)
private val LightSecondaryContainer = Color(0xFFFFDCC1)
private val LightSurface = Color(0xFFFFFBFE)
private val LightOnSurface = Color(0xFF1C1B1F)
private val LightOnSurfaceVariant = Color(0xFF49454F)
private val LightOutlineVariant = Color(0xFFCAC4D0)

private val DarkPrimary = Color(0xFF81C784)
private val DarkPrimaryContainer = Color(0xFF1B5E20)
private val DarkOnPrimaryContainer = Color(0xFFC8E6C9)
private val DarkSecondary = Color(0xFFFF8A65)
private val DarkSecondaryContainer = Color(0xFF3E2723)
private val DarkSurface = Color(0xFF1C1B1F)
private val DarkOnSurface = Color(0xFFE6E1E5)
private val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
private val DarkOutlineVariant = Color(0xFF49454F)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    secondaryContainer = LightSecondaryContainer,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outlineVariant = LightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outlineVariant = DarkOutlineVariant,
)

private val AppTypography @Composable get() = appTypography()

@Composable
fun HealthDietProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
