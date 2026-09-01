package com.woshiwangnima.healthdietpro.ui.nutrition

import android.app.Application
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import java.io.FileOutputStream
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.HealthDietProApplication
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.BotanicalTaxonomyLabels
import com.woshiwangnima.healthdietpro.model.food.BotanicalTaxonomyRepository
import com.woshiwangnima.healthdietpro.model.food.CookingMethod
import com.woshiwangnima.healthdietpro.model.food.CookingMethodRepository
import com.woshiwangnima.healthdietpro.model.food.Dish
import com.woshiwangnima.healthdietpro.model.food.DriNrvRepository
import com.woshiwangnima.healthdietpro.model.food.FoodDto
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.PreparedFood
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientRepository
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.NutrientMetaRepository
import com.woshiwangnima.healthdietpro.model.food.NrvReference
import com.woshiwangnima.healthdietpro.model.food.NutritionResolver
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.model.food.ServingContainer
import com.woshiwangnima.healthdietpro.model.food.ServingContainerRepository
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodRepository
import com.woshiwangnima.healthdietpro.model.food.UserFoodTag
import com.woshiwangnima.healthdietpro.model.food.UserFoodTagRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.disease.hasCurrentUserDiabetesRisk
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecordRepository
import com.woshiwangnima.healthdietpro.model.disease.stableId
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.prefs.UserItemCollectionRepository
import com.woshiwangnima.healthdietpro.model.prefs.deserializeSearchHistory
import com.woshiwangnima.healthdietpro.model.prefs.serializeSearchHistory
import com.woshiwangnima.healthdietpro.common.ui.FoodImageStore
import com.woshiwangnima.healthdietpro.common.cache.FoodCardMetadataCache
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
import com.woshiwangnima.healthdietpro.model.food.FoodCardMetadata
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
    val listHighlightFoodId: String? = null,
    val listHighlightToken: Long = 0L,
    val comparisonReturnTarget: NutritionDestination? = null,
    val editor: NutritionEditorState? = null,
    val nrvReference: NrvReference? = null,
    val nrvReferences: List<NrvReference> = emptyList(),
    val exerciseRequest: NutritionExerciseRequest? = null,
    val showGlycemicIndicator: Boolean = false,
)

internal data class NutritionExerciseServing(val label: String, val kilocalories: Double)

