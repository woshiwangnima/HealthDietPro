package com.woshiwangnima.healthdietpro.ui.nutrition

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.HealthDietProApplication
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.BotanicalTaxonomyLabels
import com.woshiwangnima.healthdietpro.model.food.BotanicalTaxonomyRepository
import com.woshiwangnima.healthdietpro.model.food.CookingMethod
import com.woshiwangnima.healthdietpro.model.food.CookingMethodRepository
import com.woshiwangnima.healthdietpro.model.food.Dish
import com.woshiwangnima.healthdietpro.model.food.FoodDto
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.PreparedFood
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientRepository
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.NutrientMetaRepository
import com.woshiwangnima.healthdietpro.model.food.NutritionResolver
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.model.food.ServingContainer
import com.woshiwangnima.healthdietpro.model.food.ServingContainerRepository
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodRepository
import com.woshiwangnima.healthdietpro.model.food.UserFoodTag
import com.woshiwangnima.healthdietpro.model.food.UserFoodTagRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.prefs.UserItemCollectionRepository
import com.woshiwangnima.healthdietpro.model.prefs.deserializeSearchHistory
import com.woshiwangnima.healthdietpro.model.prefs.serializeSearchHistory
import com.woshiwangnima.healthdietpro.common.ui.FoodImageStore
import com.woshiwangnima.healthdietpro.model.archive.userArchiveDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.io.File

internal data class NutritionUiState(
    val foods: List<FoodItem> = emptyList(),
    val keyword: String = "",
    val searchHistory: List<String> = emptyList(),
    val recentFoodIds: List<String> = emptyList(),
    val favoriteFoodIds: List<String> = emptyList(),
    val selectedKind: FoodKind = FoodKind.INGREDIENT,
    val selectedRoots: Set<String> = emptySet(),
    val customOnly: Boolean = false,
    val selectedChildren: Set<String> = emptySet(),
    val selectedSystemTags: Set<String> = emptySet(),
    val userTags: List<UserFoodTag> = emptyList(),
    val selectedUserTags: Set<String> = emptySet(),
    val selectedFood: FoodItem? = null,
    val comparisonReturnTarget: NutritionDestination? = null,
    val editor: NutritionEditorState? = null,
)

/** Which custom editor is open, and the item being edited (null = create new). */
internal data class NutritionEditorState(
    val kind: FoodKind,
    val editingId: String? = null,
)

internal enum class NutritionDestination { Browse, FoodDetail, Editor }

