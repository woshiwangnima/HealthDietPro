package com.woshiwangnima.healthdietpro

import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets as AndroidWindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.AppBottomNavItem
import com.woshiwangnima.healthdietpro.common.ui.AppBottomNavigationBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.medication.MedicationCatalogItem
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.ui.nutrition.NutritionScreen
import com.woshiwangnima.healthdietpro.ui.nutrition.NutritionViewModel
import com.woshiwangnima.healthdietpro.ui.profile.BmiDetailActivity
import com.woshiwangnima.healthdietpro.ui.profile.HeightDetailActivity
import com.woshiwangnima.healthdietpro.ui.profile.ProfileEditActivity
import com.woshiwangnima.healthdietpro.ui.profile.ProfileScreen
import com.woshiwangnima.healthdietpro.ui.profile.ProfileAvatarBitmapCache
import com.woshiwangnima.healthdietpro.ui.profile.ProfileUserInfoViewModel
import com.woshiwangnima.healthdietpro.ui.profile.UserSwitchActivity
import com.woshiwangnima.healthdietpro.ui.profile.WeightDetailActivity
import com.woshiwangnima.healthdietpro.ui.record.MedicationListActivity
import com.woshiwangnima.healthdietpro.ui.record.BloodGlucoseActivity
import com.woshiwangnima.healthdietpro.ui.record.RecordActionId
import com.woshiwangnima.healthdietpro.ui.record.RecordScreen
import com.woshiwangnima.healthdietpro.ui.record.RecordViewModel
import com.woshiwangnima.healthdietpro.ui.settings.AppSettingsComposeActivity
import com.woshiwangnima.healthdietpro.ui.settings.UserSettingsActivity
import com.woshiwangnima.healthdietpro.ui.test.TestAccessScreen
import com.woshiwangnima.healthdietpro.ui.test.TestAccessViewModel
import com.woshiwangnima.healthdietpro.ui.test.TestGmScreen
import com.woshiwangnima.healthdietpro.ui.test.CommonUiTestScreen
import com.woshiwangnima.healthdietpro.ui.test.CommonUiTestCategory
import com.woshiwangnima.healthdietpro.ui.test.TestLandingScreen
import com.woshiwangnima.healthdietpro.common.ui.ComponentsPreviewScreen
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabPersistence
import com.woshiwangnima.healthdietpro.util.UnitConverter

class MainActivity : BaseActivity() {

    private val navItems = listOf(
        AppBottomNavItem(ROUTE_NUTRITION, R.string.nav_nutrition, R.drawable.ic_nav_nutrition),
        AppBottomNavItem(ROUTE_RECORD, R.string.nav_record, R.drawable.ic_nav_record),
        AppBottomNavItem(ROUTE_PROFILE, R.string.nav_profile, R.drawable.ic_nav_profile),
        AppBottomNavItem(ROUTE_TEST, R.string.nav_test, R.drawable.ic_nav_test),
    )

