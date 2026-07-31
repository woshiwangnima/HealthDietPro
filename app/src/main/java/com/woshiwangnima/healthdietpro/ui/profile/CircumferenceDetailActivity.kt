package com.woshiwangnima.healthdietpro.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType

internal enum class CircumferenceMetric(val id: String, val titleRes: Int) {
    Waist("waist", R.string.circumference_waist),
    Thigh("thigh", R.string.circumference_thigh),
    Calf("calf", R.string.circumference_calf),
    Hip("hip", R.string.circumference_hip),
    Chest("chest", R.string.circumference_chest),
    Arm("arm", R.string.circumference_arm),
}

class CircumferenceDetailActivity : BaseActivity() {
    private var recordsByMetric by mutableStateOf<Map<String, List<BodyRecord>>>(emptyMap())
    private var selectedMetric by mutableIntStateOf(0)
    private var editorMetric: CircumferenceMetric? = null
    private var selectingMetricForNewRecord = false
    private val recordEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val metric = editorMetric ?: return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        @Suppress("DEPRECATION") val record = result.data?.getSerializableExtra(BodyMetricRecordActivity.EXTRA_RECORD) as? BodyRecord ?: return@registerForActivityResult
        val position = result.data?.getIntExtra(BodyMetricRecordActivity.EXTRA_POSITION, -1) ?: -1
        val old = recordsByMetric[metric.id].orEmpty()
        val updated = if (position in old.indices) old.mapIndexed { index, value -> if (index == position) record else value } else old + record
        recordsByMetric = recordsByMetric + (metric.id to updated)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        recordsByMetric = readRecords(intent.getSerializableExtra(EXTRA_RECORDS))
        selectingMetricForNewRecord = intent.getBooleanExtra(EXTRA_SELECT_METRIC_FOR_NEW_RECORD, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() = saveAndFinish() })
        setContent {
            HealthDietProTheme {
                if (selectingMetricForNewRecord) {
                    CircumferenceMetricPicker(onBack = ::saveAndFinish, onMetricSelected = { metric -> openEditor(metric, -1) })
                } else {
                    BaseScreen(title = getString(R.string.circumference_title), onBack = ::saveAndFinish, includeNavigationBarPadding = false) { padding ->
                        val metric = CircumferenceMetric.entries[selectedMetric]
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            EqualWidthSegmentedTabs(CircumferenceMetric.entries.map { EqualWidthTab(it.titleRes) }, selectedMetric, { selectedMetric = it })
                            val chart = androidx.lifecycle.viewmodel.compose.viewModel<CircumferenceChartViewModel>()
                            BodyMetricDetailScreen(
                                title = getString(metric.titleRes), isHeight = true, unitId = "cm", category = UnitCategoryType.Length.id,
                                records = recordsByMetric[metric.id].orEmpty(), initialTab = 0, chartViewModel = chart,
                                onTabSelected = {}, onRecordsChanged = { recordsByMetric = recordsByMetric + (metric.id to it) },
                                onEditRecord = { position -> openEditor(metric, position) }, onBack = ::saveAndFinish, embedded = true, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openEditor(metric: CircumferenceMetric, position: Int) {
        editorMetric = metric
        recordEditor.launch(Intent(this, BodyMetricRecordActivity::class.java)
            .putExtra(BodyMetricRecordActivity.EXTRA_IS_HEIGHT, true)
            .putExtra(BodyMetricRecordActivity.EXTRA_UNIT_ID, "cm")
            .putExtra(BodyMetricRecordActivity.EXTRA_CATEGORY, UnitCategoryType.Length.id)
            .putExtra(BodyMetricRecordActivity.EXTRA_CUSTOM_METRIC_TITLE, getString(metric.titleRes))
            .putExtra(BodyMetricRecordActivity.EXTRA_POSITION, position)
            .putExtra(BodyMetricRecordActivity.EXTRA_RECORD, recordsByMetric[metric.id].orEmpty().getOrNull(position)))
    }

    private fun saveAndFinish() {
        intent.putExtra(EXTRA_RECORDS, java.util.HashMap(recordsByMetric.mapValues { ArrayList(it.value) }))
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val EXTRA_RECORDS = "circumference_records"
        const val EXTRA_SELECT_METRIC_FOR_NEW_RECORD = "select_metric_for_new_record"

        /** Validates the serialized map boundary without an unchecked generic cast. */
        fun readRecords(value: java.io.Serializable?): Map<String, List<BodyRecord>> = (value as? Map<*, *>)
            ?.mapNotNull { (key, records) ->
                val metricId = key as? String ?: return@mapNotNull null
                val bodyRecords = (records as? List<*>)?.filterIsInstance<BodyRecord>() ?: return@mapNotNull null
                metricId to bodyRecords
            }
            ?.toMap()
            .orEmpty()
    }
}

@androidx.compose.runtime.Composable
private fun CircumferenceMetricPicker(
    onBack: () -> Unit,
    onMetricSelected: (CircumferenceMetric) -> Unit,
) {
    BaseScreen(title = androidx.compose.ui.res.stringResource(R.string.circumference_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CircumferenceMetric.entries.forEach { metric ->
                SettingRow(
                    title = androidx.compose.ui.res.stringResource(metric.titleRes),
                    subtitle = "",
                    leadingIconRes = R.drawable.ic_circumference,
                    onClick = { onMetricSelected(metric) },
                )
            }
        }
    }
}

internal class CircumferenceChartViewModel(application: android.app.Application) : com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartViewModel(application, "circumference_history")
