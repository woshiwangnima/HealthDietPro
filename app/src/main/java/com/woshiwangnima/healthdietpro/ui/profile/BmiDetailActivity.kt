package com.woshiwangnima.healthdietpro.ui.profile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.util.UnitConverter

class BmiDetailActivity : BaseBackActivity() {

    private val viewModel: BmiDetailViewModel by viewModels()

    override fun getTitleText(): String = getString(R.string.bmi_history_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(this)
        setContent {
            HealthDietProTheme {
                BmiDetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                )
            }
        }
    }
}
