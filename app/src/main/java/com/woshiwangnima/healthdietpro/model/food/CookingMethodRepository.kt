package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal data class CookingMethod(
    val id: String,
    val labels: Map<String, String>,
    val yieldFactor: Double,
    val nutrientRetention: Map<String, Double> = emptyMap(),
    val addedPer100gRaw: Map<String, FoodAmount> = emptyMap(),
) {
    fun displayLabel(language: String): String =
        labels[language] ?: labels["en"] ?: labels.values.firstOrNull().orEmpty()

    fun retentionFor(code: String): Double = nutrientRetention[code] ?: 1.0
}

@Serializable
private data class CookingMethodAsset(val methods: List<CookingMethodDto> = emptyList())

@Serializable
private data class CookingMethodDto(
    val id: String,
    val labels: Map<String, String> = emptyMap(),
    val yieldFactor: Double = 1.0,
    val nutrientRetention: Map<String, Double> = emptyMap(),
    val addedPer100gRaw: Map<String, FoodAmountDto> = emptyMap(),
) {
    fun toDomain() = CookingMethod(
        id = id,
        labels = labels,
        yieldFactor = yieldFactor,
        nutrientRetention = nutrientRetention,
        addedPer100gRaw = addedPer100gRaw.mapValues { it.value.toDomain() },
    )
}

internal class CookingMethodRepository private constructor(private val source: () -> String) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<CookingMethod>? = null
    private var index: Map<String, CookingMethod>? = null

    fun methods(): List<CookingMethod> = cache
        ?: json.decodeFromString<CookingMethodAsset>(source()).methods
            .map { it.toDomain() }
            .also { cache = it }

    fun byId(): Map<String, CookingMethod> = index ?: methods().associateBy { it.id }.also { index = it }

    fun find(id: String): CookingMethod? = byId()[id]

    companion object {
        fun fromContext(context: Context) = CookingMethodRepository {
            context.assets.open("cooking_methods.json").bufferedReader().use { it.readText() }
        }
        fun fromAsset(path: String) = CookingMethodRepository { java.io.File(path).readText() }
    }
}
