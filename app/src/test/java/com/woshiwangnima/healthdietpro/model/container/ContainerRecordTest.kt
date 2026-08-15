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
        capacityMode: ContainerCapacityMode = ContainerCapacityMode.MANUAL,
        capacityMl: Double = 250.0,
        emptyMassGrams: Double? = 120.0,
        note: String = "",
        imagePaths: List<String> = emptyList(),
        crossSections: CrossSectionProfileDto? = null,
        scenarioTags: List<String> = emptyList(),
    ): ContainerRecord = ContainerRecord(
        id = id,
        name = name,
        category = category,
        capacityMode = capacityMode,
        capacityMl = capacityMl,
        emptyMassGrams = emptyMassGrams,
        note = note,
        imagePaths = imagePaths,
        crossSections = crossSections,
        scenarioTags = scenarioTags,
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
    fun `migration normalizes scenario tags and drops unregistered tags`() {
        val archive = ContainerArchive(
            scenarioTags = listOf("  家 ", "学校", ""),
            containers = listOf(
                record(scenarioTags = listOf("家", "公司", " 学校 ")),
            ),
        )
        val migrated = migrateContainerArchive(archive)
        assertEquals(listOf("家", "学校"), migrated.scenarioTags)
        assertEquals(listOf("家", "学校"), migrated.containers[0].scenarioTags)
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
    fun `name is optional and blank names are accepted`() {
        val store = ContainerArchiveStoreValidationHarness()
        store.validate(ContainerArchive(containers = listOf(record(id = "c1", name = ""), record(id = "c2", name = "   "))))
    }

    @Test
    fun `cross-section capacity mode requires a cross-section profile`() {
        val store = ContainerArchiveStoreValidationHarness()
        try {
            store.validate(ContainerArchive(containers = listOf(record(capacityMode = ContainerCapacityMode.CROSS_SECTION, capacityMl = 200.0))))
            fail("Expected IllegalArgumentException for missing cross-section profile")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `container scenario tags must be registered`() {
        val store = ContainerArchiveStoreValidationHarness()
        try {
            store.validate(
                ContainerArchive(
                    scenarioTags = listOf("家"),
                    containers = listOf(record(scenarioTags = listOf("学校"))),
                ),
            )
            fail("Expected IllegalArgumentException for unregistered scenario tag")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `manual capacity grows linearly with height percent`() {
        val container = record(capacityMode = ContainerCapacityMode.MANUAL, capacityMl = 250.0)
        assertEquals(0.0, container.capacityMlAtHeightPercent(0.0)!!, 1e-9)
        assertEquals(125.0, container.capacityMlAtHeightPercent(50.0)!!, 1e-9)
        assertEquals(250.0, container.capacityMlAtHeightPercent(100.0)!!, 1e-9)
        assertEquals(250.0, container.capacityMlAtHeightPercent(150.0)!!, 1e-9)
    }

    @Test
    fun `cross-section capacity at height percent integrates the profile`() {
        val profile = CrossSectionProfileDto(
            totalHeightCm = 10.0,
            lengthUnitId = "cm",
            points = listOf(
                CrossSectionDto(0.0, CrossSectionShapeDto(kind = "circle", diameterCm = 10.0)),
                CrossSectionDto(10.0, CrossSectionShapeDto(kind = "circle", diameterCm = 10.0)),
            ),
        )
        val container = record(capacityMode = ContainerCapacityMode.CROSS_SECTION, capacityMl = 785.4, crossSections = profile)
        val domain = requireNotNull(profile.toDomain())
        assertEquals(domain.volumeUpTo(5.0), container.capacityMlAtHeightPercent(50.0)!!, 1e-6)
        assertEquals(domain.totalVolumeMl(), container.capacityMlAtHeightPercent(100.0)!!, 1e-6)
    }

    @Test
    fun `cross-section capacity lookup returns null without profile`() {
        val container = record(capacityMode = ContainerCapacityMode.CROSS_SECTION, capacityMl = 200.0)
        assertNull(container.capacityMlAtHeightPercent(50.0))
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

/** Test-only seam that exposes the pure archive validation. */
internal class ContainerArchiveStoreValidationHarness {
    fun validate(archive: ContainerArchive) = validateContainerArchive(archive)
}