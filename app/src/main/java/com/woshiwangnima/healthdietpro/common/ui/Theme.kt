package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

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
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = Color(0xFF3E2723),
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = LightOnSurfaceVariant,
    background = LightSurface,
    onBackground = LightOnSurface,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = Color(0xFF79747E),
    outlineVariant = LightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = Color.Black,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFFFDCC1),
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = DarkOnSurfaceVariant,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    outline = Color(0xFF938F99),
    outlineVariant = DarkOutlineVariant,
)

private val AppTypography @Composable get() = appTypography()

@Composable
fun HealthDietProTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkMode by AppPrefs.darkMode.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context, lifecycleOwner) {
        AppFontScaleState.load(context)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                AppFontScaleState.load(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val fontScale by AppFontScaleState.scale.collectAsState()
    val density = LocalDensity.current
    val resolvedDarkTheme = darkTheme ?: when (darkMode) {
        "YES" -> true
        "NO" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (resolvedDarkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = fontScale,
        )
    ) {
        DisposableEffect(context) { AppPrefs.loadDarkMode(context); onDispose {} }
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
