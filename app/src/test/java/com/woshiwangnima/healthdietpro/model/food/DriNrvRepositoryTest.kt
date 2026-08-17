package com.woshiwangnima.healthdietpro.model.food

import com.woshiwangnima.healthdietpro.model.profile.AppDate
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriNrvRepositoryTest {
    private val childPopulation = """
        {"population":{"id":"children_7_8y_male"},"data":[
          {"code":"PROTEIN","RNI":40,"unit":"g/d"},
          {"code":"CA","RNI":800,"unit":"mg/d"}
        ]}
    """.trimIndent()

    @Test
    fun `uses matching DRI population when its asset is available`() {
        val repository = DriNrvRepository.fromPopulationAssets(mapOf("children_7_8y_male.json" to childPopulation))
        val profile = UserProfile(
            gender = Gender.MALE,
            birthday = AppDate(java.time.LocalDate.now().minusYears(8).toString()),
        )

        val reference = repository.referenceFor(profile)

        assertEquals("children_7_8y_male", reference.id)
        assertEquals(25.0, reference.percent("PROTEIN", FoodAmount(10.0, "weight", "g"), 1.0)!!, 0.0001)
    }

    @Test
    fun `falls back to adult food label NRV when no matching population asset exists`() {
        val repository = DriNrvRepository.fromPopulationAssets(emptyMap())

        val reference = repository.referenceFor(UserProfile(gender = Gender.FEMALE))

        assertEquals(DriNrvRepository.ADULT_REFERENCE_ID, reference.id)
        assertEquals(20.0, reference.percent("PROTEIN", FoodAmount(12.0, "weight", "g"), 1.0)!!, 0.0001)
        assertNull(reference.percent("WATER", FoodAmount(100.0, "volume", "mL"), 1.0))
    }
}
