package com.woshiwangnima.healthdietpro.model.container

import kotlinx.serialization.Serializable

/** Household container category. Mirrors `serving_containers.json` categories plus bottle/custom. */
@Serializable
internal enum class ContainerCategory {
    CUP,
    BOWL,
    PLATE,
    SPOON,
    BOTTLE,
    CUSTOM,
}

/** How a container's capacity is obtained. */
@Serializable
internal enum class ContainerCapacityMode {
    /** User-entered capacity value + unit, assumed to grow linearly with fill level. */
    MANUAL,

    /** Capacity is derived from the 2D cross-section profile (volume at full height). */
    CROSS_SECTION,
}

/**
 * A user-recorded household container (记容器).
 *
 * 长度/体积/质量不变量与项目一致：
 * - [capacityMl] 与截面系统容积以基准 ml 存储；
 * - [emptyMassGrams] 以基准 g 存储（可选）；
 * - UI 边界经 `UnitConverter.toBase / fromBase` 在体积/质量类目换算显示。
 *
 * [crossSections] 为可选 2D 水平截面定义（引用 [CrossSectionProfileDto]），
 * 仅供记录形状与校验容积，不强制填写。
 *
 * [capacityMode] 决定 [capacityMl] 的来源：`MANUAL` 为用户填写的容量值；
 * `CROSS_SECTION` 时 [capacityMl] 为截面系统在满高度处的容积。
 * [scenarioTags] 为可自定义的容器使用场景标签（如“家”“学校”），仅引用
 * [ContainerArchive.scenarioTags] 注册表中的标签。
 */
@Serializable
internal data class ContainerRecord(
    val id: String,
    val name: String,
    val category: ContainerCategory = ContainerCategory.CUSTOM,
    val capacityMode: ContainerCapacityMode = ContainerCapacityMode.MANUAL,
    val capacityMl: Double,
    val emptyMassGrams: Double? = null,
    val note: String = "",
    val imagePaths: List<String> = emptyList(),
    val crossSections: CrossSectionProfileDto? = null,
    val scenarioTags: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

/**
 * Capacity at a given fill-height percentage (0..100).
 *
 * - `MANUAL` mode grows linearly: `capacityMl * percent / 100`.
 * - `CROSS_SECTION` mode integrates the cross-section profile up to `percent` of its total
 *   height (returns `null` when the profile is missing or invalid).
 */
internal fun ContainerRecord.capacityMlAtHeightPercent(heightPercent: Double): Double? {
    val fraction = heightPercent.coerceIn(0.0, 100.0) / 100.0
    return when (capacityMode) {
        ContainerCapacityMode.MANUAL -> capacityMl * fraction
        ContainerCapacityMode.CROSS_SECTION -> {
            val profile = crossSections?.toDomain() ?: return null
            runCatching { profile.volumeUpTo(profile.totalHeightCm * fraction) }.getOrNull()
        }
    }
}

@Serializable
internal data class ContainerArchive(
    val schemaVersion: Int = CONTAINER_ARCHIVE_SCHEMA_VERSION,
    val scenarioTags: List<String> = emptyList(),
    val containers: List<ContainerRecord> = emptyList(),
)

internal const val CONTAINER_ARCHIVE_SCHEMA_VERSION = 2

/**
 * Idempotent forward migration for the container archive.
 *
 * - v1 → v2：引入使用场景标签注册表 [ContainerArchive.scenarioTags] 与
 *   [ContainerRecord.scenarioTags]/[ContainerRecord.capacityMode]；容器标签只保留注册表中的项。
 * - 后续版本在此链式追加。
 */
internal fun migrateContainerArchive(archive: ContainerArchive): ContainerArchive {
    val scenarioTagRegistry = archive.scenarioTags.map(String::trim).filter(String::isNotBlank).distinct()
    val containers = archive.containers.map { record ->
        record.copy(
            name = record.name.trim(),
            note = record.note.trim(),
            imagePaths = record.imagePaths.distinct(),
            emptyMassGrams = record.emptyMassGrams?.takeIf { it > 0.0 },
            scenarioTags = record.scenarioTags.map(String::trim).filter(String::isNotBlank).distinct().filter { it in scenarioTagRegistry },
        )
    }
    return archive.copy(schemaVersion = CONTAINER_ARCHIVE_SCHEMA_VERSION, scenarioTags = scenarioTagRegistry, containers = containers)
}

/** Pure archive validation (no Android dependency, JVM-testable). */
internal fun validateContainerArchive(archive: ContainerArchive) {
    require(archive.schemaVersion == CONTAINER_ARCHIVE_SCHEMA_VERSION) { "Unsupported container archive schema" }
    require(archive.scenarioTags.all(String::isNotBlank) && archive.scenarioTags.distinct().size == archive.scenarioTags.size) { "Invalid scenario tag registry" }
    val ids = archive.containers.map(ContainerRecord::id)
    require(ids.all(String::isNotBlank) && ids.distinct().size == ids.size) { "Invalid container record ids" }
    require(archive.containers.all { it.capacityMl > 0.0 }) { "Invalid container record" }
    require(archive.containers.all { record ->
        record.emptyMassGrams == null || record.emptyMassGrams > 0.0
    }) { "Invalid container empty mass" }
    require(archive.containers.all { record ->
        record.imagePaths.all(String::isNotBlank) && record.imagePaths.distinct().size == record.imagePaths.size
    }) { "Invalid container image paths" }
    require(archive.containers.all { record ->
        record.scenarioTags.all(String::isNotBlank) &&
            record.scenarioTags.distinct().size == record.scenarioTags.size &&
            record.scenarioTags.all { it in archive.scenarioTags }
    }) { "Invalid container scenario tags" }
    require(archive.containers.all { record ->
        record.capacityMode != ContainerCapacityMode.CROSS_SECTION || record.crossSections != null
    }) { "Cross-section capacity requires a cross-section profile" }
}
