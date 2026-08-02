package com.woshiwangnima.healthdietpro.model.water

import com.woshiwangnima.healthdietpro.model.profile.Gender
import org.junit.Assert.assertEquals
import org.junit.Test

class WaterRecommendationTest {
    @Test
    fun latestWeightTakesPrecedenceAndActivityAddsToRecommendation() {
        assertEquals(
            2_950,
            recommendedWaterMl(Gender.FEMALE, age = 35, latestWeightKg = 70f, activityLevel = ActivityLevel.MODERATE),
        )
    }

    @Test
    fun unknownAgeUsesAdultSexDefault() {
        assertEquals(
            3_000,
            recommendedWaterMl(Gender.MALE, age = null, latestWeightKg = null, activityLevel = ActivityLevel.NONE),
        )
    }
}
