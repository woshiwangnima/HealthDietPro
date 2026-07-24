package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One nutrient definition from assets/DRIs/nutrients_meta.json. */
internal data class NutrientMeta(
    val code: String,
    val category: String,
    val needType: String,
    val baseUnit: String,
    val names: Map<String, String>,
) {
    fun displayName(language: String): String = names[language] ?: names["en"] ?: code

    /** Nutrient codes required for every food (core macros). */
    val isRequired: Boolean get() = code in REQUIRED_CODES

    companion object {
        val REQUIRED_CODES = setOf("ENERGY", "PROTEIN", "FAT", "CHO")
        val unitCategoryFor: (String) -> String = { code -> if (code == "ENERGY") "energy" else "weight" }
    }
}

@Serializable
private data class NutrientMetaDto(
    val code: String,
    val category: String = "",
    val needType: String = "",
    val baseUnit: String = "g",
    val i18n: Map<String, NutrientI18nDto> = emptyMap(),
) {
    fun toDomain() = NutrientMeta(
        code = code,
        category = category,
        needType = needType,
        baseUnit = baseUnit,
        names = i18n.mapValues { it.value.name },
    )
}

@Serializable
private data class NutrientI18nDto(val name: String = "", val description: String = "")

internal class NutrientMetaRepository private constructor(private val source: () -> String) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<NutrientMeta>? = null

    fun nutrients(): List<NutrientMeta> = cache
        ?: json.decodeFromString<List<NutrientMetaDto>>(source()).map { it.toDomain() }.also { cache = it }

    companion object {
        fun fromContext(context: Context) = NutrientMetaRepository {
            context.assets.open("DRIs/nutrients_meta.json").bufferedReader().use { it.readText() }
        }
        fun fromAsset(path: String) = NutrientMetaRepository { java.io.File(path).readText() }
    }
}
