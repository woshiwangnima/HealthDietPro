package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal data class ServingContainer(
    val id: String,
    val category: String,
    val size: String,
    val capacityMl: Double,
    val defaultFillRatio: Double,
    val labels: Map<String, String>,
) {
    fun displayLabel(language: String): String =
        labels[language] ?: labels["en"] ?: labels.values.firstOrNull().orEmpty()

    fun grams(densityGramsPerMl: Double, fillRatio: Double = defaultFillRatio): Double =
        capacityMl * fillRatio * densityGramsPerMl
}

@Serializable
private data class ServingContainerAsset(val containers: List<ServingContainerDto> = emptyList())

@Serializable
private data class ServingContainerDto(
    val id: String,
    val category: String = "",
    val size: String = "",
    val capacityMl: Double = 0.0,
    val defaultFillRatio: Double = 1.0,
    val labels: Map<String, String> = emptyMap(),
) {
    fun toDomain() = ServingContainer(id, category, size, capacityMl, defaultFillRatio, labels)
}

internal class ServingContainerRepository private constructor(private val source: () -> String) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<ServingContainer>? = null
    private var index: Map<String, ServingContainer>? = null

    fun containers(): List<ServingContainer> = cache
        ?: json.decodeFromString<ServingContainerAsset>(source()).containers
            .map { it.toDomain() }
            .also { cache = it }

    fun byId(): Map<String, ServingContainer> = index ?: containers().associateBy { it.id }.also { index = it }

    fun find(id: String): ServingContainer? = byId()[id]

    companion object {
        fun fromContext(context: Context) = ServingContainerRepository {
            context.assets.open("serving_containers.json").bufferedReader().use { it.readText() }
        }
        fun fromAsset(path: String) = ServingContainerRepository { java.io.File(path).readText() }
    }
}
