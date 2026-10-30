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
        separator = " · ",
        formatAge = { age -> "${age}岁" },
    )

    @Test
    fun formatsGenderAndAge() {
        val result = formatProfileHeadline(
            gender = ProfileGenderDisplay.Male,
            age = 28,
            labels = labels,
        )

        assertEquals("男 · 28岁", result.text)
        assertEquals("♂", result.genderIcon)
        assertEquals(ProfileGenderTone.Male, result.genderTone)
    }

    @Test
    fun formatsUnknownAge() {
        val result = formatProfileHeadline(
            gender = ProfileGenderDisplay.Male,
            age = null,
            labels = labels,
        )

        assertEquals("男 · 年龄未知", result.text)
        assertNoBadSeparator(result.text)
    }

    @Test
    fun formatsUnknownGenderAndAge() {
        val result = formatProfileHeadline(
            gender = ProfileGenderDisplay.Unknown,
            age = null,
            labels = labels,
        )

        assertEquals("性别未知 · 年龄未知", result.text)
        assertEquals("", result.genderIcon)
        assertEquals(ProfileGenderTone.Unknown, result.genderTone)
        assertNoBadSeparator(result.text)
    }

    @Test
    fun formatsFemale() {
        val result = formatProfileHeadline(
            gender = ProfileGenderDisplay.Female,
            age = 30,
            labels = labels,
        )

        assertEquals("女 · 30岁", result.text)
        assertEquals("♀", result.genderIcon)
        assertEquals(ProfileGenderTone.Female, result.genderTone)
    }

    private fun assertNoBadSeparator(text: String) {
        assertFalse(text.startsWith(" · "))
        assertFalse(text.endsWith(" · "))
        assertFalse(text.contains(" ·  · "))
    }
}
