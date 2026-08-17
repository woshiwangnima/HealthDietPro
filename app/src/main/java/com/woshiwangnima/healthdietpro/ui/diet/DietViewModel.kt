package com.woshiwangnima.healthdietpro.ui.diet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.HealthDietProApplication
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.ContainerRepository
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietNutrientAmount
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.DietRepository
import com.woshiwangnima.healthdietpro.model.diet.loadDietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.food.CookingMethod
import com.woshiwangnima.healthdietpro.model.food.CookingMethodRepository
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.NutritionResolver
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodRepository
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class DietUiState(
    val records: List<DietRecord> = emptyList(),
    val foods: List<FoodItem> = emptyList(),
    val containers: List<ContainerRecord> = emptyList(),
    val weightUnitIds: List<String> = listOf("g"),
    val defaultWeightUnitId: String = "g",
    val pendingCreatedFoodId: String? = null,
    val goals: DietGoalsPrefs = DietGoalsPrefs(),
)

class DietViewModel(application: Application) : AndroidViewModel(application) {
    private val dietRepository = DietRepository.fromContext(application)
    private val foodNutrientRepository = (application as HealthDietProApplication).foodNutrientRepository
    private val cookingMethodRepository = CookingMethodRepository.fromContext(application)
    private var customFoodRepository = UserCustomFoodRepository.fromContext(application)
    private var containerRepository = ContainerRepository.fromContext(application)

    private val _state = MutableStateFlow(DietUiState())
    internal val state: StateFlow<DietUiState> = _state.asStateFlow()

    private var foodsById: Map<String, FoodItem> = emptyMap()
    private var cookingMethodsById: Map<String, CookingMethod> = emptyMap()
    private var resolver: NutritionResolver = NutritionResolver(emptyMap(), emptyMap())

    init {
        refresh()
        refreshFoods()
        refreshContainers()
        _state.value = _state.value.copy(
            weightUnitIds = weightUnits(),
            goals = loadDietGoalsPrefs(application),
        )
    }

    internal fun refreshGoals() {
        _state.value = _state.value.copy(goals = loadDietGoalsPrefs(getApplication()))
    }

    internal fun refresh() {
        _state.value = _state.value.copy(records = dietRepository.load().records)
    }

    internal fun save(record: DietRecord) {
        dietRepository.upsert(record)
        refresh()
    }

    internal fun delete(id: String) {
        dietRepository.delete(id)
        refresh()
    }

    internal fun newId(): String = UUID.randomUUID().toString()

    internal fun foodById(id: String): FoodItem? = foodsById[id]

    internal fun categoryRoots(): List<com.woshiwangnima.healthdietpro.model.food.FoodCategory> = foodNutrientRepository.categoryRoots()

    internal fun categoryChildren(parentTag: String): List<com.woshiwangnima.healthdietpro.model.food.FoodCategory> = foodNutrientRepository.categoryChildren(parentTag)

    internal fun hasCategory(tags: List<String>, ancestor: String): Boolean = foodNutrientRepository.hasCategory(tags, ancestor)

    internal fun hasAnyCategory(tags: List<String>, ancestors: Set<String>): Boolean = foodNutrientRepository.hasAnyCategory(tags, ancestors)

    internal fun containerById(id: String): ContainerRecord? =
        _state.value.containers.firstOrNull { it.id == id }

    /** Tare candidates: user containers that recorded a positive empty mass. */
    internal fun refreshContainers() {
        viewModelScope.launch {
            val containers = withContext(Dispatchers.IO) {
                containerRepository.load().containers.filter { it.emptyMassGrams != null && it.emptyMassGrams > 0.0 }
            }
            _state.value = _state.value.copy(containers = containers)
        }
    }

    /** Merge built-in foods with the current user's custom foods and rebuild the resolver. */
    internal fun refreshFoods() {
        viewModelScope.launch {
            val builtIn = withContext(Dispatchers.IO) { foodNutrientRepository.foods() }
            val methods = withContext(Dispatchers.IO) { cookingMethodRepository.byId() }
            val customs = withContext(Dispatchers.IO) { customFoodRepository.load() }
            foodsById = (builtIn + customs).associateBy(FoodItem::id)
            cookingMethodsById = methods
            resolver = NutritionResolver(foodsById, cookingMethodsById)
            _state.value = _state.value.copy(foods = foodsById.values.toList())
        }
    }

    /** Rebuild index after the user creates a custom food and auto-select it in the food sheet. */
    internal fun onCustomFoodCreated(id: String) {
        customFoodRepository = UserCustomFoodRepository.fromContext(getApplication())
        refreshFoods()
        _state.value = _state.value.copy(pendingCreatedFoodId = id)
    }

    internal fun consumeCreatedFoodId() {
        _state.value = _state.value.copy(pendingCreatedFoodId = null)
    }

    internal fun resolvePer100g(food: FoodItem): ResolvedNutrition? =
        runCatching { resolver.resolvePer100g(food) }.getOrNull()

    /** Scale a per-100g resolved nutrition snapshot to a consumed net mass in grams. */
    internal fun scaleToNetGrams(resolved: ResolvedNutrition, netGrams: Double): Map<String, DietNutrientAmount> {
        val factor = netGrams / 100.0
        return resolved.nutrients.mapValues { (_, amount) ->
            DietNutrientAmount(amount.value * factor, amount.unitCategory, amount.unitId)
        }
    }

    /** Build a persisted entry snapshot. Free-name entries carry no nutrition data. */
    internal fun buildEntry(food: FoodItem?, foodName: String, weightValue: Double, weightUnitId: String, container: ContainerRecord?): DietFoodEntry {
        val netGrams = netGrams(weightValue, weightUnitId, container)
        val nutrients = food?.let { item ->
            resolvePer100g(item)?.let { scaleToNetGrams(it, netGrams) }.orEmpty()
        }.orEmpty()
        return DietFoodEntry(
            foodId = food?.id,
            foodName = foodName.trim(),
            foodKind = food?.kind,
            weightValue = weightValue,
            weightUnitId = weightUnitId,
            containerId = container?.id,
            netWeightGrams = netGrams,
            resolvedNutrients = nutrients,
        )
    }

    /** base grams from a weight-category value + unit id, subtracting container empty mass. */
    internal fun netGrams(weightValue: Double, weightUnitId: String, container: ContainerRecord?): Double {
        val baseKg = UnitConverter.toBase(UnitCategoryType.Weight.id, weightValue.toFloat(), weightUnitId)
        val grossGrams = baseKg * 1000.0
        val tareGrams = container?.emptyMassGrams ?: 0.0
        return (grossGrams - tareGrams).coerceAtLeast(0.0)
    }

    internal fun weightUnits(): List<String> {
        val repo = UnitConverter.getRepository() ?: return listOf("g")
        val weightUnits = repo.getCategory(UnitCategoryType.Weight.id)?.units.orEmpty()
        return listOf("g", "kg", "liang", "jin", "oz", "lb").filter { id -> weightUnits.any { it.id == id } }.ifEmpty { listOf("g") }
    }

    /** Convert a net mass in grams back to a value in the given weight unit (weight base is kg). */
    internal fun gramsToUnitValue(netGrams: Double, weightUnitId: String): Double {
        val repo = UnitConverter.getRepository() ?: return netGrams
        return UnitConverter.fromBase(UnitCategoryType.Weight.id, (netGrams / 1000.0).toFloat(), weightUnitId).toDouble()
    }
}

internal fun newDietId(): String = UUID.randomUUID().toString()