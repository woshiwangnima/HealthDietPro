package com.woshiwangnima.healthdietpro.model.container

import kotlinx.serialization.Serializable

/**
 * Serializable representation of a cross-section shape.
 *
 * All linear dimensions are stored in the base length unit (cm); irregular area in cm².
 * Domain types ([CrossSectionShape]) are not @Serializable; all JSON I/O goes through this
 * DTO boundary. `kind` defaults to "circle" for legacy data compatibility.
 */
@Serializable
internal data class CrossSectionShapeDto(
    val kind: String = "circle",
    val diameterCm: Double? = null,
    val sideLengthCm: Double? = null,
    val lengthCm: Double? = null,
    val widthCm: Double? = null,
    val customAreaCm2: Double? = null,
    val customPerimeterCm: Double? = null,
)

@Serializable
internal data class CrossSectionDto(
    val heightCm: Double,
    val shape: CrossSectionShapeDto,
)

@Serializable
internal data class CrossSectionProfileDto(
    val totalHeightCm: Double = 0.0,
    val lengthUnitId: String = "cm",
    val points: List<CrossSectionDto> = emptyList(),
)

internal fun CrossSectionShapeDto.toDomain(): CrossSectionShape = when (kind) {
    "circle" -> CircleShape(
        diameterCm = requireNotNull(diameterCm) { "Circle requires diameterCm" },
    )
    "square" -> SquareShape(
        sideLengthCm = requireNotNull(sideLengthCm) { "Square requires sideLengthCm" },
    )
    "rectangle" -> RectangleShape(
        lengthCm = requireNotNull(lengthCm) { "Rectangle requires lengthCm" },
        widthCm = requireNotNull(widthCm) { "Rectangle requires widthCm" },
    )
    "irregular" -> IrregularShape(
        area = requireNotNull(customAreaCm2) { "Irregular requires customAreaCm2" },
        perimeter = customPerimeterCm,
    )
    else -> error("Unknown shape kind: $kind")
}

internal fun CrossSectionShape.toDto(): CrossSectionShapeDto = when (this) {
    is CircleShape -> CrossSectionShapeDto(kind = "circle", diameterCm = diameterCm)
    is SquareShape -> CrossSectionShapeDto(kind = "square", sideLengthCm = sideLengthCm)
    is RectangleShape -> CrossSectionShapeDto(kind = "rectangle", lengthCm = lengthCm, widthCm = widthCm)
    is IrregularShape -> CrossSectionShapeDto(kind = "irregular", customAreaCm2 = area, customPerimeterCm = perimeter)
}

internal fun CrossSectionDto.toDomain() = CrossSection(
    heightCm = heightCm,
    shape = shape.toDomain(),
)

internal fun CrossSection.toDto() = CrossSectionDto(
    heightCm = heightCm,
    shape = shape.toDto(),
)

internal fun CrossSectionProfileDto.toDomain(): CrossSectionProfile =
    CrossSectionProfile(
        points = points.map { it.toDomain() },
        totalHeightCm = totalHeightCm,
        lengthUnitId = lengthUnitId.ifBlank { "cm" },
    )

internal fun CrossSectionProfile.toDto(): CrossSectionProfileDto =
    CrossSectionProfileDto(
        totalHeightCm = totalHeightCm,
        lengthUnitId = lengthUnitId,
        points = points.map { it.toDto() },
    )
