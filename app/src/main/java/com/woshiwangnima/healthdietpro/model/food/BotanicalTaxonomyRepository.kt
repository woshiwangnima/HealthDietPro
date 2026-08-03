package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal data class BotanicalTaxonomyLabels(
    val families: Map<String, Map<String, String>>,
    val genera: Map<String, Map<String, String>>,
) {
    fun familyName(id: String, language: String): String = families[id].localized(language, id)
    fun genusName(id: String, language: String): String = genera[id].localized(language, id)
}

private fun Map<String, String>?.localized(language: String, fallback: String): String =
    this?.get(language) ?: this?.get("en") ?: fallback

@Serializable
private data class BotanicalTaxonomyAsset(
    val families: Map<String, Map<String, String>> = emptyMap(),
    val genera: Map<String, Map<String, String>> = emptyMap(),
)

internal class BotanicalTaxonomyRepository private constructor(private val source: () -> String) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: BotanicalTaxonomyLabels? = null

    fun labels(): BotanicalTaxonomyLabels = cache ?: json.decodeFromString<BotanicalTaxonomyAsset>(source())
        .let { BotanicalTaxonomyLabels(it.families, it.genera) }
        .also { cache = it }

    companion object {
        fun fromContext(context: Context) = BotanicalTaxonomyRepository {
            context.assets.open("botanical_taxonomy.json").bufferedReader().use { it.readText() }
        }

        fun fromAsset(path: String) = BotanicalTaxonomyRepository { java.io.File(path).readText() }
    }
}
