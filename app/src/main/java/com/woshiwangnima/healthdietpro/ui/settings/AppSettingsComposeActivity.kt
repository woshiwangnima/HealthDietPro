package com.woshiwangnima.healthdietpro.ui.settings

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme

class AppSettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 外层保留旧主题：它负责把「文字显示」里的字号倍率灌进 LocalDensity，
            // 丢掉它这一页改字号就不再有任何反馈。内层 AppTheme 只接管颜色与排版，
            // 让设置页与「我的」「用户设置」共用同一套 token（不再绿蓝两套并存）。
            HealthDietProTheme {
                AppTheme(darkTheme = rememberAppDarkTheme()) {
                    var route by rememberSaveable { mutableStateOf(SettingsRoute.Root) }
                    BackHandler(enabled = route != SettingsRoute.Root) { route = SettingsRoute.Root }
                    AnimatedPageContent(
                        targetState = route,
                        direction = { initialRoute, targetRoute -> targetRoute.ordinal - initialRoute.ordinal },
                    ) { currentRoute ->
                        when (currentRoute) {
                            SettingsRoute.Root -> AppSettingsScreen(
                                onBack = {
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                },
                                onOpenTextDisplay = { route = SettingsRoute.TextDisplay },
                                onOpenAbout = { route = SettingsRoute.About },
                            )
                            SettingsRoute.TextDisplay -> TextDisplaySettingsScreen { route = SettingsRoute.Root }
                            SettingsRoute.About -> AboutScreen { route = SettingsRoute.Root }
                        }
                    }
                }
            }
        }
    }
}

private enum class SettingsRoute {
    Root,
    TextDisplay,
    About,
}