internal data class NutritionExerciseRequest(val servings: List<NutritionExerciseServing>)

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
    private val driNrvRepository = DriNrvRepository.fromContext(application)
    private val botanicalTaxonomyRepository = BotanicalTaxonomyRepository.fromContext(application)
    private var tagRepository = UserFoodTagRepository.fromContext(application)
    private var customRepository = UserCustomFoodRepository.fromContext(application)
    private var foodCollections = UserItemCollectionRepository.fromContext(application, NUTRITION_FOOD_COLLECTIONS_KEY)
    val foodImages = FoodImageStore(
        context = application,
        cacheRegistry = (application as HealthDietProApplication).cacheRegistry,
    )
    private val cardMetadataCache = FoodCardMetadataCache().also { (application as HealthDietProApplication).cacheRegistry.register(it) }
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
    private var diseaseRiskRefreshVersion = 0L
    init {
        val initialUserId = userId
        val initialTagRepository = tagRepository
        val initialCustomRepository = customRepository
        viewModelScope.launch {
            val initialDiseaseRiskVersion = diseaseRiskRefreshVersion
            val showGlycemicIndicator = withContext(Dispatchers.IO) { hasCurrentUserDiabetesRisk(application) }
            val foods = withContext(Dispatchers.IO) { repository.foods() }
            withContext(Dispatchers.IO) { repository.warmIndexes() }
            val methods = withContext(Dispatchers.IO) { cookingMethodRepository.byId() }
            val loadedContainers = withContext(Dispatchers.IO) { servingContainerRepository.containers() }
            val tags = withContext(Dispatchers.IO) { initialTagRepository.load() }
            val customs = withContext(Dispatchers.IO) { initialCustomRepository.load() }
            val metas = withContext(Dispatchers.IO) { nutrientMetaRepository.nutrients() }
            val taxonomy = withContext(Dispatchers.IO) { botanicalTaxonomyRepository.labels() }
            val nrvReferences = withContext(Dispatchers.IO) { driNrvRepository.referencesFor(ProfilePrefs.load(application)) }
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
                    nrvReference = nrvReferences.firstOrNull(),
                    nrvReferences = nrvReferences,
                    showGlycemicIndicator = if (diseaseRiskRefreshVersion == initialDiseaseRiskVersion) showGlycemicIndicator else _state.value.showGlycemicIndicator,
                )
            }
        }
    }

    /** Rebuild the merged food index + resolver from built-ins plus current custom foods. */
    private fun rebuild(customFoods: List<FoodItem>) {
        cardMetadataCache.invalidate()
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

    /** Refreshes the low-cost disease-dependent UI flag when the app returns to the foreground. */
    internal fun refreshDiseaseRisk() {
        val refreshVersion = ++diseaseRiskRefreshVersion
        viewModelScope.launch(Dispatchers.IO) {
            val visible = hasCurrentUserDiabetesRisk(getApplication())
            if (refreshVersion == diseaseRiskRefreshVersion) {
                _state.value = _state.value.copy(showGlycemicIndicator = visible)
            }
        }
    }

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
        .filter { dish ->
            repository.relatedDishIds(foodId).contains(dish.id) ||
                dish.components.any { it.foodId == foodId }
        }

    fun selectKind(kind: FoodKind) {
        if (_state.value.selectedKind == kind) return
        _state.value = _state.value.copy(
            selectedKind = kind,
            selectedRoots = emptySet(),
            selectedChildren = emptySet(),
            selectedSystemTags = emptySet(),
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
            val nrvReferences = withContext(Dispatchers.IO) { driNrvRepository.referencesFor(ProfilePrefs.load(getApplication())) }
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
                    nrvReference = nrvReferences.firstOrNull(),
                    nrvReferences = nrvReferences,
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
    fun openFood(id: String) {
        foodsById[id]?.let(::openFood)
    }
    fun closeFood() {
        val foodId = _state.value.selectedFood?.id
        _state.value = _state.value.copy(
            selectedFood = null,
            listHighlightFoodId = foodId,
            listHighlightToken = if (foodId == null) _state.value.listHighlightToken else _state.value.listHighlightToken + 1,
        )
    }
    fun selectNrvReference(id: String) {
        _state.value.nrvReferences.firstOrNull { it.id == id }?.let { reference ->
            _state.value = _state.value.copy(nrvReference = reference)
        }
    }
    fun openExerciseExpenditure(servings: List<NutritionExerciseServing>) {
        _state.value = _state.value.copy(exerciseRequest = NutritionExerciseRequest(servings))
    }
    fun closeExerciseExpenditure() { _state.value = _state.value.copy(exerciseRequest = null) }
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
        val updated = targetCustom.upsert(dto)
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
        val updated = targetCustom.delete(id)
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
                val id = UUID.randomUUID().toString()
                val original = File(directory, "$id.original")
                val detail = File(directory, "$id.detail.webp")
                val thumb = File(directory, "$id.thumb.webp")
                runCatching {
                    checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        original.outputStream().use(input::copyTo)
                    }
                    val bitmap = checkNotNull(android.graphics.BitmapFactory.decodeFile(original.absolutePath))
                    FileOutputStream(detail).use { output ->
                        check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 86, output))
                    }
                    val scale = minOf(1.0, 160.0 / maxOf(bitmap.width, bitmap.height).toDouble())
                    val preview = if (scale < 1.0) {
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    } else {
                        bitmap
                    }
                    FileOutputStream(thumb).use { output ->
                        check(preview.compress(Bitmap.CompressFormat.WEBP_LOSSY, 82, output))
                    }
                    if (preview !== bitmap) preview.recycle()
                    bitmap.recycle()
                    "user:user_archives/${imageUserId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/attachments/foods/${detail.name}"
                }.getOrElse {
                    original.delete()
                    detail.delete()
                    thumb.delete()
                    null
                }
            }
            savedKey?.let(onSaved)
        }
    }

    fun isCustom(id: String): Boolean = UserCustomFoodRepository.isCustom(id)

    fun canNavigateBack(): Boolean = _state.value.selectedFood != null ||
        _state.value.comparisonReturnTarget != null ||
        _state.value.editor != null ||
        _state.value.exerciseRequest != null
    fun navigateBack(): Boolean {
        val state = _state.value
        when {
            state.exerciseRequest != null -> closeExerciseExpenditure()
            state.comparisonReturnTarget != null -> closeComparison()
            state.editor != null -> closeEditor()
            state.selectedFood != null -> closeFood()
            else -> return false
        }
        return true
    }
    fun filteredFoods(language: String): List<FoodItem> = state.value.let { state ->
        val indexedIds = state.foods.map(FoodItem::id).toMutableSet().apply {
            if (state.keyword.isNotBlank()) {
                val query = state.keyword.lowercase().replace(" ", "")
                val searchIds = repository.searchIds(state.keyword) + state.foods.filter { food ->
                    food.searchableNames().any { it.lowercase().replace(" ", "").contains(query) }
                }.map(FoodItem::id)
                retainAll(searchIds.toSet())
            }
            if (state.selectedRoots.isNotEmpty()) {
                val rootIds = state.selectedRoots.flatMap(repository::categoryIds) + state.foods
                    .filter { food ->
                        val tags = (food as? CategorizedFood)?.categoryTags.orEmpty()
                        state.selectedRoots.any { repository.hasCategory(tags, it) }
                    }
                    .map(FoodItem::id)
                retainAll(rootIds.toSet())
            }
            if (state.selectedChildren.isNotEmpty()) {
                val childIds = state.selectedChildren.flatMap(repository::categoryIds) + state.foods
                    .filter { food ->
                        val tags = (food as? CategorizedFood)?.categoryTags.orEmpty()
                        state.selectedChildren.any { repository.hasCategory(tags, it) }
                    }
                    .map(FoodItem::id)
                retainAll(childIds.toSet())
            }
        }
        state.foods.filter { food ->
            if (food.id !in indexedIds) return@filter false
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

    fun filteredCardMetadata(language: String): List<FoodCardMetadata> = filteredFoods(language).map { food ->
        val key = "${food.id}#$language#${(food as? CategorizedFood)?.categoryTags.orEmpty().joinToString()}#${_state.value.favoriteFoodIds.contains(food.id)}#${_state.value.recentFoodIds.contains(food.id)}"
        cardMetadataCache.get(key) ?: buildCardMetadata(food, language).also { cardMetadataCache.put(key, it) }
    }

    private fun buildCardMetadata(food: FoodItem, language: String): FoodCardMetadata {
        val aliases = food.allNames(language).drop(1)
        val categoryLabels = (food as? CategorizedFood)?.categoryTags.orEmpty().mapNotNull { tag ->
            categoryDisplayPath(tag).takeIf { it.isNotEmpty() }?.joinToString(".") { resourceId -> getApplication<Application>().getString(resourceId) }
        }
        val cookingMethodLabel = (food as? PreparedFood)?.let { prepared ->
            val methodId = prepared.derivedFrom?.cookingMethodId ?: prepared.techniqueId
            methodId?.let { cookingMethodFor(it)?.displayLabel(language) }
        }
        val componentCount = when (food) {
            is Dish -> food.components.size
            is PreparedFood -> food.components.takeIf { it.isNotEmpty() }?.size
            else -> null
        }
        val energy = runCatching { resolvePer100g(food).nutrients["ENERGY"]?.value ?: 0.0 }.getOrDefault(0.0)
        return FoodCardMetadata(
            id = food.id,
            kind = food.kind,
            primaryName = food.displayName(language),
            aliases = aliases,
            categoryLabels = categoryLabels,
            cookingMethodLabel = cookingMethodLabel,
            componentCount = componentCount,
            imageKey = food.image?.localKey?.takeUnless { it == FoodImageStore.DEFAULT_KEY } ?: food.id,
            systemTags = food.systemTags,
            isCustom = isCustom(food.id),
            isFavorite = isFavorite(food.id),
            isRecent = food.id in _state.value.recentFoodIds,
            energyPer100g = energy,
            glycemicIndex = food.healthMetrics.glycemicIndex?.value,
            glycemicLoadPer100g = food.healthMetrics.glycemicLoadPer100g?.value,
        )
    }

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
