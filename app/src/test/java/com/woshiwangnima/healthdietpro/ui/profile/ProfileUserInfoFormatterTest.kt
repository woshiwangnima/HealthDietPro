package com.woshiwangnima.healthdietpro.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileUserInfoFormatterTest {
    private val labels = ProfileUserInfoLabels(
        male = "男",
        female = "女",
        genderUnknown = "性别未知",
        ageUnknown = "年龄未知",
        birthdayUnknown = "出生年月未知",
        separator = " | ",
        formatAge = { age -> "${age}岁" },
    )

    @Test
    fun formatsCompleteGenderAgeBirthday() {
        val result = formatProfileInfoLine(
            gender = ProfileGenderDisplay.Male,
            age = 28,
            birthday = "1990-01-15",
            labels = labels,
        )

        assertEquals("男 | 28岁 | 1990-01-15", result.text)
        assertEquals("♂", result.genderIcon)
        assertEquals(ProfileGenderTone.Male, result.genderTone)
    }

    @Test
    fun formatsUnknownAgeAndBirthdayWithoutExtraSeparators() {
        val result = formatProfileInfoLine(
            gender = ProfileGenderDisplay.Male,
            age = null,
            birthday = null,
            labels = labels,
        )

        assertEquals("男 | 年龄未知 | 出生年月未知", result.text)
        assertNoBadSeparator(result.text)
    }

    @Test
    fun formatsUnknownGenderAgeAndBirthday() {
        val result = formatProfileInfoLine(
            gender = ProfileGenderDisplay.Unknown,
            age = null,
            birthday = "",
            labels = labels,
        )

        assertEquals("性别未知 | 年龄未知 | 出生年月未知", result.text)
        assertEquals("", result.genderIcon)
        assertEquals(ProfileGenderTone.Unknown, result.genderTone)
        assertNoBadSeparator(result.text)
    }

    @Test
    fun formatsFemale() {
        val result = formatProfileInfoLine(
            gender = ProfileGenderDisplay.Female,
            age = 30,
            birthday = "1994-02-03",
            labels = labels,
        )

        assertEquals("女 | 30岁 | 1994-02-03", result.text)
        assertEquals("♀", result.genderIcon)
        assertEquals(ProfileGenderTone.Female, result.genderTone)
    }

    private fun assertNoBadSeparator(text: String) {
        assertFalse(text.startsWith(" | "))
        assertFalse(text.endsWith(" | "))
        assertFalse(text.contains(" |  | "))
    }
}
