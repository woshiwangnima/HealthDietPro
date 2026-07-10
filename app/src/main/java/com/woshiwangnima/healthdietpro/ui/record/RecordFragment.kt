package com.woshiwangnima.healthdietpro.ui.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.ui.profile.HeightDetailActivity
import com.woshiwangnima.healthdietpro.ui.profile.WeightDetailActivity

class RecordFragment : Fragment() {

    private val viewModel: RecordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HealthDietProTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    RecordScreen(
                        uiState = uiState,
                        onActionClick = ::handleActionClick,
                    )
                }
            }
        }
    }

    private fun handleActionClick(actionId: RecordActionId) {
        when (actionId) {
            RecordActionId.Height -> openHeightDetail()
            RecordActionId.Weight -> openWeightDetail()
            RecordActionId.Medication -> startActivity(Intent(requireContext(), MedicationListActivity::class.java))
            RecordActionId.Waist,
            RecordActionId.Period,
            RecordActionId.Diet,
            RecordActionId.Water,
            RecordActionId.Exercise,
            RecordActionId.Sleep,
            RecordActionId.Bowel,
            RecordActionId.Habit,
            RecordActionId.Feeling,
            -> Unit
        }
    }

    private fun openHeightDetail() {
        val profile = ProfilePrefs.load(requireContext())
        val records = ArrayList(profile.heightRecords)
        startActivity(Intent(requireContext(), HeightDetailActivity::class.java).apply {
            putExtra("records", records)
            putExtra("unit", UnitCategoryType.Length.defaultUnitId)
        })
    }

    private fun openWeightDetail() {
        val profile = ProfilePrefs.load(requireContext())
        val records = ArrayList(profile.weightRecords)
        startActivity(Intent(requireContext(), WeightDetailActivity::class.java).apply {
            putExtra("records", records)
            putExtra("unit", UnitCategoryType.Weight.defaultUnitId)
        })
    }
}
