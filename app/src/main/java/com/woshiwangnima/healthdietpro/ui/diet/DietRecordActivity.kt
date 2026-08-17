package com.woshiwangnima.healthdietpro.ui.diet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.MainActivity
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.diet.DietPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.loadDietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.loadDietPrefs
import com.woshiwangnima.healthdietpro.model.diet.recommendedDietGoals
import com.woshiwangnima.healthdietpro.model.diet.saveDietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.saveDietPrefs
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs

/** 记饮食：一次用餐（时间 + 时段 + 食物信息数组）的记录。 */
class DietRecordActivity : BaseActivity() {
    companion object {
        const val EXTRA_OPEN_EDITOR = "open_editor"
    }

    private val dietViewModel: DietViewModel by lazy {
        ViewModelProvider(this)[DietViewModel::class.java]
    }

    private val customFoodEditorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val createdId = result.data?.getStringExtra(MainActivity.EXTRA_CREATED_CUSTOM_FOOD_ID)
            createdId?.let { dietViewModel.onCustomFoodCreated(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                DietRoute(
                    viewModel = dietViewModel,
                    onFinish = ::finish,
                    onCreateCustomFood = ::launchCustomFoodEditor,
                    openEditorInitially = intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false),
                )
            }
        }
    }

    private fun launchCustomFoodEditor(kind: FoodKind) {
        customFoodEditorLauncher.launch(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_NUTRITION_EDITOR_KIND, kind.name),
        )
    }
}

private enum class DietRoute { HOME, EDITOR, SETTINGS, DEFAULT_DURATION, GOALS, CONTAINERS }

@Composable
private fun DietRoute(
    viewModel: DietViewModel,
    onFinish: () -> Unit,
    onCreateCustomFood: (FoodKind) -> Unit,
    openEditorInitially: Boolean,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var prefs by remember { mutableStateOf(loadDietPrefs(context)) }
    var goals by remember { mutableStateOf(loadDietGoalsPrefs(context)) }
    val profile = remember { ProfilePrefs.load(context) }
    val recommendedGoals = remember(profile) {
        recommendedDietGoals(
            weightKg = profile.latestWeight?.value?.toDouble(),
            heightCm = profile.latestHeight?.value?.toDouble(),
            age = profile.age,
            isMale = profile.gender == Gender.MALE,
        )
    }
    var route by rememberSaveable { mutableStateOf(if (openEditorInitially) DietRoute.EDITOR else DietRoute.HOME) }
    var editingRecord by remember { mutableStateOf<DietRecord?>(null) }

    BackHandler(enabled = route != DietRoute.HOME) {
        when {
            openEditorInitially && route == DietRoute.EDITOR -> onFinish()
            route == DietRoute.DEFAULT_DURATION -> route = DietRoute.SETTINGS
            route == DietRoute.GOALS -> route = DietRoute.SETTINGS
            route == DietRoute.CONTAINERS -> route = DietRoute.SETTINGS
            route == DietRoute.SETTINGS -> route = DietRoute.HOME
            else -> route = DietRoute.HOME
        }
    }

    when (route) {
        DietRoute.HOME -> DietHomeScreen(
            uiState = uiState,
            onAdd = { editingRecord = null; route = DietRoute.EDITOR },
            onEdit = { editingRecord = it; route = DietRoute.EDITOR },
            onDelete = viewModel::delete,
            onSettings = { route = DietRoute.SETTINGS },
            onBack = onFinish,
            modifier = Modifier,
        )
        DietRoute.EDITOR -> DietEditorScreen(
            existing = editingRecord,
            prefs = prefs,
            viewModel = viewModel,
            onBack = { route = DietRoute.HOME },
            onCreateCustomFood = onCreateCustomFood,
            modifier = Modifier,
        )
        DietRoute.SETTINGS -> DietSettingsScreen(
            onBack = { route = DietRoute.HOME },
            onDefaultHabits = { route = DietRoute.DEFAULT_DURATION },
            onGoals = { route = DietRoute.GOALS },
            onContainers = { route = DietRoute.CONTAINERS },
        )
        DietRoute.DEFAULT_DURATION -> DietDefaultDurationScreen(
            prefs = prefs,
            onBack = { route = DietRoute.SETTINGS },
            onSave = {
                prefs = it
                saveDietPrefs(context, it)
                route = DietRoute.SETTINGS
            },
        )
        DietRoute.GOALS -> DietGoalsScreen(
            initialGoals = goals,
            recommendedGoals = recommendedGoals,
            onBack = { route = DietRoute.SETTINGS },
            onSave = {
                goals = it
                saveDietGoalsPrefs(context, it)
                viewModel.refreshGoals()
                route = DietRoute.SETTINGS
            },
        )
        DietRoute.CONTAINERS -> DietContainersScreen(
            onBack = { route = DietRoute.SETTINGS },
        )
    }
}