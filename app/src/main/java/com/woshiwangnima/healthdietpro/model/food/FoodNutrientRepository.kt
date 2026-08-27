package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.json.Json

internal class FoodNutrientRepository private constructor(
    private val source: () -> List<FoodDto>,
    private val findSource: ((String) -> FoodDto?)? = null,
    private val searchSource: (() -> Map<String, List<String>>)? = null,
    private val categorySource: (() -> Map<String, List<String>>)? = null,
    private val relatedDishSource: (() -> Map<String, List<String>>)? = null,
    private val nutrientCodes: () -> Set<String> = { emptySet() },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<FoodItem>? = null
    private var index: Map<String, FoodItem>? = null
    private val recordCache = mutableMapOf<String, FoodItem>()
    private var searchIndexCache: Map<String, List<String>>? = null
    private var categoryIndexCache: Map<String, List<String>>? = null
    private var relatedDishIndexCache: Map<String, List<String>>? = null

    fun foods(): List<FoodItem> = cache ?: source()
        .map { it.toDomain() }
        .also { foods ->
            val codes = nutrientCodes()
            if (codes.isNotEmpty()) require(foods.all { food ->
                val tables = when (food) {
                    is Ingredient -> food.nutritionTables.values
                    else -> emptyList()
                }
                tables.all { table -> table.nutrients.keys.all(codes::contains) }
            }) { "Food nutrient code is absent from nutrients_meta.json" }
        }
        .also { cache = it }

    fun byId(): Map<String, FoodItem> = index ?: foods().associateBy { it.id }.also { index = it }

    fun find(id: String): FoodItem? = recordCache[id] ?: findSource?.invoke(id)?.toDomain()?.also {
        recordCache[id] = it
    } ?: byId()[id]?.also { recordCache[id] = it }

    fun searchIds(query: String): List<String> {
        val token = query.lowercase().filterNot(Char::isWhitespace)
        if (token.isBlank()) return emptyList()
        return searchIndex().asSequence()
            .filter { it.key.contains(token) }
            .flatMap { it.value.asSequence() }
            .distinct()
            .toList()
    }

    fun categoryIds(tag: String): List<String> = categoryIndex()
        .orEmpty()
        .asSequence()
        .filter { it.key == tag || it.key.startsWith("$tag.") }
        .flatMap { it.value.asSequence() }
        .distinct()
        .toList()

    fun relatedDishIds(foodId: String): List<String> = relatedDishIndex()[foodId].orEmpty()

    fun warmIndexes() {
        searchIndex()
        categoryIndex()
        relatedDishIndex()
    }

    private fun searchIndex(): Map<String, List<String>> = searchIndexCache ?: searchSource?.invoke().orEmpty().also {
        searchIndexCache = it
    }

    private fun categoryIndex(): Map<String, List<String>> = categoryIndexCache ?: categorySource?.invoke().orEmpty().also {
        categoryIndexCache = it
    }

    private fun relatedDishIndex(): Map<String, List<String>> = relatedDishIndexCache ?: relatedDishSource?.invoke().orEmpty().also {
        relatedDishIndexCache = it
    }

    fun categoryRoots(): List<FoodCategory> = FoodCategories.roots

    fun categoryChildren(parentTag: String): List<FoodCategory> = FoodCategories.childrenOf(parentTag)

    fun categoryTree(): List<FoodCategory> = FoodCategories.roots + FoodCategories.children

    fun categoryDisplayPath(tag: String): List<Int> = FoodCategories.displayTagPath(tag)

    fun hasCategory(tags: List<String>, ancestor: String): Boolean = FoodCategories.hasTagWithin(tags, ancestor)

    fun hasAnyCategory(tags: List<String>, ancestors: Set<String>): Boolean =
        FoodCategories.hasTagWithinAny(tags, ancestors)

    fun retainCategoryChildren(selected: Set<String>, roots: Set<String>): Set<String> =
        FoodCategories.retainChildrenForRoots(selected, roots)

    fun isSeasoning(food: FoodItem): Boolean =
        hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), "food.seasoning")

    fun isAuxiliary(food: FoodItem): Boolean =
        hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), "food.seasoning") ||
            hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), "food.oil")

    fun foodsWithin(categoryTag: String): List<FoodItem> = foods().filter { food ->
        hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), categoryTag)
    }

    companion object {
        private const val CATALOG_ROOT = "food_catalog"

        fun fromContext(context: Context): FoodNutrientRepository {
            val appContext = context.applicationContext
            val manifest = lazy { loadManifest(appContext) }
            return FoodNutrientRepository(
                source = { manifest.value.records.values.map { loadRecord(appContext, it) } },
                findSource = { id -> manifest.value.records[id]?.let { loadRecord(appContext, it) } },
                searchSource = { loadIndex(appContext, manifest.value.indexes.search) },
                categorySource = { loadIndex(appContext, manifest.value.indexes.categories) },
                relatedDishSource = { loadIndex(appContext, manifest.value.indexes.relatedDishes) },
                nutrientCodes = {
                    NutrientMetaRepository.fromContext(appContext)
                        .nutrients().mapTo(mutableSetOf(), NutrientMeta::code)
                },
            )
        }

        fun fromAsset(path: String) = FoodNutrientRepository(
            source = { json.decodeFromString<FoodAsset>(java.io.File(path).readText()).foods },
        )

        fun fromCatalogAsset(path: String): FoodNutrientRepository {
            val root = java.io.File(path)
            val manifest = lazy {
                json.decodeFromString<FoodCatalogManifest>(java.io.File(root, "manifest.json").readText())
            }
            return FoodNutrientRepository(
                source = { manifest.value.records.values.map { recordPath -> json.decodeFromString<FoodDto>(java.io.File(root, recordPath).readText()) } },
                findSource = { id -> manifest.value.records[id]?.let { recordPath -> json.decodeFromString<FoodDto>(java.io.File(root, recordPath).readText()) } },
                searchSource = { json.decodeFromString(java.io.File(root, manifest.value.indexes.search).readText()) },
                categorySource = { json.decodeFromString(java.io.File(root, manifest.value.indexes.categories).readText()) },
                relatedDishSource = { json.decodeFromString(java.io.File(root, manifest.value.indexes.relatedDishes).readText()) },
            )
        }

        private fun loadManifest(context: Context): FoodCatalogManifest =
            context.assets.open("$CATALOG_ROOT/manifest.json").bufferedReader().use {
                json.decodeFromString(it.readText())
            }

        private fun loadIndex(context: Context, path: String): Map<String, List<String>> =
            context.assets.open("$CATALOG_ROOT/$path").bufferedReader().use {
                json.decodeFromString(it.readText())
            }

        private fun loadRecord(context: Context, path: String): FoodDto =
            context.assets.open("$CATALOG_ROOT/$path").bufferedReader().use {
                json.decodeFromString(it.readText())
            }

        private val json = Json { ignoreUnknownKeys = true }
    }
}

@kotlinx.serialization.Serializable
private data class FoodCatalogManifest(
    val records: Map<String, String> = emptyMap(),
    val indexes: FoodCatalogIndexes = FoodCatalogIndexes(),
)

@kotlinx.serialization.Serializable
private data class FoodCatalogIndexes(
    val search: String = "indexes/search.json",
    val categories: String = "indexes/categories.json",
    @kotlinx.serialization.SerialName("related_dishes")
    val relatedDishes: String = "indexes/related_dishes.json",
)
