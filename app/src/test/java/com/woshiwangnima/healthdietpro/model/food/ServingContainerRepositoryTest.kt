package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServingContainerRepositoryTest {
    private fun repo() = ServingContainerRepository.fromAsset("src/main/assets/serving_containers.json")

    @Test
    fun loadsFixedCupAndBowlCapacities() {
        val byId = repo().byId()
        assertEquals(150.0, requireNotNull(byId["cup_small"]).capacityMl, 0.0001)
        assertEquals(250.0, requireNotNull(byId["cup_medium"]).capacityMl, 0.0001)
        assertEquals(400.0, requireNotNull(byId["cup_large"]).capacityMl, 0.0001)
        assertEquals(200.0, requireNotNull(byId["bowl_small"]).capacityMl, 0.0001)
        assertEquals(350.0, requireNotNull(byId["bowl_medium"]).capacityMl, 0.0001)
        assertEquals(500.0, requireNotNull(byId["bowl_large"]).capacityMl, 0.0001)
    }

    @Test
    fun uniqueIdsAndValidFillRatios() {
        val containers = repo().containers()
        assertEquals(containers.size, containers.map { it.id }.toSet().size)
        assertTrue(containers.all { it.defaultFillRatio in 0.0..1.0 })
        assertTrue(containers.all { it.capacityMl > 0.0 })
    }

    @Test
    fun mediumCupOfWaterIsAboutCapacityGrams() {
        val cup = requireNotNull(repo().find("cup_medium"))
        val grams = NutritionResolver.containerGrams(cup, densityGramsPerMl = 1.0, fillRatio = 1.0)
        assertEquals(250.0, grams, 0.0001)
    }
}
