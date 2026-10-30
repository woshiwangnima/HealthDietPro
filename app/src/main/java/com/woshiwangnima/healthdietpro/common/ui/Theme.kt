package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

/**
 * 全局主题 = 字号倍率（[LocalDensity]）+ 设计系统主题（[AppTheme]）。
 *
 * 收口说明（计划书 G 批）：这里以前自己拼了一套 `lightColorScheme` / `darkColorScheme` 与
 * `appTypography()`，和 `designsystem/AppTokens` 是两套色两套字号。结果是「设计系统组件按新令牌取色、
 * 业务代码按老 MaterialTheme 取色」，同一张卡里卡片是新色、文字是老色。现在只保留字号倍率这一层职责，
 * 配色与字号一律交给 [AppTheme]，全站因此只有一套值。
 *
 * 屏内**不需要**再套一层 `AppTheme`：`LocalAppTokens` 已由此处提供。历史上已套的屏保留也不会错
 * （内层覆盖外层，值相同）。
 */
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
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = fontScale,
        )
    ) {
        DisposableEffect(context) { AppPrefs.loadDarkMode(context); onDispose {} }
        AppTheme(darkTheme = resolvedDarkTheme, content = content)
    }
}
