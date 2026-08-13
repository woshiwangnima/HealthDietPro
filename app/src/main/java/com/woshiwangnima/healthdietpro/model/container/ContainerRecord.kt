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
 */
@Serializable
internal data class ContainerRecord(
    val id: String,
    val name: String,
    val category: ContainerCategory = ContainerCategory.CUSTOM,
    val capacityMl: Double,
    val emptyMassGrams: Double? = null,
    val note: String = "",
    val imagePaths: List<String> = emptyList(),
    val crossSections: CrossSectionProfileDto? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

@Serializable
internal data class ContainerArchive(
    val schemaVersion: Int = CONTAINER_ARCHIVE_SCHEMA_VERSION,
    val containers: List<ContainerRecord> = emptyList(),
)

internal const val CONTAINER_ARCHIVE_SCHEMA_VERSION = 1

/**
 * Idempotent forward migration for the container archive.
 * 当前为 v1 首版，仅做数据规整；后续版本在此链式追加。
 */
internal fun migrateContainerArchive(archive: ContainerArchive): ContainerArchive {
    val containers = archive.containers.map { record ->
        record.copy(
            name = record.name.trim(),
            note = record.note.trim(),
            imagePaths = record.imagePaths.distinct(),
            emptyMassGrams = record.emptyMassGrams?.takeIf { it > 0.0 },
        )
    }
    return archive.copy(schemaVersion = CONTAINER_ARCHIVE_SCHEMA_VERSION, containers = containers)
}

/** Pure archive validation (no Android dependency, JVM-testable). */
internal fun validateContainerArchive(archive: ContainerArchive) {
    require(archive.schemaVersion == CONTAINER_ARCHIVE_SCHEMA_VERSION) { "Unsupported container archive schema" }
    val ids = archive.containers.map(ContainerRecord::id)
    require(ids.all(String::isNotBlank) && ids.distinct().size == ids.size) { "Invalid container record ids" }
    require(archive.containers.all { it.name.isNotBlank() && it.capacityMl > 0.0 }) { "Invalid container record" }
    require(archive.containers.all { record ->
        record.emptyMassGrams == null || record.emptyMassGrams > 0.0
    }) { "Invalid container empty mass" }
    require(archive.containers.all { record ->
        record.imagePaths.all(String::isNotBlank) && record.imagePaths.distinct().size == record.imagePaths.size
    }) { "Invalid container image paths" }
}
