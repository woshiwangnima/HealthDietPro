package com.woshiwangnima.healthdietpro.ui.settings

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme

class AppSettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthDietProTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.APP_SETTINGS,
                ) {
                    composable(Routes.APP_SETTINGS) {
                        AppSettingsScreen(
                            onBack = {
                                setResult(Activity.RESULT_OK)
                                finish()
                            },
                            onOpenTextDisplay = { navController.navigate(Routes.TEXT_DISPLAY) },
                        )
                    }
                    composable(Routes.TEXT_DISPLAY) { TextDisplaySettingsScreen { navController.popBackStack() } }
                }
            }
        }
    }
}

private object Routes {
    const val APP_SETTINGS = "app_settings"
    const val TEXT_DISPLAY = "text_display"
}
