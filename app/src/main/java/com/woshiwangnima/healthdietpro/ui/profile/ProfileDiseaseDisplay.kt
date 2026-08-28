package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.ui.graphics.Color
import com.woshiwangnima.healthdietpro.model.disease.DiseaseStatus

internal data class ProfileDiseaseDisplay(
    val name: String,
    val status: DiseaseStatus,
)

internal fun DiseaseStatus.profileColor(): Color = when (this) {
    DiseaseStatus.ACTIVE -> Color(0xFFE53935)
    DiseaseStatus.ONGOING_RISK -> Color(0xFFF57C00)
    DiseaseStatus.RESOLVED -> Color(0xFF43A047)
    DiseaseStatus.HISTORY_ONLY -> Color(0xFF607D8B)
}
