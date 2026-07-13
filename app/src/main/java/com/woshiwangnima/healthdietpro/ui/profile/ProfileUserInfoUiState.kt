package com.woshiwangnima.healthdietpro.ui.profile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

internal data class ProfileUserInfoUiState(
    val displayName: String = "",
    val avatarInitial: String = "?",
    val avatarColor: Color = Color(0xFF1976D2),
    val avatarBitmap: Bitmap? = null,
    val userAvatarBitmaps: Map<String, Bitmap> = emptyMap(),
    val genderIcon: String = "",
    val genderTone: ProfileGenderTone = ProfileGenderTone.Unknown,
    val infoLine: String = "",
    val birthdayText: String = "",
    val regionText: String = "",
    val diseaseText: String = "",
) {
    val hasDiseaseText: Boolean get() = diseaseText.isNotBlank()
}
