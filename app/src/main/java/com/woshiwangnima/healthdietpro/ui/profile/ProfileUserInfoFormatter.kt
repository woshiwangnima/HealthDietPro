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
    /** 性别与年龄之间的分隔符（含两侧空格，例如「 · 」）。 */
    val separator: String,
    val formatAge: (Int) -> String,
)

data class ProfileInfoLine(
    val text: String,
    val genderIcon: String,
    val genderTone: ProfileGenderTone,
)

/**
 * 卡片姓名下方那一行：只放「性别 · 年龄」。
 *
 * 生日与地区各自有独立的图标行。旧版把生日也拼进这一行，导致同一张卡片里生日出现两次
 * （拼在信息行里、又单独一行），而且卡片还得靠 `split("|")` 取前两段来遮掉重复。
 */
fun formatProfileHeadline(
    gender: ProfileGenderDisplay,
    age: Int?,
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

    return ProfileInfoLine(
        text = listOf(genderText, ageText).joinToString(labels.separator),
        genderIcon = icon,
        genderTone = tone,
    )
}
