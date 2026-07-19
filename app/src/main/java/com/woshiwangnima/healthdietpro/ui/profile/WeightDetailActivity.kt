package com.woshiwangnima.healthdietpro.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType

class WeightDetailActivity : BaseActivity() {
    private val viewModel: WeightDetailViewModel by viewModels()
    private var records by mutableStateOf<List<BodyRecord>>(emptyList())
    private var unit = UnitCategoryType.Weight.defaultUnitId
    private val recordEditorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        @Suppress("DEPRECATION")
        val record = result.data?.getSerializableExtra(BodyMetricRecordActivity.EXTRA_RECORD) as? BodyRecord ?: return@registerForActivityResult
        val position = result.data?.getIntExtra(BodyMetricRecordActivity.EXTRA_POSITION, -1) ?: -1
        records = if (position in records.indices) {
            records.mapIndexed { index, old -> if (index == position) record else old }
        } else {
            records + record
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNCHECKED_CAST")
        records = (intent.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>).orEmpty()
        unit = intent.getStringExtra("unit") ?: UnitCategoryType.Weight.defaultUnitId
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = saveAndFinish()
        })
        setContent {
            HealthDietProTheme {
                BodyMetricDetailScreen(
                    title = getString(R.string.weight_history_title), isHeight = false, unitId = unit,
                    category = UnitCategoryType.Weight.id, records = records,
                    initialTab = AppPrefs.getWeightChartTab(this), chartViewModel = viewModel,
                    onTabSelected = { AppPrefs.setWeightChartTab(this, it) }, onRecordsChanged = { records = it },
                    onEditRecord = ::openRecordEditor,
                    onBack = ::saveAndFinish,
                )
            }
        }
    }

    private fun saveAndFinish() {
        intent.putExtra("records", ArrayList(records))
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun openRecordEditor(position: Int) {
        recordEditorLauncher.launch(
            Intent(this, BodyMetricRecordActivity::class.java)
                .putExtra(BodyMetricRecordActivity.EXTRA_IS_HEIGHT, false)
                .putExtra(BodyMetricRecordActivity.EXTRA_UNIT_ID, unit)
                .putExtra(BodyMetricRecordActivity.EXTRA_CATEGORY, UnitCategoryType.Weight.id)
                .putExtra(BodyMetricRecordActivity.EXTRA_POSITION, position)
                .putExtra(BodyMetricRecordActivity.EXTRA_RECORD, records.getOrNull(position)),
        )
    }
}
