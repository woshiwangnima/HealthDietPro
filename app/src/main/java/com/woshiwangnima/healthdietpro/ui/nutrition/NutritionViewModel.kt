package com.woshiwangnima.healthdietpro.ui.nutrition

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.HealthDietProApplication
import com.woshiwangnima.healthdietpro.model.food.Food
import com.woshiwangnima.healthdietpro.model.food.FoodCategories
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientRepository
import com.woshiwangnima.healthdietpro.model.food.UserFoodTag
import com.woshiwangnima.healthdietpro.model.food.UserFoodTagRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.common.ui.FoodImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

internal data class NutritionUiState(
    val foods: List<Food> = emptyList(),
    val keyword: String = "",
    val selectedRoots: Set<String> = emptySet(),
    val selectedCustomFilter: CustomFoodFilter? = null,
    val selectedChildren: Set<String> = emptySet(),
    val selectedSystemTag: String? = null,
    val userTags: List<UserFoodTag> = emptyList(),
    val selectedUserTags: Set<String> = emptySet(),
    val selectedFood: Food? = null,
    val comparisonReturnTarget: NutritionDestination? = null,
    val managementScreen: NutritionManagementScreen? = null,
)

internal enum class NutritionManagementScreen { CustomFood, MealSet }

internal enum class CustomFoodFilter { Food, MealSet }
internal enum class NutritionDestination { Browse, FoodDetail, CustomFood, MealSet }

internal class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FoodNutrientRepository.fromContext(application)
    private var tagRepository = UserFoodTagRepository.fromContext(application)
    val foodImages = FoodImageStore(
        context = application,
        cacheRegistry = (application as HealthDietProApplication).cacheRegistry,
    )
    private var userId = ProfilePrefs.getCurrentUserId(application)
    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()
    init {
        val initialUserId = userId
        val initialTagRepository = tagRepository
        viewModelScope.launch {
            val foods = withContext(Dispatchers.IO) { repository.foods() }
            val tags = withContext(Dispatchers.IO) { initialTagRepository.load() }
            if (userId == initialUserId) {
                _state.value = _state.value.copy(foods = foods, userTags = tags)
            }
        }
    }
    fun setKeyword(value: String) { _state.value = _state.value.copy(keyword = value) }
    fun toggleRoot(tag: String) {
        val state = _state.value
        val selectedRoots = state.selectedRoots.toggle(tag)
        _state.value = state.copy(
            selectedRoots = selectedRoots,
            selectedCustomFilter = null,
            selectedChildren = FoodCategories.retainChildrenForRoots(
                selectedChildren = state.selectedChildren,
                roots = selectedRoots,
            ),
        )
    }
    fun selectCustomFilter(filter: CustomFoodFilter) {
        _state.value = _state.value.copy(
            selectedRoots = emptySet(),
            selectedCustomFilter = if (_state.value.selectedCustomFilter == filter) null else filter,
            selectedChildren = emptySet(),
        )
    }
    fun toggleChild(tag: String) {
        val state = _state.value
        if (tag !in FoodCategories.childrenForRoots(state.selectedRoots).map { it.tag }) return
        _state.value = state.copy(selectedChildren = state.selectedChildren.toggle(tag))
    }
    fun toggleSystemTag(tag: String) {
        _state.value = _state.value.copy(
            selectedSystemTag = if (_state.value.selectedSystemTag == tag) null else tag,
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
        val targetRepository = tagRepository
        viewModelScope.launch {
            val tags = withContext(Dispatchers.IO) { targetRepository.load() }
            if (userId == targetUserId) {
                _state.value = _state.value.copy(userTags = tags, selectedUserTags = emptySet())
            }
        }
    }
    fun openFood(food: Food) { _state.value = _state.value.copy(selectedFood = food, comparisonReturnTarget = null) }
    fun closeFood() { _state.value = _state.value.copy(selectedFood = null) }
    fun openComparison(from: NutritionDestination) { _state.value = _state.value.copy(comparisonReturnTarget = from) }
    fun closeComparison() {
        _state.value = _state.value.copy(
            comparisonReturnTarget = null,
            selectedFood = if (_state.value.comparisonReturnTarget == NutritionDestination.FoodDetail) _state.value.selectedFood else null,
            managementScreen = when (_state.value.comparisonReturnTarget) {
                NutritionDestination.CustomFood -> NutritionManagementScreen.CustomFood
                NutritionDestination.MealSet -> NutritionManagementScreen.MealSet
                else -> null
            },
        )
    }
    fun openManagement(screen: NutritionManagementScreen) { _state.value = _state.value.copy(managementScreen = screen) }
    fun closeManagement() { _state.value = _state.value.copy(managementScreen = null) }
    fun canNavigateBack(): Boolean = _state.value.selectedFood != null ||
        _state.value.comparisonReturnTarget != null ||
        _state.value.managementScreen != null
    fun navigateBack(): Boolean {
        val state = _state.value
        when {
            state.comparisonReturnTarget != null -> closeComparison()
            state.selectedFood != null -> closeFood()
            state.managementScreen != null -> closeManagement()
            else -> return false
        }
        return true
    }
    fun filteredFoods(language: String): List<Food> = state.value.let { state ->
        state.foods.filter { food ->
            val searchable = food.searchableNames().joinToString(" ").lowercase()
            val root = FoodCategories.hasTagWithinAny(food.categoryTags, state.selectedRoots)
            val child = state.selectedChildren.isEmpty() || state.selectedChildren.any { FoodCategories.hasTagWithin(food.categoryTags, it) }
            val custom = when (state.selectedCustomFilter) {
                CustomFoodFilter.Food -> food.categoryTags.any { it.startsWith("food.custom") }
                CustomFoodFilter.MealSet -> food.categoryTags.any { it.startsWith("meal.custom") }
                null -> true
            }
            searchable.contains(state.keyword.lowercase()) && root && child && custom
        }
    }.sortedWith(compareByDescending<Food> { it.commonness }.thenBy { it.displayName(language) })
    private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
}
