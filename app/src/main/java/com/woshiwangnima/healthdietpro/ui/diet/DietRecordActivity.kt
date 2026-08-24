package com.woshiwangnima.healthdietpro.ui.diet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.woshiwangnima.healthdietpro.model.diet.DietEditorDraftRepository
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
        const val EXTRA_OPEN_RECORD_ID = "open_record_id"
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
                    onOpenFoodDetail = ::openFoodDetail,
                    openEditorInitially = intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false),
                    openRecordId = intent.getStringExtra(EXTRA_OPEN_RECORD_ID),
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

    private fun openFoodDetail(foodId: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_NUTRITION_DETAIL_ID, foodId),
        )
    }
}

private enum class DietRoute { HOME, EDITOR, DETAIL, SETTINGS, DEFAULT_DURATION, GOALS, CONTAINERS }

@Composable
private fun DietRoute(
    viewModel: DietViewModel,
    onFinish: () -> Unit,
    onCreateCustomFood: (FoodKind) -> Unit,
    onOpenFoodDetail: (String) -> Unit,
    openEditorInitially: Boolean,
    openRecordId: String?,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val draftRepository = remember(context) { DietEditorDraftRepository.fromContext(context) }
    val pendingDraft = remember { draftRepository.loadDraft() }
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
    var route by rememberSaveable {
        mutableStateOf(
            when {
                openRecordId != null -> DietRoute.DETAIL
                openEditorInitially || pendingDraft != null -> DietRoute.EDITOR
                else -> DietRoute.HOME
            },
        )
    }
    var editingRecord by remember { mutableStateOf(pendingDraft?.record?.takeIf { it.id.isNotBlank() }) }
    var viewingRecord by remember { mutableStateOf<DietRecord?>(null) }

    LaunchedEffect(Unit) {
        if (pendingDraft != null && route == DietRoute.HOME) route = DietRoute.EDITOR
    }

    BackHandler(enabled = route != DietRoute.HOME) {
        when {
            openEditorInitially && route == DietRoute.EDITOR -> onFinish()
            route == DietRoute.DETAIL && openRecordId != null -> onFinish()
            route == DietRoute.DETAIL -> route = DietRoute.HOME
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
            onOpen = { viewingRecord = it; route = DietRoute.DETAIL },
            onEdit = { editingRecord = it; route = DietRoute.EDITOR },
            onDelete = viewModel::delete,
            onSettings = { route = DietRoute.SETTINGS },
            onBack = onFinish,
            modifier = Modifier,
        )
        DietRoute.DETAIL -> {
            val detailRecord = viewingRecord?.let { current ->
                uiState.records.firstOrNull { it.id == current.id } ?: current
            } ?: openRecordId?.let { id -> uiState.records.firstOrNull { it.id == id } }
            if (detailRecord != null) {
                val zone = remember { java.time.ZoneId.systemDefault() }
                val detailDayStart = remember(detailRecord, zone) {
                    com.woshiwangnima.healthdietpro.common.time.recordDateStartMillis(
                        java.time.Instant.ofEpochMilli(detailRecord.mealStartAt).atZone(zone).toLocalDate(),
                        zone,
                    )
                }
                val detailDayEnd = remember(detailRecord, zone) { detailDayStart + java.time.Duration.ofDays(1).toMillis() }
                val detailDayTotals = remember(uiState.records, detailDayStart, detailDayEnd) {
                    sumNutrients(uiState.records.filter { it.mealStartAt in detailDayStart until detailDayEnd })
                }
                DietMealDetailScreen(
                    record = detailRecord,
                    goals = goals,
                    dayTotals = detailDayTotals,
                    onEdit = { editingRecord = detailRecord; route = DietRoute.EDITOR },
                    onOpenFood = { entry -> entry.foodId?.let(onOpenFoodDetail) },
                    onBack = { if (openRecordId != null) onFinish() else route = DietRoute.HOME },
                    modifier = Modifier,
                )
            }
        }
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
