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

class AppSettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthDietProTheme {
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
                            onOpenLanguage = { route = SettingsRoute.Language },
                            onOpenDisclaimer = { route = SettingsRoute.Disclaimer },
                            onOpenAbout = { route = SettingsRoute.About },
                        )
                        SettingsRoute.TextDisplay -> TextDisplaySettingsScreen { route = SettingsRoute.Root }
                        SettingsRoute.Language -> LanguageSettingsScreen { route = SettingsRoute.Root }
                        SettingsRoute.Disclaimer -> DisclaimerScreen { route = SettingsRoute.Root }
                        SettingsRoute.About -> AboutScreen { route = SettingsRoute.Root }
                    }
                }
            }
        }
    }
}

private enum class SettingsRoute {
    Root,
    TextDisplay,
    Language,
    Disclaimer,
    About,
}
