package com.woshiwangnima.healthdietpro.ui.profile

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs

internal class ProfileUserInfoViewModel(
    application: Application,
    private val avatarBitmapCache: ProfileAvatarBitmapCache,
) : AndroidViewModel(application) {

    private val avatarColors = listOf(
        Color(0xFF1976D2),
        Color(0xFF388E3C),
        Color(0xFFF57C00),
        Color(0xFF7B1FA2),
        Color(0xFFC2185B),
        Color(0xFF0097A7),
        Color(0xFF689F38),
        Color(0xFF455A64),
    )

    private val _uiState = MutableStateFlow(ProfileUserInfoUiState())
    val uiState: StateFlow<ProfileUserInfoUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                buildState(ProfilePrefs.load(getApplication()))
            }
            _uiState.value = state
        }
    }

    fun loadUserAvatars(users: List<UserProfile>) {
        viewModelScope.launch {
            val bitmaps = withContext(Dispatchers.IO) {
                users.mapNotNull { user ->
                    avatarPath(user)?.let(avatarBitmapCache::load)?.let { user.id to it }
                }.toMap()
            }
            _uiState.value = _uiState.value.copy(userAvatarBitmaps = bitmaps)
        }
    }

    private fun buildState(profile: UserProfile): ProfileUserInfoUiState {
        val app = getApplication<Application>()
        val displayName = profile.name.ifEmpty { app.getString(R.string.profile_name_unknown) }
        val infoLine = formatProfileInfoLine(
            gender = profile.gender.toDisplay(),
            age = profile.age,
            birthday = profile.birthday?.date,
            labels = ProfileUserInfoLabels(
                male = app.getString(R.string.profile_gender_male),
                female = app.getString(R.string.profile_gender_female),
                genderUnknown = app.getString(R.string.profile_gender_unknown),
                ageUnknown = app.getString(R.string.profile_age_unknown),
                birthdayUnknown = app.getString(R.string.profile_birthday_unknown),
                separator = app.getString(R.string.profile_separator),
                formatAge = { age -> app.getString(R.string.profile_age_years, age) },
            ),
        )
        val colorIndex = abs(profile.id.hashCode()) % avatarColors.size
        val avatarBitmap = avatarPath(profile)?.let(avatarBitmapCache::load)

        return ProfileUserInfoUiState(
            displayName = displayName,
            avatarInitial = displayName.firstOrNull()?.toString() ?: "?",
            avatarColor = avatarColors[colorIndex],
            avatarBitmap = avatarBitmap,
            genderIcon = infoLine.genderIcon,
            genderTone = infoLine.genderTone,
            infoLine = infoLine.text,
            birthdayText = profile.birthday?.date?.takeIf { it.isNotBlank() }
                ?: app.getString(R.string.profile_birthday_unknown),
            regionText = resolveRegionText(profile),
            diseaseText = resolveDiseaseText(profile),
        )
    }

    private fun avatarPath(profile: UserProfile): String? {
        val app = getApplication<Application>()
        return profile.avatarFileName
            .takeIf { it.isNotBlank() }
            ?.let { File(app.filesDir, "avatars/$it") }
            ?.takeIf { it.exists() }
            ?.absolutePath
    }

    private fun Gender.toDisplay(): ProfileGenderDisplay = when (name) {
        "MALE" -> ProfileGenderDisplay.Male
        "FEMALE" -> ProfileGenderDisplay.Female
        else -> ProfileGenderDisplay.Unknown
    }

    private fun resolveRegionText(profile: UserProfile): String {
        val app = getApplication<Application>()
        var regionDisplay = profile.region.display()
        if (profile.region.provinceCode.isNotEmpty() && profile.region.provinceName.isEmpty()) {
            runCatching {
                ProvinceRepository.fromContext(app).findByCode(profile.region.provinceCode)?.name
            }.getOrNull()?.let { name ->
                regionDisplay = profile.region.copy(provinceName = name).display()
            }
        }
        return regionDisplay
    }

    private fun resolveDiseaseText(profile: UserProfile): String {
        if (profile.diseaseIds.isEmpty()) return ""
        val diseases = DiseaseRepository(getApplication()).loadAll()
        val locale = Locale.getDefault()
        return profile.diseaseIds.joinToString("、") { id ->
            diseases.find { it.id == id }?.displayName(locale) ?: id
        }
    }
}
