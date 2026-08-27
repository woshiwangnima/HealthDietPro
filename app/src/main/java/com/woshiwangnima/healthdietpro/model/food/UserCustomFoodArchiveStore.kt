package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class UserCustomFoodArchive(
    val foods: List<FoodDto> = emptyList(),
    val tags: List<UserFoodTag> = emptyList(),
)

internal class UserCustomFoodArchiveStore(
    private val context: Context,
    private val userId: String,
) {
    fun load(): UserCustomFoodArchive = synchronized(lock) {
        read() ?: UserCustomFoodArchive()
    }

    fun saveFoods(foods: List<FoodDto>) = synchronized(lock) {
        write(loadUnlocked().copy(foods = foods))
    }

    fun saveTags(tags: List<UserFoodTag>) = synchronized(lock) {
        write(loadUnlocked().copy(tags = tags))
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validate(json.decodeDomain(raw, DOMAIN_ID, UserCustomFoodArchive.serializer())) }
    }

    internal fun replaceJson(raw: String) = synchronized(lock) {
        write(json.decodeDomain(raw, DOMAIN_ID, UserCustomFoodArchive.serializer()))
    }

    private fun loadUnlocked(): UserCustomFoodArchive = read() ?: UserCustomFoodArchive()

    private fun read(): UserCustomFoodArchive? = runCatching {
        val target = file()
        if (!target.isFile) {
            null
        } else {
            val raw = target.readText(Charsets.UTF_8)
            json.decodeDomain(raw, DOMAIN_ID, UserCustomFoodArchive.serializer()).also(::validate)
        }
    }.getOrNull()

    private fun write(archive: UserCustomFoodArchive) {
        validate(archive)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(json.encodeDomain(context, DOMAIN_ID, archive, UserCustomFoodArchive.serializer()), Charsets.UTF_8)
        check(temporary.renameTo(target)) { "Unable to replace custom foods archive" }
        writeUserArchiveManifest(context, userId)
        com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs.noteUserActivity(context, userId)
    }

    private fun validate(archive: UserCustomFoodArchive) {
        val foodIds = archive.foods.map(FoodDto::id)
        require(foodIds.all { it.isNotBlank() }) { "Custom food id is blank" }
        require(foodIds.all { it.startsWith(UserCustomFoodRepository.CUSTOM_ID_PREFIX) }) {
            "Custom food id must start with ${UserCustomFoodRepository.CUSTOM_ID_PREFIX}"
        }
        require(foodIds.distinct().size == foodIds.size) { "Duplicate custom food id" }

        val tagIds = archive.tags.map(UserFoodTag::id)
        require(tagIds.all { it.isNotBlank() }) { "User food tag id is blank" }
        require(tagIds.distinct().size == tagIds.size) { "Duplicate user food tag id" }
        require(archive.foods.all { food ->
            val tags = food.categoryTags
            FoodCategories.normalizeTags(tags) == tags
        }) { "Custom food category tags must contain leaf tags only" }

        val allowedNutrients = NutrientMetaRepository.fromContext(context).nutrients()
            .mapTo(mutableSetOf(), NutrientMeta::code)
        archive.foods.forEach { food ->
            val codes = buildSet {
                addAll(food.nutrients.keys)
                food.nutritionTables.values.forEach { addAll(it.nutrients.keys) }
                food.derivedFrom?.nutrientOverrides?.keys?.let(::addAll)
            }
            require(codes.all(allowedNutrients::contains)) {
                "Custom food contains an unknown nutrient: ${food.id}"
            }
            val references = buildList {
                food.derivedFrom?.ingredientId?.let(::add)
                food.components.forEach { add(it.foodId) }
            }
            require(references.all { it in builtInFoodIds }) {
                "Custom food may reference built-in foods only: ${food.id}"
            }
        }
    }

    private val builtInFoodIds: Set<String> by lazy {
        runCatching {
            val raw = context.assets.open("food_catalog/manifest.json").bufferedReader().use { it.readText() }
            val manifest = json.decodeFromString<CustomFoodCatalogManifest>(raw)
            manifest.records.keys
        }.getOrDefault(emptySet())
    }

    private fun file(): File = File(context.filesDir, "user_archives/${safeId(userId)}/custom_foods.json")

    private fun safeId(value: String): String = value.replace(Regex("[^A-Za-z0-9_-]"), "_")

    companion object {
        private const val DOMAIN_ID = "custom_foods"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }

        fun forUser(context: Context, userId: String) =
            UserCustomFoodArchiveStore(context.applicationContext, userId)
    }
}

@Serializable
private data class CustomFoodCatalogManifest(val records: Map<String, String> = emptyMap())
