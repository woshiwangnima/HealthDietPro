package com.woshiwangnima.healthdietpro.model.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ContainerRecordTest {

    private fun record(
        id: String = "c1",
        name: String = "我的水杯",
        category: ContainerCategory = ContainerCategory.CUP,
        capacityMl: Double = 250.0,
        emptyMassGrams: Double? = 120.0,
        note: String = "",
        imagePaths: List<String> = emptyList(),
        crossSections: CrossSectionProfileDto? = null,
    ): ContainerRecord = ContainerRecord(
        id = id,
        name = name,
        category = category,
        capacityMl = capacityMl,
        emptyMassGrams = emptyMassGrams,
        note = note,
        imagePaths = imagePaths,
        crossSections = crossSections,
    )

    @Test
    fun `migration trims names and notes and dedupes image paths`() {
        val archive = ContainerArchive(
            containers = listOf(
                record(name = "  杯子  ", note = "  客厅用  ", imagePaths = listOf("a.jpg", "a.jpg", "b.jpg")),
            ),
        )
        val migrated = migrateContainerArchive(archive)
        assertEquals("杯子", migrated.containers[0].name)
        assertEquals("客厅用", migrated.containers[0].note)
        assertEquals(listOf("a.jpg", "b.jpg"), migrated.containers[0].imagePaths)
        assertEquals(CONTAINER_ARCHIVE_SCHEMA_VERSION, migrated.schemaVersion)
    }

    @Test
    fun `empty mass can be optional and zero is dropped`() {
        val withMass = record(emptyMassGrams = 150.0)
        assertEquals(150.0, withMass.emptyMassGrams!!, 1e-9)

        val migrated = migrateContainerArchive(ContainerArchive(containers = listOf(record(emptyMassGrams = 0.0))))
        assertNull(migrated.containers[0].emptyMassGrams)
    }

    @Test
    fun `capacity must be positive`() {
        val store = ContainerArchiveStoreValidationHarness()
        try {
            store.validate(ContainerArchive(containers = listOf(record(capacityMl = -5.0))))
            fail("Expected IllegalArgumentException for negative capacity")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `cross section dto round trip preserves volume`() {
        val profile = CrossSectionProfileDto(
            totalHeightCm = 10.0,
            lengthUnitId = "cm",
            points = listOf(
                CrossSectionDto(0.0, CrossSectionShapeDto(kind = "circle", diameterCm = 10.0)),
                CrossSectionDto(10.0, CrossSectionShapeDto(kind = "circle", diameterCm = 20.0)),
            ),
        )
        val domain = profile.toDomain()
        val volume = domain.totalVolumeMl()
        assertTrue(volume > 0.0)
        val back = domain.toDto()
        assertEquals(profile.points.size, back.points.size)
        assertEquals(profile.totalHeightCm, back.totalHeightCm, 1e-9)
    }
}

/** Test-only seam that exposes the private validation of [ContainerArchiveStore]. */
internal class ContainerArchiveStoreValidationHarness {
    fun validate(archive: ContainerArchive) = validateArchive(archive)
}
