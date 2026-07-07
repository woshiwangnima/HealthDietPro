package com.woshiwangnima.healthdietpro.ui.settings

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme

class AppSettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthDietProTheme {
                AppSettingsScreen(
                    onBack = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                )
            }
        }
    }
}
