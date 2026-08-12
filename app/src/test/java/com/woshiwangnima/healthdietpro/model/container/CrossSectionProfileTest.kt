package com.woshiwangnima.healthdietpro.model.container

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CrossSectionProfileTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun cylinderProfile(diameterCm: Double, totalHeightCm: Double): CrossSectionProfile = CrossSectionProfile(
        points = listOf(CrossSection(0.0, CircleShape(diameterCm))),
        totalHeightCm = totalHeightCm,
    )

    @Test
    fun singlePointIsUniformPrism() {
        val profile = cylinderProfile(10.0, 20.0)
        val expected = Math.PI * 25.0 * 20.0
        assertEquals(expected, profile.totalVolumeMl(), 1e-6)
    }

    @Test
    fun partialFillOfSinglePointScalesWithHeight() {
        val profile = cylinderProfile(10.0, 20.0)
        val full = profile.totalVolumeMl()
        assertEquals(full / 2.0, profile.volumeUpTo(10.0), 1e-6)
    }

    @Test
    fun parallelExtensionBelowFirstAndAboveLastPoint() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(8.0, CircleShape(10.0)),
                CrossSection(12.0, CircleShape(14.0)),
            ),
            totalHeightCm = 16.0,
        )
        val areaBottom = Math.PI * 25.0
        val areaTop = Math.PI * 49.0
        val frustum = (4.0 / 3.0) * (areaBottom + areaTop + Math.sqrt(areaBottom * areaTop))
        val expected = 8.0 * areaBottom + frustum + 4.0 * areaTop
        assertEquals(expected, profile.totalVolumeMl(), 1e-6)
    }

    @Test
    fun frustumMatchesTruncatedConeFormula() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(8.0)),
                CrossSection(10.0, CircleShape(20.0)),
            ),
            totalHeightCm = 10.0,
        )
        val expected = (10.0 / 3.0) * (Math.PI * 16.0 + Math.PI * 100.0 + Math.sqrt(Math.PI * 16.0 * Math.PI * 100.0))
        assertEquals(expected, profile.totalVolumeMl(), 1e-6)
    }

    @Test
    fun coneFromPointAtBottom() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(5.0, CircleShape(0.0)),
                CrossSection(10.0, CircleShape(10.0)),
            ),
            totalHeightCm = 15.0,
        )
        val areaTop = Math.PI * 25.0
        val expected = 5.0 * areaTop / 3.0 + 5.0 * areaTop
        assertEquals(expected, profile.totalVolumeMl(), 1e-6)
    }

    @Test
    fun dissimilarShapesFallBackToTrapezoid() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(6.0)),
                CrossSection(10.0, RectangleShape(18.0, 12.0)),
            ),
            totalHeightCm = 10.0,
        )
        val areaCircle = Math.PI * 9.0
        val areaRect = 18.0 * 12.0
        val expected = 10.0 * (areaCircle + areaRect) / 2.0
        assertEquals(expected, profile.totalVolumeMl(), 1e-6)
    }

    @Test
    fun volumeUpToStopsAtRequestedHeight() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(6.0)),
                CrossSection(6.0, CircleShape(8.0)),
                CrossSection(12.0, CircleShape(10.0)),
            ),
            totalHeightCm = 12.0,
        )
        val area6 = Math.PI * 9.0
        val area8 = Math.PI * 16.0
        val expected = (6.0 / 3.0) * (area6 + area8 + Math.sqrt(area6 * area8))
        assertEquals(expected, profile.volumeUpTo(6.0), 1e-6)
    }

    @Test
    fun areaAtUsesQuadraticInterpolationForSimilarShapes() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(6.0)),
                CrossSection(6.0, CircleShape(10.0)),
            ),
            totalHeightCm = 10.0,
        )
        val area6 = Math.PI * 9.0
        val area10 = Math.PI * 25.0
        assertEquals(area6, profile.areaAt(-10.0), 1e-6)
        assertEquals(Math.PI * 16.0, profile.areaAt(3.0), 1e-6)
        assertEquals(area10, profile.areaAt(999.0), 1e-6)
    }

    @Test
    fun rectangleShapeAreaAndPerimeter() {
        val shape = RectangleShape(18.0, 12.0)
        assertEquals(18.0 * 12.0, shape.area, 1e-6)
        assertEquals(2.0 * (18.0 + 12.0), shape.perimeter!!, 1e-6)
    }

    @Test
    fun constructionRequiresAtLeastOnePoint() {
        try {
            CrossSectionProfile(points = emptyList(), totalHeightCm = 10.0)
            fail("Expected empty profile to be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun constructionRejectsNonPositiveTotalHeight() {
        try {
            CrossSectionProfile(points = listOf(CrossSection(0.0, CircleShape(5.0))), totalHeightCm = 0.0)
            fail("Expected non-positive total height to be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun constructionRejectsUnsortedOrDuplicateHeights() {
        val bottom = CrossSection(0.0, CircleShape(5.0))
        val middle = CrossSection(5.0, CircleShape(6.0))
        listOf(
            listOf(middle, bottom),
            listOf(bottom, middle, middle),
        ).forEach { points ->
            try {
                CrossSectionProfile(points = points, totalHeightCm = 10.0)
                fail("Expected invalid ordering to be rejected")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun constructionRejectsHeightOutsideContainer() {
        try {
            CrossSectionProfile(
                points = listOf(CrossSection(12.0, CircleShape(5.0))),
                totalHeightCm = 10.0,
            )
            fail("Expected out-of-range height to be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun topPointBelowTotalHeightIsAllowed() {
        val profile = CrossSectionProfile(
            points = listOf(CrossSection(0.0, CircleShape(6.0)), CrossSection(8.0, CircleShape(9.0))),
            totalHeightCm = 10.0,
        )
        assertEquals(10.0, profile.totalHeightCm, 1e-6)
    }

    @Test
    fun zeroAreaAtMiddleHeightIsValidationError() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(6.0)),
                CrossSection(5.0, CircleShape(0.0)),
                CrossSection(10.0, CircleShape(8.0)),
            ),
            totalHeightCm = 10.0,
        )
        assertTrue(profile.validate().any { it is CrossSectionValidation.ZeroAreaAtMiddle })
    }

    @Test
    fun zeroAreaAtBottomIsAllowed() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(0.0)),
                CrossSection(8.0, CircleShape(8.0)),
            ),
            totalHeightCm = 10.0,
        )
        assertTrue(profile.validate().none { it is CrossSectionValidation.ZeroAreaAtMiddle })
    }

    @Test
    fun decreasingAreaRaisesSingleWarning() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(10.0)),
                CrossSection(4.0, CircleShape(8.0)),
                CrossSection(8.0, CircleShape(6.0)),
            ),
            totalHeightCm = 10.0,
        )
        val warnings = profile.validate().filterIsInstance<CrossSectionValidation.AreaDecreasing>()
        assertEquals(1, warnings.size)
    }

    @Test
    fun validMonotonicProfileHasNoWarnings() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(6.0)),
                CrossSection(5.0, CircleShape(8.0)),
                CrossSection(10.0, CircleShape(10.0)),
            ),
            totalHeightCm = 10.0,
        )
        assertTrue(profile.validate().isEmpty())
    }

    @Test
    fun lengthUnitIdDefaultsToCmAndRoundTrips() {
        assertEquals("cm", CrossSectionProfile(points = listOf(CrossSection(0.0, CircleShape(5.0))), totalHeightCm = 10.0).lengthUnitId)
        val profile = CrossSectionProfile(
            points = listOf(CrossSection(0.0, CircleShape(5.0))),
            totalHeightCm = 10.0,
            lengthUnitId = "mm",
        )
        assertEquals("mm", profile.toDto().toDomain().lengthUnitId)
    }

    @Test
    fun dtoRoundTripPreservesDomain() {
        val profile = CrossSectionProfile(
            points = listOf(
                CrossSection(0.0, CircleShape(6.0)),
                CrossSection(6.0, RectangleShape(18.0, 12.0)),
                CrossSection(12.0, IrregularShape(28.3, 18.5)),
            ),
            totalHeightCm = 12.0,
            lengthUnitId = "mm",
        )
        assertEquals(profile, profile.toDto().toDomain())
    }

    @Test
    fun profileDtoSerializesToStableJson() {
        val dto = CrossSectionProfileDto(
            totalHeightCm = 12.0,
            lengthUnitId = "cm",
            points = listOf(
                CrossSectionDto(0.0, CrossSectionShapeDto(kind = "circle", diameterCm = 6.0)),
                CrossSectionDto(11.0, CrossSectionShapeDto(kind = "rectangle", lengthCm = 18.0, widthCm = 12.0)),
            ),
        )
        val raw = json.encodeToString(dto)
        assertEquals(dto, json.decodeFromString(CrossSectionProfileDto.serializer(), raw))
    }

    @Test
    fun migrationFillsBlankKindAsCircle() {
        val legacy = CrossSectionProfileDto(
            totalHeightCm = 10.0,
            points = listOf(
                CrossSectionDto(0.0, CrossSectionShapeDto(kind = "", diameterCm = 6.0)),
                CrossSectionDto(9.0, CrossSectionShapeDto(diameterCm = 8.0)),
            ),
        )
        val migrated = migrateCrossSectionProfileDto(legacy, fromVersion = 1)
        assertTrue(migrated.points.all { it.shape.kind == "circle" })
        assertEquals(migrated, migrateCrossSectionProfileDto(migrated, fromVersion = 1))
    }

    @Test
    fun migrationLeavesKnownKindsUntouched() {
        val dto = CrossSectionProfileDto(
            totalHeightCm = 10.0,
            lengthUnitId = "mm",
            points = listOf(
                CrossSectionDto(0.0, CrossSectionShapeDto(kind = "square", sideLengthCm = 5.0)),
                CrossSectionDto(9.0, CrossSectionShapeDto(kind = "irregular", customAreaCm2 = 20.0)),
            ),
        )
        assertEquals(dto, migrateCrossSectionProfileDto(dto, fromVersion = 1))
    }
}