    private val recordViewModel: RecordViewModel by viewModels()
    private val nutritionViewModel: NutritionViewModel by viewModels()
    private val profileAvatarBitmapCache = ProfileAvatarBitmapCache()
    private val profileViewModel: ProfileUserInfoViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.NewInstanceFactory().let { factory ->
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ProfileUserInfoViewModel::class.java)) {
                        return ProfileUserInfoViewModel(application, profileAvatarBitmapCache) as T
                    }
                    return factory.create(modelClass)
                }
            }
        }
    }
    private val testAccessViewModel: TestAccessViewModel by viewModels()

    private var selectedRoute by mutableStateOf(ROUTE_NUTRITION)
    private var routeBeforeTest = ROUTE_NUTRITION
    private var showOnboarding by mutableStateOf(false)
    private var lastBackPressedAt = 0L
    private var previousSoftInputMode: Int? = null
    private var testPage by mutableStateOf(TestPage.Landing)
    private var commonUiTestCategory by mutableStateOf<CommonUiTestCategory?>(null)

    private val onboardingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        AppPrefs.markFirstLaunchComplete(this)
        showOnboarding = false
        switchTab(ROUTE_PROFILE)
    }

    private val profileEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        profileViewModel.refresh()
    }

    private val userSwitchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        profileViewModel.refresh()
        nutritionViewModel.refreshUser()
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        profileViewModel.refresh()
    }

    private val heightDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val records = result.bodyRecordsResult() ?: return@registerForActivityResult
        ProfilePrefs.save(this, ProfilePrefs.load(this).copy(heightRecords = records))
        profileViewModel.refresh()
    }

    private val weightDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val records = result.bodyRecordsResult() ?: return@registerForActivityResult
        ProfilePrefs.save(this, ProfilePrefs.load(this).copy(weightRecords = records))
        profileViewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(this)
        selectedRoute = restoredRoute()
        setContent {
            HealthDietProTheme {
                MainShell()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedRoute == ROUTE_NUTRITION && nutritionViewModel.navigateBack()) {
                    return
                } else if (selectedRoute == ROUTE_TEST && testAccessViewModel.isVerified.value) {
                    when {
                        commonUiTestCategory != null -> commonUiTestCategory = null
                        testPage != TestPage.Landing -> testPage = TestPage.Landing
                        else -> returnFromTest()
                    }
                } else handleDoubleBackExit()
            }
        })

        checkFirstLaunch()
    }

    override fun onResume() {
        super.onResume()
        try {
            window.decorView.windowInsetsController?.show(AndroidWindowInsets.Type.systemBars())
        } catch (_: Exception) {
        }
        profileViewModel.refresh()
    }

    override fun onDestroy() {
        restoreSoftInputMode()
        super.onDestroy()
    }

    @Composable
    private fun MainShell() {
        val nutritionState by nutritionViewModel.state.collectAsState()
        val showBottomNavigation = selectedRoute != ROUTE_NUTRITION ||
            (nutritionState.selectedFood == null && nutritionState.comparisonReturnTarget == null && nutritionState.managementScreen == null)
        if (showOnboarding) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.onboarding_title)) },
                text = { Text(stringResource(R.string.onboarding_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onboardingLauncher.launch(Intent(this@MainActivity, ProfileEditActivity::class.java))
                        },
                    ) {
                        Text(stringResource(R.string.onboarding_start))
                    }
                },
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (showBottomNavigation) {
                    AppBottomNavigationBar(
                        items = navItems,
                        selectedRoute = selectedRoute,
                        onItemClick = { switchTab(it.route) },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            },
        ) { padding ->
            MainContent(padding)
        }
    }

    @Composable
    private fun MainContent(padding: PaddingValues) {
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (selectedRoute) {
            ROUTE_NUTRITION -> NutritionScreen(viewModel = nutritionViewModel, modifier = modifier)
            ROUTE_RECORD -> {
                val uiState by recordViewModel.uiState.collectAsState()
                RecordScreen(
                    uiState = uiState,
                    onActionClick = ::handleRecordAction,
                    modifier = modifier,
                )
            }
            ROUTE_PROFILE -> {
                val state by profileViewModel.uiState.collectAsState()
                ProfileScreen(
                    state = state,
                    onOpenAppSettings = {
                        settingsLauncher.launch(Intent(this@MainActivity, AppSettingsComposeActivity::class.java))
                    },
                    onOpenBmi = {
                        startActivity(Intent(this@MainActivity, BmiDetailActivity::class.java))
                    },
                    onOpenUserSettings = {
                        startActivity(Intent(this@MainActivity, UserSettingsActivity::class.java))
                    },
                    onEditProfile = {
                        profileEditLauncher.launch(Intent(this@MainActivity, ProfileEditActivity::class.java))
                    },
                    onOpenUserSwitch = {
                        userSwitchLauncher.launch(Intent(this@MainActivity, UserSwitchActivity::class.java))
                    },
                    modifier = modifier,
                )
            }
            ROUTE_TEST -> {
                val isVerified by testAccessViewModel.isVerified.collectAsState()
                if (isVerified) {
                    when (testPage) {
                        TestPage.Landing -> TestLandingScreen({ testPage = TestPage.Commands }, { testPage = TestPage.CommonUi }, modifier)
                        TestPage.Commands -> TestGmScreen(::addTestHeightRecord, ::addTestWeightRecord, ::addTestMedicationRecord, { testPage = TestPage.Landing }, modifier)
                        TestPage.Features -> ComponentsPreviewScreen(onBack = { testPage = TestPage.Landing })
                        TestPage.CommonUi -> CommonUiTestScreen(commonUiTestCategory, { commonUiTestCategory = it }, { if (commonUiTestCategory == null) testPage = TestPage.Landing else commonUiTestCategory = null }, modifier)
                    }
                } else {
                    TestAccessScreen(
                        onCancel = ::returnFromTest,
                        onVerify = testAccessViewModel::verify,
                        modifier = modifier,
                    )
                }
            }
        }
    }

    private fun restoredRoute(): String {
        val index = TabPersistence.loadIndex(this, MAIN_NAV_KEY, 0)
        return navItems.getOrNull(index)?.route ?: ROUTE_NUTRITION
    }

    private fun checkFirstLaunch() {
        showOnboarding = AppPrefs.isFirstLaunch(this)
    }

    private fun switchTab(route: String) {
        if (route == selectedRoute) return
        val index = navItems.indexOfFirst { it.route == route }
        if (index == -1) return

        if (route == ROUTE_TEST) {
            routeBeforeTest = selectedRoute
            testPage = TestPage.Landing
            commonUiTestCategory = null
            applyTestSoftInputMode()
        } else {
            restoreSoftInputMode()
            TabPersistence.saveIndex(this, MAIN_NAV_KEY, index)
        }
        selectedRoute = route
    }

    private fun returnFromTest() {
        switchTab(routeBeforeTest)
    }

    private fun addTestHeightRecord() = addTestBodyRecord(isWeight = false)

    private fun addTestWeightRecord() = addTestBodyRecord(isWeight = true)

    private fun addTestBodyRecord(isWeight: Boolean) {
        val profile = ProfilePrefs.createDefaultIfEmpty(this)
        val record = BodyRecord(
            date = java.time.LocalDate.now().toString(),
            value = if (isWeight) 67.5f else 170f,
            unit = if (isWeight) "kg" else "cm",
        )
        val updated = if (isWeight) {
            profile.copy(weightRecords = (profile.weightRecords + record).sortedBy { it.date })
        } else {
            profile.copy(heightRecords = (profile.heightRecords + record).sortedBy { it.date })
        }
        ProfilePrefs.save(this, updated)
        profileViewModel.refresh()
        Toast.makeText(this, if (isWeight) R.string.test_gm_weight_added else R.string.test_gm_height_added, Toast.LENGTH_SHORT).show()
    }

    private fun addTestMedicationRecord() {
        ProfilePrefs.createDefaultIfEmpty(this)
        val catalogId = "test_medication"
        val catalog = MedicationPrefs.getCatalog(this)
        val item = catalog.find { it.id == catalogId } ?: MedicationCatalogItem(
            id = catalogId,
            name = getString(R.string.test_gm_medication_name),
            defaultDoseValue = 1f,
            defaultDoseUnit = getString(R.string.test_gm_medication_unit),
            defaultMethod = getString(R.string.test_gm_medication_method),
        ).also { MedicationPrefs.upsertCatalogItem(this, it) }
        MedicationPrefs.addRecord(this, MedicationRecord(
            id = "test_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            medicationName = item.name,
            medicationId = item.id,
            doseValue = item.defaultDoseValue,
            doseUnit = item.defaultDoseUnit,
            specValue = item.specValue,
            specUnitCategory = item.specUnitCategory,
            specUnitId = item.specUnitId,
            method = item.defaultMethod,
            manufacturer = item.manufacturer,
            medicationImagePaths = item.imagePaths,
        ))
        Toast.makeText(this, R.string.test_gm_medication_added, Toast.LENGTH_SHORT).show()
    }

    private fun handleRecordAction(actionId: RecordActionId) {
        when (actionId) {
            RecordActionId.Height -> openHeightDetail()
            RecordActionId.Weight -> openWeightDetail()
            RecordActionId.BloodGlucose -> startActivity(Intent(this, BloodGlucoseActivity::class.java))
            RecordActionId.Medication -> startActivity(Intent(this, MedicationListActivity::class.java))
            RecordActionId.Waist,
            RecordActionId.Period,
            RecordActionId.Diet,
            RecordActionId.Water,
            RecordActionId.Exercise,
            RecordActionId.Sleep,
            RecordActionId.Bowel,
            RecordActionId.Habit,
            RecordActionId.Feeling,
            -> Unit
        }
    }

    private fun openHeightDetail() {
        val profile = ProfilePrefs.load(this)
        heightDetailLauncher.launch(Intent(this, HeightDetailActivity::class.java).apply {
            putExtra("records", ArrayList(profile.heightRecords))
            putExtra("unit", AppPrefs.getUnit(this@MainActivity, UnitCategoryType.Length.id, UnitCategoryType.Length.defaultUnitId))
        })
    }

    private fun openWeightDetail() {
        val profile = ProfilePrefs.load(this)
        weightDetailLauncher.launch(Intent(this, WeightDetailActivity::class.java).apply {
            putExtra("records", ArrayList(profile.weightRecords))
            putExtra("unit", AppPrefs.getUnit(this@MainActivity, UnitCategoryType.Weight.id, UnitCategoryType.Weight.defaultUnitId))
        })
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun androidx.activity.result.ActivityResult.bodyRecordsResult(): List<BodyRecord>? {
        if (resultCode != RESULT_OK) return null
        return data?.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>
    }

    private fun applyTestSoftInputMode() {
        if (previousSoftInputMode == null) previousSoftInputMode = window.attributes.softInputMode
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
        )
    }

    private fun restoreSoftInputMode() {
        previousSoftInputMode?.let(window::setSoftInputMode)
        previousSoftInputMode = null
    }

    private fun handleDoubleBackExit() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressedAt <= BACK_EXIT_WINDOW_MS) {
            finish()
        } else {
            lastBackPressedAt = now
            Toast.makeText(this, R.string.back_press_exit_hint, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val MAIN_NAV_KEY = "main_nav"
        const val BACK_EXIT_WINDOW_MS = 2_000L
        const val ROUTE_NUTRITION = "nutrition"
        const val ROUTE_RECORD = "record"
        const val ROUTE_PROFILE = "profile"
        const val ROUTE_TEST = "test"
    }

    private enum class TestPage { Landing, Commands, Features, CommonUi }
}