internal class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as HealthDietProApplication).foodNutrientRepository
    private val cookingMethodRepository = CookingMethodRepository.fromContext(application)
    private val servingContainerRepository = ServingContainerRepository.fromContext(application)
    private val nutrientMetaRepository = NutrientMetaRepository.fromContext(application)
    private val botanicalTaxonomyRepository = BotanicalTaxonomyRepository.fromContext(application)
    private var tagRepository = UserFoodTagRepository.fromContext(application)
    private var customRepository = UserCustomFoodRepository.fromContext(application)
    private var foodCollections = UserItemCollectionRepository.fromContext(application, NUTRITION_FOOD_COLLECTIONS_KEY)
    val foodImages = FoodImageStore(
        context = application,
        cacheRegistry = (application as HealthDietProApplication).cacheRegistry,
    )
    private var userId = ProfilePrefs.getCurrentUserId(application)
    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()
    private var resolver: NutritionResolver = NutritionResolver(emptyMap(), emptyMap())
    private var foodsById: Map<String, FoodItem> = emptyMap()
    private var cookingMethodsById: Map<String, CookingMethod> = emptyMap()
    private var containers: List<ServingContainer> = emptyList()
    private var builtInFoods: List<FoodItem> = emptyList()
    private var nutrientMetas: List<NutrientMeta> = emptyList()
    private var botanicalTaxonomy: BotanicalTaxonomyLabels = BotanicalTaxonomyLabels(emptyMap(), emptyMap())
    init {
        val initialUserId = userId
        val initialTagRepository = tagRepository
        val initialCustomRepository = customRepository
        viewModelScope.launch {
            val foods = withContext(Dispatchers.IO) { repository.foods() }
            val methods = withContext(Dispatchers.IO) { cookingMethodRepository.byId() }
            val loadedContainers = withContext(Dispatchers.IO) { servingContainerRepository.containers() }
            val tags = withContext(Dispatchers.IO) { initialTagRepository.load() }
            val customs = withContext(Dispatchers.IO) { initialCustomRepository.load() }
            val metas = withContext(Dispatchers.IO) { nutrientMetaRepository.nutrients() }
            val taxonomy = withContext(Dispatchers.IO) { botanicalTaxonomyRepository.labels() }
            if (userId == initialUserId) {
                builtInFoods = foods
                cookingMethodsById = methods
                containers = loadedContainers
                nutrientMetas = metas
                botanicalTaxonomy = taxonomy
                rebuild(customs)
                val collections = loadFoodCollections()
                _state.value = _state.value.copy(
                    foods = foodsById.values.toList(),
                    userTags = tags,
                    searchHistory = loadSearchHistory(),
                    recentFoodIds = collections.recentIds,
                    favoriteFoodIds = collections.favoriteIds,
                )
            }
        }
    }

    /** Rebuild the merged food index + resolver from built-ins plus current custom foods. */
    private fun rebuild(customFoods: List<FoodItem>) {
        val merged = builtInFoods + customFoods
        foodsById = merged.associateBy { it.id }
        resolver = NutritionResolver(foodsById, cookingMethodsById)
    }

    fun resolvePer100g(food: FoodItem): ResolvedNutrition = resolver.resolvePer100g(food)
    fun cookingMethodFor(id: String): CookingMethod? = cookingMethodsById[id]
    fun cookingMethods(): List<CookingMethod> = cookingMethodsById.values.toList()
    fun foodById(id: String): FoodItem? = foodsById[id]
    fun availableContainers(): List<ServingContainer> = containers
    fun nutrientMetas(): List<NutrientMeta> = nutrientMetas
    fun botanicalTaxonomy(): BotanicalTaxonomyLabels = botanicalTaxonomy
    fun categoryRoots(): List<com.woshiwangnima.healthdietpro.model.food.FoodCategory> = repository.categoryRoots()
    fun categoryChildren(parentTag: String): List<com.woshiwangnima.healthdietpro.model.food.FoodCategory> = repository.categoryChildren(parentTag)
    fun categoryDisplayPath(tag: String): List<Int> = repository.categoryDisplayPath(tag)
    fun hasCategory(tags: List<String>, categoryTag: String): Boolean = repository.hasCategory(tags, categoryTag)

    /** Ingredients + prepared foods usable as dish components / derivation sources. */
    fun selectableIngredients(): List<Ingredient> = foodsById.values
        .filterIsInstance<Ingredient>()
    fun selectableComponents(): List<FoodItem> = foodsById.values
        .filterNot { it is Dish }

    /** Preview per-100g resolved nutrition for a not-yet-saved derived food. */
    fun previewDerived(ingredientId: String, cookingMethodId: String): ResolvedNutrition? {
        val ingredient = foodsById[ingredientId] ?: return null
        val method = cookingMethodsById[cookingMethodId] ?: return null
        val preview = PreparedFood(
            id = "preview",
            names = emptyMap(),
            categoryTags = emptyList(),
            derivedFrom = com.woshiwangnima.healthdietpro.model.food.FoodDerivation(ingredientId, method.id),
        )
        val temp = NutritionResolver(foodsById + (preview.id to preview), cookingMethodsById)
        return runCatching { temp.resolvePer100g(preview) }.getOrNull()
    }

    /** True when the food is categorized as a seasoning (调味品). */
    fun isSeasoning(food: FoodItem): Boolean = repository.isSeasoning(food)

    /** 辅料判定：调味品或油脂（其余为主料）。菜肴食材清单据此排序：主料在前，辅料在后。 */
    fun isAuxiliary(food: FoodItem): Boolean = repository.isAuxiliary(food)

    /** Every dish whose components reference [foodId] (for the "related dishes" section). */
    fun relatedDishes(foodId: String): List<Dish> = foodsById.values
        .filterIsInstance<Dish>()
        .filter { dish -> dish.components.any { it.foodId == foodId } }

    fun selectKind(kind: FoodKind) {
        if (_state.value.selectedKind == kind) return
        _state.value = _state.value.copy(
            selectedKind = kind,
            selectedRoots = emptySet(),
            selectedChildren = emptySet(),
            customOnly = false,
        )
    }
    fun setKeyword(value: String) { _state.value = _state.value.copy(keyword = value) }
    fun removeSearchHistory(value: String) {
        val history = _state.value.searchHistory - value
        _state.value = _state.value.copy(searchHistory = history)
        saveSearchHistory(history)
    }
    fun clearSearchHistory() {
        _state.value = _state.value.copy(searchHistory = emptyList())
        saveSearchHistory(emptyList())
    }
    fun removeRecentFood(id: String) {
        updateCollections(foodCollections.removeRecent(id, foodsById.keys))
    }
    fun clearRecentFoods() {
        updateCollections(foodCollections.clearRecents(foodsById.keys))
    }
    fun toggleFavorite(food: FoodItem) = updateCollections(foodCollections.toggleFavorite(food.id, foodsById.keys))
    fun isFavorite(id: String): Boolean = id in _state.value.favoriteFoodIds
    fun toggleRoot(tag: String) {
        val state = _state.value
        val selectedRoots = state.selectedRoots.toggle(tag)
        _state.value = state.copy(
            selectedRoots = selectedRoots,
            customOnly = false,
            selectedChildren = repository.retainCategoryChildren(state.selectedChildren, selectedRoots),
        )
    }
    fun toggleCustomOnly() {
        _state.value = _state.value.copy(
            selectedRoots = emptySet(),
            customOnly = !_state.value.customOnly,
            selectedChildren = emptySet(),
        )
    }
    fun toggleChild(tag: String) {
        val state = _state.value
        if (tag !in state.selectedRoots.flatMap(repository::categoryChildren).map { it.tag }) return
        _state.value = state.copy(selectedChildren = state.selectedChildren.toggle(tag))
    }
    fun toggleSystemTag(tag: String) {
        _state.value = _state.value.copy(
            selectedSystemTags = _state.value.selectedSystemTags.toggle(tag),
        )
    }
    fun toggleUserTag(tag: String) { _state.value = _state.value.let { it.copy(selectedUserTags = it.selectedUserTags.toggle(tag)) } }
    fun addUserTag(label: String) {
        if (label.isBlank()) return
        val tags = _state.value.userTags + UserFoodTag(UUID.randomUUID().toString(), label.trim())
        _state.value = _state.value.copy(userTags = tags)
        val targetRepository = tagRepository
        viewModelScope.launch(Dispatchers.IO) { targetRepository.save(tags) }
    }
    fun refreshUser() {
        val targetUserId = ProfilePrefs.getCurrentUserId(getApplication())
        userId = targetUserId
        tagRepository = UserFoodTagRepository.fromContext(getApplication())
        customRepository = UserCustomFoodRepository.fromContext(getApplication())
        foodCollections = UserItemCollectionRepository.fromContext(getApplication(), NUTRITION_FOOD_COLLECTIONS_KEY)
        val targetRepository = tagRepository
        val targetCustom = customRepository
        viewModelScope.launch {
            val tags = withContext(Dispatchers.IO) { targetRepository.load() }
            val customs = withContext(Dispatchers.IO) { targetCustom.load() }
            if (userId == targetUserId) {
                rebuild(customs)
                val collections = loadFoodCollections()
                _state.value = _state.value.copy(
                    foods = foodsById.values.toList(),
                    userTags = tags,
                    selectedUserTags = emptySet(),
                    searchHistory = loadSearchHistory(),
                    recentFoodIds = collections.recentIds,
                    favoriteFoodIds = collections.favoriteIds,
                )
            }
        }
    }
    fun openFood(food: FoodItem) {
        val keyword = _state.value.keyword.trim()
        val history = if (keyword.isBlank()) _state.value.searchHistory else {
            (listOf(keyword) + _state.value.searchHistory.filterNot { it.equals(keyword, true) })
        }
        val collections = foodCollections.recordRecent(food.id, foodsById.keys)
        _state.value = _state.value.copy(
            selectedFood = food,
            comparisonReturnTarget = null,
            searchHistory = history,
            recentFoodIds = collections.recentIds,
            favoriteFoodIds = collections.favoriteIds,
        )
        if (keyword.isNotBlank()) saveSearchHistory(history)
    }
    fun closeFood() { _state.value = _state.value.copy(selectedFood = null) }
    fun openComparison(from: NutritionDestination) { _state.value = _state.value.copy(comparisonReturnTarget = from) }
    fun closeComparison() {
        _state.value = _state.value.copy(
            comparisonReturnTarget = null,
            selectedFood = if (_state.value.comparisonReturnTarget == NutritionDestination.FoodDetail) _state.value.selectedFood else null,
        )
    }

    fun openEditor(kind: FoodKind, editingId: String? = null) {
        _state.value = _state.value.copy(editor = NutritionEditorState(kind, editingId))
    }
    fun closeEditor() { _state.value = _state.value.copy(editor = null) }

    /** Persist a custom food (create or edit) and rebuild the merged index. */
    fun saveCustomFood(dto: FoodDto) {
        val targetCustom = customRepository
        val previousImageKey = targetCustom.load().firstOrNull { it.id == dto.id }?.image?.localKey
        val updated = targetCustom.upsert(dto)
        if (previousImageKey != dto.image?.localKey) deleteCustomImage(previousImageKey)
        rebuild(updated.map { it.toDomain() })
        val collections = foodCollections.load(foodsById.keys)
        _state.value = _state.value.copy(
            foods = foodsById.values.toList(),
            editor = null,
            selectedFood = _state.value.selectedFood?.let { selected -> foodsById[selected.id] ?: selected },
            favoriteFoodIds = collections.favoriteIds,
            recentFoodIds = collections.recentIds,
        )
    }

    /** Test-only convenience entry point that writes the normal per-user custom-food archive. */
    fun addTestFoods(dtos: List<FoodDto>) {
        val currentUserId = ProfilePrefs.getCurrentUserId(getApplication())
        val targetRepository = UserCustomFoodRepository.fromContext(getApplication())
        val updated = dtos.fold(targetRepository.loadDtos()) { foods, dto ->
            foods.filterNot { it.id == dto.id } + dto
        }
        targetRepository.save(updated)
        userId = currentUserId
        customRepository = targetRepository
        rebuild(updated.map { it.toDomain() })
        _state.value = _state.value.copy(foods = foodsById.values.toList())
    }

    fun deleteCustomFood(id: String) {
        val targetCustom = customRepository
        val imageKey = targetCustom.load().firstOrNull { it.id == id }?.image?.localKey
        val updated = targetCustom.delete(id)
        deleteCustomImage(imageKey)
        rebuild(updated.map { it.toDomain() })
        val collections = foodCollections.load(foodsById.keys)
        _state.value = _state.value.copy(
            foods = foodsById.values.toList(),
            editor = null,
            selectedFood = _state.value.selectedFood?.takeIf { it.id != id },
            favoriteFoodIds = collections.favoriteIds,
            recentFoodIds = collections.recentIds,
        )
    }

    /** Copies a selected image into the current user's private app storage. */
    fun saveCustomImage(context: Context, uri: Uri, onSaved: (String) -> Unit) {
        val imageUserId = userId ?: return
        viewModelScope.launch {
            val savedKey = withContext(Dispatchers.IO) {
                val directory = File(userArchiveDirectory(context, imageUserId), "attachments/foods").apply { mkdirs() }
                val file = File(directory, "${UUID.randomUUID()}.image")
                runCatching {
                    checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        file.outputStream().use(input::copyTo)
                    }
                    "user:user_archives/${imageUserId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/attachments/foods/${file.name}"
                }.getOrElse {
                    file.delete()
                    null
                }
            }
            savedKey?.let(onSaved)
        }
    }

    private fun deleteCustomImage(key: String?) {
        if (key == null || !key.startsWith("user:")) return
        val relativePath = key.removePrefix("user:")
        val root = getApplication<Application>().filesDir.canonicalFile
        val imageFile = java.io.File(root, relativePath).canonicalFile
        if (imageFile.path.startsWith(root.path + java.io.File.separator)) imageFile.delete()
    }

    fun isCustom(id: String): Boolean = UserCustomFoodRepository.isCustom(id)

    fun canNavigateBack(): Boolean = _state.value.selectedFood != null ||
        _state.value.comparisonReturnTarget != null ||
        _state.value.editor != null
    fun navigateBack(): Boolean {
        val state = _state.value
        when {
            state.comparisonReturnTarget != null -> closeComparison()
            state.editor != null -> closeEditor()
            state.selectedFood != null -> closeFood()
            else -> return false
        }
        return true
    }
    fun filteredFoods(language: String): List<FoodItem> = state.value.let { state ->
        state.foods.filter { food ->
            if (food.kind != state.selectedKind) return@filter false
            val searchable = food.searchableNames().joinToString(" ").lowercase()
            val categoryTags = (food as? CategorizedFood)?.categoryTags.orEmpty()
            val root = repository.hasAnyCategory(categoryTags, state.selectedRoots)
            val child = state.selectedChildren.isEmpty() || state.selectedChildren.any { repository.hasCategory(categoryTags, it) }
            val systemTag = state.selectedSystemTags.isEmpty() || state.selectedSystemTags.all { tag ->
                when (tag) {
                    "common" -> "common" in food.systemTags
                    "favorite" -> food.id in state.favoriteFoodIds
                    "recent" -> food.id in state.recentFoodIds
                    else -> false
                }
            }
            val custom = !state.customOnly || isCustom(food.id)
            searchable.contains(state.keyword.lowercase()) && root && child && systemTag && custom
        }
    }.sortedWith(compareByDescending<FoodItem> { it.commonness }.thenBy { it.displayName(language) })

    private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
    private fun loadSearchHistory(): List<String> = deserializeSearchHistory(
        UserPrefs.current(getApplication()).getString(NUTRITION_SEARCH_HISTORY_KEY, "[]"),
    )
    private fun saveSearchHistory(history: List<String>) {
        UserPrefs.current(getApplication()).putString(NUTRITION_SEARCH_HISTORY_KEY, serializeSearchHistory(history))
    }
    private fun updateCollections(collections: com.woshiwangnima.healthdietpro.model.prefs.UserItemCollectionState) {
        _state.value = _state.value.copy(
            favoriteFoodIds = collections.favoriteIds,
            recentFoodIds = collections.recentIds,
        )
    }
    private fun loadFoodCollections(): com.woshiwangnima.healthdietpro.model.prefs.UserItemCollectionState {
        val prefs = UserPrefs.current(getApplication())
        val legacyRecentIds = deserializeSearchHistory(prefs.getString(NUTRITION_RECENT_FOODS_KEY, "[]"))
        val collections = foodCollections.load(foodsById.keys, legacyRecentIds)
        if (prefs.contains(NUTRITION_RECENT_FOODS_KEY)) prefs.remove(NUTRITION_RECENT_FOODS_KEY)
        return collections
    }
    private companion object {
        const val NUTRITION_SEARCH_HISTORY_KEY = "nutrition_search_history_v1"
        const val NUTRITION_FOOD_COLLECTIONS_KEY = "nutrition_food_collections_v1"
        const val NUTRITION_RECENT_FOODS_KEY = "nutrition_recent_foods_v1"
    }
}
