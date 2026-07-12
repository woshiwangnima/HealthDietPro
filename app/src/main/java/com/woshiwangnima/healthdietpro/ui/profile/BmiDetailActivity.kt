package com.woshiwangnima.healthdietpro.ui.profile

import android.os.Bundle
import androidx.activity.compose.setContent
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil

class BmiDetailActivity : BaseBackActivity() {

    override fun getTitleText(): String = getString(R.string.bmi_history_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = ProfilePrefs.load(this)
        val bmiData = BmiUtil.buildBmiDataPoints(profile.weightRecords, profile.heightRecords)
        setContent {
            HealthDietProTheme {
                BmiDetailScreen(
                    bmiData = bmiData,
                    initialTab = AppPrefs.getBmiChartTab(this),
                    onBack = { finish() },
                    onTabSelected = { AppPrefs.setBmiChartTab(this, it) },
                )
            }
        }
    }
}
