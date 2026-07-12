package com.woshiwangnima.healthdietpro.ui.profile

enum class ProfileGenderDisplay {
    Male,
    Female,
    Unknown,
}

enum class ProfileGenderTone {
    Male,
    Female,
    Unknown,
}

data class ProfileUserInfoLabels(
    val male: String,
    val female: String,
    val genderUnknown: String,
    val ageUnknown: String,
    val birthdayUnknown: String,
    val separator: String,
    val formatAge: (Int) -> String,
)

data class ProfileInfoLine(
    val text: String,
    val genderIcon: String,
    val genderTone: ProfileGenderTone,
)

fun formatProfileInfoLine(
    gender: ProfileGenderDisplay,
    age: Int?,
    birthday: String?,
    labels: ProfileUserInfoLabels,
): ProfileInfoLine {
    val genderText = when (gender) {
        ProfileGenderDisplay.Male -> labels.male
        ProfileGenderDisplay.Female -> labels.female
        ProfileGenderDisplay.Unknown -> labels.genderUnknown
    }
    val icon = when (gender) {
        ProfileGenderDisplay.Male -> "\u2642"
        ProfileGenderDisplay.Female -> "\u2640"
        ProfileGenderDisplay.Unknown -> ""
    }
    val tone = when (gender) {
        ProfileGenderDisplay.Male -> ProfileGenderTone.Male
        ProfileGenderDisplay.Female -> ProfileGenderTone.Female
        ProfileGenderDisplay.Unknown -> ProfileGenderTone.Unknown
    }
    val ageText = age?.let(labels.formatAge) ?: labels.ageUnknown
    val birthdayText = birthday?.takeIf { it.isNotBlank() } ?: labels.birthdayUnknown

    return ProfileInfoLine(
        text = listOf(genderText, ageText, birthdayText).joinToString(labels.separator),
        genderIcon = icon,
        genderTone = tone,
    )
}
