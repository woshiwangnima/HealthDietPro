package com.woshiwangnima.healthdietpro.ui.nutrition

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.food.Food
import com.woshiwangnima.healthdietpro.model.food.FoodCategories
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientRepository
import com.woshiwangnima.healthdietpro.model.food.UserFoodTag
import com.woshiwangnima.healthdietpro.model.food.UserFoodTagRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
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
    val selectedRoot: String? = null,
    val selectedCustomFilter: CustomFoodFilter? = null,
    val selectedChildren: Set<String> = emptySet(),
    val selectedSystemTag: String? = null,
    val userTags: List<UserFoodTag> = emptyList(),
    val selectedUserTags: Set<String> = emptySet(),
    val selectedFood: Food? = null,
    val showComparison: Boolean = false,
    val managementScreen: NutritionManagementScreen? = null,
)

internal enum class NutritionManagementScreen { CustomFood, MealSet }

internal enum class CustomFoodFilter { Food, MealSet }

internal class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FoodNutrientRepository.fromContext(application)
    private val tagRepository = UserFoodTagRepository.fromContext(application)
    private var userId = ProfilePrefs.getCurrentUserId(application)
    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()
    init { viewModelScope.launch { _state.value = _state.value.copy(foods = withContext(Dispatchers.IO) { repository.foods() }, userTags = withContext(Dispatchers.IO) { tagRepository.load(userId) }) } }
    fun setKeyword(value: String) { _state.value = _state.value.copy(keyword = value) }
    fun selectRoot(tag: String) { _state.value = _state.value.copy(selectedRoot = tag, selectedCustomFilter = null, selectedChildren = emptySet()) }
    fun selectCustomFilter(filter: CustomFoodFilter) {
        _state.value = _state.value.copy(
            selectedRoot = null,
            selectedCustomFilter = if (_state.value.selectedCustomFilter == filter) null else filter,
            selectedChildren = emptySet(),
        )
    }
    fun toggleChild(tag: String) { _state.value = _state.value.let { it.copy(selectedChildren = it.selectedChildren.toggle(tag)) } }
    fun toggleSystemTag(tag: String) {
        _state.value = _state.value.copy(
            selectedSystemTag = if (_state.value.selectedSystemTag == tag) null else tag,
        )
    }
    fun toggleUserTag(tag: String) { _state.value = _state.value.let { it.copy(selectedUserTags = it.selectedUserTags.toggle(tag)) } }
    fun addUserTag(label: String) {
        if (label.isBlank()) return
        val tags = _state.value.userTags + UserFoodTag(UUID.randomUUID().toString(), label.trim())
        val targetUserId = userId
        _state.value = _state.value.copy(userTags = tags)
        viewModelScope.launch(Dispatchers.IO) { tagRepository.save(targetUserId, tags) }
    }
    fun refreshUser() {
        val targetUserId = ProfilePrefs.getCurrentUserId(getApplication())
        userId = targetUserId
        viewModelScope.launch {
            val tags = withContext(Dispatchers.IO) { tagRepository.load(targetUserId) }
            if (userId == targetUserId) {
                _state.value = _state.value.copy(userTags = tags, selectedUserTags = emptySet())
            }
        }
    }
    fun openFood(food: Food) { _state.value = _state.value.copy(selectedFood = food) }
    fun closeFood() { _state.value = _state.value.copy(selectedFood = null) }
    fun openComparison() { _state.value = _state.value.copy(showComparison = true) }
    fun closeComparison() { _state.value = _state.value.copy(showComparison = false) }
    fun openManagement(screen: NutritionManagementScreen) { _state.value = _state.value.copy(managementScreen = screen) }
    fun closeManagement() { _state.value = _state.value.copy(managementScreen = null) }
    fun filteredFoods(language: String): List<Food> = state.value.let { state ->
        state.foods.filter { food ->
            val searchable = (food.displayName(language) + food.aliases.joinToString(" ")).lowercase()
            val root = state.selectedRoot == null || FoodCategories.isWithin(food.categoryTag, state.selectedRoot)
            val child = state.selectedChildren.isEmpty() || state.selectedChildren.any { FoodCategories.isWithin(food.categoryTag, it) }
            val custom = when (state.selectedCustomFilter) {
                CustomFoodFilter.Food -> food.categoryTag.startsWith("food.custom")
                CustomFoodFilter.MealSet -> food.categoryTag.startsWith("meal.custom")
                null -> true
            }
            searchable.contains(state.keyword.lowercase()) && root && child && custom
        }
    }
    private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
}
