package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
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
        ) {
            // `MaterialTheme` 本身**不提供** `LocalContentColor`：不在 `Surface` / 按钮 / 输入框里的
            // `Text` 会落到它的默认值 `Color.Black`。深色底上是纯黑配近黑，几乎读不出来
            // （物价「选择地区」的省/市列表就踩过这个坑）。这里在主题层兜一个 `text`，
            // 让「没写颜色」的文本也永远是主题正文色；`Surface`、`Button`、`TextField` 等
            // 会在更深的位置覆盖它，行为不变。
            CompositionLocalProvider(LocalContentColor provides tokens.colors.text) {
                content()
            }
        }
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
