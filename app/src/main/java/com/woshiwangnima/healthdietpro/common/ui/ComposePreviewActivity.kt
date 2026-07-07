package com.woshiwangnima.healthdietpro.common.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woshiwangnima.healthdietpro.ui.settings.AppSettingsScreen

class ComposePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthDietProTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.THEME_PREVIEW,
                ) {
                    composable(Routes.THEME_PREVIEW) {
                        ThemePreviewScreen(
                            onBack = { finish() },
                            onOpenSettings = { navController.navigate(Routes.APP_SETTINGS) },
                        )
                    }
                    composable(Routes.APP_SETTINGS) {
                        AppSettingsScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

object Routes {
    const val THEME_PREVIEW = "theme_preview"
    const val APP_SETTINGS = "app_settings"
}
