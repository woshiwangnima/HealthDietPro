package com.woshiwangnima.healthdietpro.model.container

/** Schema version of the embedded cross-section profile. Host archives own the version field. */
internal const val CROSS_SECTION_SCHEMA_VERSION = 1

/**
 * Idempotent migration chain for [CrossSectionProfileDto].
 *
 * Called by the host archive's read() after decodeDomain and before toDomain().
 * Runs version-by-version code blocks; unknown or blank kinds fall back to "circle"
 * and blank length units fall back to "cm".
 */
internal fun migrateCrossSectionProfileDto(
    dto: CrossSectionProfileDto,
    fromVersion: Int,
): CrossSectionProfileDto {
    var current = dto
    if (fromVersion < 2) {
        current = current.copy(
            points = current.points.map { point ->
                if (point.shape.kind.isBlank()) {
                    point.copy(shape = point.shape.copy(kind = "circle"))
                } else point
            },
        )
    }
    // if (fromVersion < 3) { ... }
    return current
}
