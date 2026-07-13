package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme

class BloodGlucoseActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                BloodGlucoseScreen(onBack = ::finish)
            }
        }
    }
}

@Composable
private fun BloodGlucoseScreen(onBack: () -> Unit) {
    BaseScreen(
        title = stringResource(R.string.blood_glucose_title),
        onBack = onBack,
    ) { padding ->
        Text(
            text = stringResource(R.string.blood_glucose_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}
