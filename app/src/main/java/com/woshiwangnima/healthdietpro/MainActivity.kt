package com.woshiwangnima.healthdietpro

import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.view.WindowInsets as AndroidWindowInsets
import android.view.WindowManager
import android.widget.Toast
import android.util.Log
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.AppBottomNavItem
import com.woshiwangnima.healthdietpro.common.ui.AppBottomNavigationBar
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.PagePreloader
import com.woshiwangnima.healthdietpro.common.ui.PAGE_ENTER_DURATION_MILLIS
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.HealthDietProApplication
import com.woshiwangnima.healthdietpro.model.medication.MedicationCatalogItem
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.ui.profile.archive.ArchiveTransferController
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.prefs.serializeSearchHistory
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.BodyMetricsRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDateTime
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodRepository
import com.woshiwangnima.healthdietpro.model.food.DishComponentDto
import com.woshiwangnima.healthdietpro.model.food.FoodAmountDto
import com.woshiwangnima.healthdietpro.model.food.FoodDerivationDto
import com.woshiwangnima.healthdietpro.model.food.FoodDto
import com.woshiwangnima.healthdietpro.model.food.FoodHealthMetricsDto
import com.woshiwangnima.healthdietpro.model.food.FoodMetricDto
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientTableDto
import com.woshiwangnima.healthdietpro.model.food.FoodQuantityDto
import com.woshiwangnima.healthdietpro.model.food.RecipeStepDto
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseSource
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTimingAnchor
import com.woshiwangnima.healthdietpro.model.water.WaterRecord
import com.woshiwangnima.healthdietpro.model.water.WaterRepository
import com.woshiwangnima.healthdietpro.ui.nutrition.NutritionScreen
import com.woshiwangnima.healthdietpro.ui.nutrition.NutritionViewModel
import com.woshiwangnima.healthdietpro.ui.profile.BmiDetailActivity
import com.woshiwangnima.healthdietpro.ui.profile.BodyMetricRecordActivity
import com.woshiwangnima.healthdietpro.ui.profile.HeightDetailActivity
import com.woshiwangnima.healthdietpro.ui.profile.ProfileEditActivity
import com.woshiwangnima.healthdietpro.ui.profile.ProfileScreen
import com.woshiwangnima.healthdietpro.ui.profile.ProfileUserInfoViewModel
import com.woshiwangnima.healthdietpro.ui.splash.StartupSplashHost
import com.woshiwangnima.healthdietpro.ui.profile.UserSwitchActivity
import com.woshiwangnima.healthdietpro.ui.profile.WeightDetailActivity
import com.woshiwangnima.healthdietpro.ui.profile.CircumferenceDetailActivity
import com.woshiwangnima.healthdietpro.ui.record.MedicationListActivity
import com.woshiwangnima.healthdietpro.ui.record.MedicationRecordActivity
import com.woshiwangnima.healthdietpro.ui.record.BloodGlucoseActivity
import com.woshiwangnima.healthdietpro.ui.record.BloodPressureActivity
import com.woshiwangnima.healthdietpro.ui.record.DiseaseRecordActivity
import com.woshiwangnima.healthdietpro.ui.record.WaterRecordActivity
import com.woshiwangnima.healthdietpro.ui.container.ContainerRecordActivity
import com.woshiwangnima.healthdietpro.ui.price.PriceRecordActivity
import com.woshiwangnima.healthdietpro.ui.sleep.SleepRecordActivity
import com.woshiwangnima.healthdietpro.ui.diet.DietRecordActivity
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
import com.woshiwangnima.healthdietpro.ui.test.CrossSectionUiTestScreen
import com.woshiwangnima.healthdietpro.common.ui.ComponentsPreviewScreen
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabPersistence
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.model.bloodglucose.AgpPreviewImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MainActivity : BaseActivity() {

    private val navItems = listOf(
        AppBottomNavItem(ROUTE_NUTRITION, R.string.nav_nutrition, R.drawable.ic_nav_nutrition),
        AppBottomNavItem(ROUTE_RECORD, R.string.nav_record, R.drawable.ic_nav_record),
        AppBottomNavItem(ROUTE_PROFILE, R.string.nav_profile, R.drawable.ic_nav_profile),
        AppBottomNavItem(ROUTE_TEST, R.string.nav_test, R.drawable.ic_nav_test),
    )

    private val recordViewModel: RecordViewModel by viewModels()
    private val nutritionViewModel: NutritionViewModel by viewModels()
    // 头像缓存归 Application 所有：ViewModel 跨配置变更保留，缓存不能跟着 Activity 重建。
    private val profileAvatarBitmapCache
        get() = (application as HealthDietProApplication).profileAvatarBitmapCache
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
    private var isMainPageTransitionRunning by mutableStateOf(false)
    private var routeBeforeTest = ROUTE_NUTRITION
    private var showOnboarding by mutableStateOf(false)
    private var foodDetailMode by mutableStateOf(false)
    private var lastBackPressedAt = 0L
    private var previousSoftInputMode: Int? = null
    private var testPage by mutableStateOf(TestPage.Landing)
    private var commonUiTestCategory by mutableStateOf<CommonUiTestCategory?>(null)
    private val pagePreloader = PagePreloader()
    private var archiveController: ArchiveTransferController? = null
    private var startupSplashVisible = false

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
        profileAvatarBitmapCache.clear()
        profileViewModel.refresh()
        recordViewModel.refresh()
        nutritionViewModel.refreshDiseaseRisk()
    }

    private val userSwitchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        profileAvatarBitmapCache.clear()
        profileViewModel.refresh()
        nutritionViewModel.refreshUser()
        recordViewModel.refresh()
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
        BodyMetricsRepository.current(this).update { it.copy(heightRecords = records) }
        profileViewModel.refresh()
        recordViewModel.refresh()
    }

    private val weightDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val records = result.bodyRecordsResult() ?: return@registerForActivityResult
        BodyMetricsRepository.current(this).update { it.copy(weightRecords = records) }
        profileViewModel.refresh()
        recordViewModel.refresh()
    }
    private val heightRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.bodyRecordResult()?.let { record ->
            BodyMetricsRepository.current(this).update { metrics ->
                metrics.copy(heightRecords = metrics.heightRecords + record)
            }
            profileViewModel.refresh()
            recordViewModel.refresh()
        }
    }
    private val weightRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.bodyRecordResult()?.let { record ->
            BodyMetricsRepository.current(this).update { metrics ->
                metrics.copy(weightRecords = metrics.weightRecords + record)
            }
            profileViewModel.refresh()
            recordViewModel.refresh()
        }
    }
    private val circumferenceDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        @Suppress("DEPRECATION")
        val records = CircumferenceDetailActivity.readRecords(result.data?.getSerializableExtra(CircumferenceDetailActivity.EXTRA_RECORDS))
        if (records.isEmpty() && result.data?.hasExtra(CircumferenceDetailActivity.EXTRA_RECORDS) != true) return@registerForActivityResult
        BodyMetricsRepository.current(this).update { it.copy(circumferenceRecords = records) }
        profileViewModel.refresh()
        recordViewModel.refresh()
    }

    // 导入/导出用系统文件选择器：导出按格式分两个 contract（MIME 在注册时就固定了），
    // 导入统一用 */* —— 用户从聊天软件、网盘存的档案常常没有正确后缀，按 MIME 过滤只会让人找不到文件。
    private val archiveSaveJsonLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> archiveController?.onExportTargetPicked(uri) }

    private val archiveSaveBinaryLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> archiveController?.onExportTargetPicked(uri) }

    private val archiveOpenLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> archiveController?.onImportFilePicked(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动阶段只做必须发生在首帧前的轻量同步初始化；数据加载交给启动页期间的
        // 主界面组合与 preloadMainPages，引导弹窗 / 外部深链则等启动页淡出后再分发。
        UnitConverter.init(this)
        val launchSplash = savedInstanceState == null &&
            !hasExternalLaunchIntent() &&
            AppPrefs.isSplashEnabled(this)
        startupSplashVisible = launchSplash
        archiveController = ArchiveTransferController(
            context = this,
            scope = lifecycleScope,
            launchOpenDocument = { archiveOpenLauncher.launch(it) },
            launchCreateDocument = { mimeType, fileName ->
                when (mimeType) {
                    "application/json" -> archiveSaveJsonLauncher.launch(fileName)
                    else -> archiveSaveBinaryLauncher.launch(fileName)
                }
            },
            onImported = {
                profileAvatarBitmapCache.clear()
                profileViewModel.refresh()
                nutritionViewModel.refreshUser()
                recordViewModel.refresh()
            },
        )
        selectedRoute = restoredRoute()
        setContent {
            HealthDietProTheme {
                StartupSplashHost(
                    enabled = launchSplash,
                    // 引导弹窗是独立 Dialog 窗口，必须等启动页淡出后再弹，否则会盖在启动页上。
                    // 外部深链也在此时再分发，避免工具链启动时被启动页拖慢。
                    onFinished = {
                        startupSplashVisible = false
                        ProfilePrefs.noteApplicationOpened(this@MainActivity)
                        // 当前页的 ViewModel 已在启动页期间加载；其余主页的预热放到淡出后，
                        // 避免它们的构造与首帧抢主线程。
                        preloadMainPages()
                        checkFirstLaunch()
                        handleCustomFoodEditorIntent()
                        handleFoodDetailIntent()
                        handleAgpPreviewImportIntent()
                    },
                ) {
                    MainShell()
                }
            }
        }
        // 首帧只画启动页；其余主页预热由启动页 onFinished 触发，避免和首帧抢主线程。

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (startupSplashVisible) return
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
    }

    private fun handleAgpPreviewImportIntent() {
        if (!intent.getBooleanExtra(EXTRA_IMPORT_AGP_PREVIEW, false)) return
        lifecycleScope.launch {
            val importer = AgpPreviewImporter(this@MainActivity)
            val sources = withContext(Dispatchers.IO) { importer.loadSources() }
            if (sources.isEmpty()) {
                finishAgpPreviewImport(importer, Result.failure(IllegalStateException("No blood glucose sources configured")))
                return@launch
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle("选择血糖值来源")
                .setItems(sources.map(BloodGlucoseSource::note).toTypedArray()) { _, which ->
                    lifecycleScope.launch {
                        val result = runCatching { withContext(Dispatchers.IO) { importer.importPending(sources[which].id) } }
                        finishAgpPreviewImport(importer, result)
                    }
                }
                .setOnCancelListener {
                    lifecycleScope.launch { finishAgpPreviewImport(importer, Result.failure(IllegalStateException("Blood glucose source selection cancelled"))) }
                }
                .show()
        }
    }

    private suspend fun finishAgpPreviewImport(importer: AgpPreviewImporter, result: Result<Int>) {
        withContext(Dispatchers.IO) {
            importer.writeResult(
                result.fold(
                    onSuccess = { "SUCCESS:$it" },
                    onFailure = { "FAILED:${it.message ?: it.javaClass.simpleName}" },
                ),
            )
        }
        Toast.makeText(
            this,
            result.fold({ "AGP 导入完成：新增 $it 条血糖记录" }, { "AGP 导入失败：${it.message}" }),
            Toast.LENGTH_LONG,
        ).show()
    }


    private fun handleCustomFoodEditorIntent() {
        val kindName = intent.getStringExtra(EXTRA_OPEN_NUTRITION_EDITOR_KIND) ?: return
        val kind = FoodKind.entries.firstOrNull { it.name == kindName } ?: return
        selectedRoute = ROUTE_NUTRITION
        val userId = ProfilePrefs.getCurrentUserId(this)
        val customRepository = UserCustomFoodRepository.fromContext(this)
        val idsBefore = customRepository.loadDtos().map { it.id }.toSet()
        nutritionViewModel.openEditor(kind)
        lifecycleScope.launch {
            nutritionViewModel.state.map { it.editor }
                .distinctUntilChanged()
                .first { it == null }
            val createdId = customRepository.loadDtos().map { it.id }.toSet()
                .filterNot { idsBefore.contains(it) }
                .firstOrNull()
            if (createdId != null && ProfilePrefs.getCurrentUserId(this@MainActivity) == userId) {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_CREATED_CUSTOM_FOOD_ID, createdId))
            }
            finish()
        }
    }

    private fun handleFoodDetailIntent() {
        val foodId = intent.getStringExtra(EXTRA_OPEN_NUTRITION_DETAIL_ID) ?: return
        foodDetailMode = true
        selectedRoute = ROUTE_NUTRITION
        val userId = ProfilePrefs.getCurrentUserId(this)
        lifecycleScope.launch {
            val foods = nutritionViewModel.state.map { it.foods }.distinctUntilChanged().first { it.isNotEmpty() }
            val food = foods.firstOrNull { it.id == foodId }
            if (food == null) {
                finish()
                return@launch
            }
            nutritionViewModel.openFood(food)
            nutritionViewModel.state.map { it.selectedFood }.distinctUntilChanged().first { it == null }
            if (ProfilePrefs.getCurrentUserId(this@MainActivity) == userId) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ProfilePrefs.noteCurrentUserActivity(this)
        try {
            window.decorView.windowInsetsController?.show(AndroidWindowInsets.Type.systemBars())
        } catch (_: Exception) {
        }
        profileViewModel.refresh()
        recordViewModel.refresh()
        nutritionViewModel.refreshDiseaseRisk()
    }

    override fun onDestroy() {
        restoreSoftInputMode()
        super.onDestroy()
    }

    @Composable
    private fun MainShell() {
        val nutritionState by nutritionViewModel.state.collectAsState()
        val showBottomNavigation = selectedRoute != ROUTE_NUTRITION ||
            (!foodDetailMode && nutritionState.selectedFood == null && nutritionState.comparisonReturnTarget == null && nutritionState.editor == null)
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
                        enabled = !isMainPageTransitionRunning,
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
        val saveableStateHolder = rememberSaveableStateHolder()
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        AnimatedPageContent(
            targetState = selectedRoute,
            modifier = modifier,
            direction = { initialRoute, targetRoute ->
                navItems.indexOfFirst { it.route == targetRoute } -
                    navItems.indexOfFirst { it.route == initialRoute }
            },
        ) { route ->
            saveableStateHolder.SaveableStateProvider(route) {
            when (route) {
                ROUTE_NUTRITION -> {
                    val nutritionState by nutritionViewModel.state.collectAsState()
                    if (foodDetailMode && nutritionState.selectedFood == null) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        NutritionScreen(viewModel = nutritionViewModel, modifier = Modifier.fillMaxSize())
                    }
                }
                ROUTE_RECORD -> {
                    val uiState by recordViewModel.uiState.collectAsState()
                    RecordScreen(
                        uiState = uiState,
                        onActionClick = ::handleRecordAction,
                        onAddActionClick = ::handleRecordAddAction,
                        onQueryChange = recordViewModel::setQuery,
                        onCommitSearch = recordViewModel::commitSearch,
                        onRemoveSearchHistory = recordViewModel::removeSearchHistory,
                        onClearSearchHistory = recordViewModel::clearSearchHistory,
                        onOpenRecentAction = ::handleRecordAction,
                        onRemoveRecentAction = recordViewModel::removeRecentAction,
                        onClearRecentActions = recordViewModel::clearRecentActions,
                        modifier = Modifier.fillMaxSize(),
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
                        archiveController = requireNotNull(archiveController),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ROUTE_TEST -> {
                    val isVerified by testAccessViewModel.isVerified.collectAsState()
                    if (isVerified) {
                        when (testPage) {
                            TestPage.Landing -> TestLandingScreen({ testPage = TestPage.Commands }, { testPage = TestPage.Features }, { testPage = TestPage.CommonUi }, { testPage = TestPage.CrossSection }, Modifier.fillMaxSize())
                            TestPage.Commands -> TestGmScreen(::addTestHeightRecord, ::addTestWeightRecord, ::addTestMedicationRecord, ::addTestNutritionFoods, ::addYesterdayGlucoseSeries, ::addTodayGlucoseSeries, ::addTodayWaterRecords, ::addTestSearchHistories, { testPage = TestPage.Landing }, Modifier.fillMaxSize())
                            TestPage.Features -> ComponentsPreviewScreen(onBack = { testPage = TestPage.Landing }, includeStatusBarPadding = false)
                            TestPage.CommonUi -> CommonUiTestScreen(commonUiTestCategory, { commonUiTestCategory = it }, { if (commonUiTestCategory == null) testPage = TestPage.Landing else commonUiTestCategory = null }, Modifier.fillMaxSize())
                            TestPage.CrossSection -> CrossSectionUiTestScreen(onBack = { testPage = TestPage.Landing }, Modifier.fillMaxSize())
                        }
                    } else {
                        TestAccessScreen(
                            onCancel = ::returnFromTest,
                            onVerify = testAccessViewModel::verify,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            }
        }
    }

    private fun preloadMainPages() {
        pagePreloader.preloadData(ROUTE_NUTRITION) { nutritionViewModel.state }
        pagePreloader.preloadData(ROUTE_RECORD) { recordViewModel.uiState }
        pagePreloader.preloadData(ROUTE_PROFILE) { profileViewModel.refresh() }
    }

    private fun restoredRoute(): String {
        val index = TabPersistence.loadIndex(this, MAIN_NAV_KEY, 0)
        return navItems.getOrNull(index)?.route ?: ROUTE_NUTRITION
    }

    private fun checkFirstLaunch() {
        showOnboarding = AppPrefs.isFirstLaunch(this)
    }

    /** 外部工具直接打开编辑 / 详情 / AGP 导入时跳过启动页，避免多等 1.2 秒。 */
    private fun hasExternalLaunchIntent(): Boolean =
        intent.hasExtra(EXTRA_OPEN_NUTRITION_EDITOR_KIND) ||
            intent.hasExtra(EXTRA_OPEN_NUTRITION_DETAIL_ID) ||
            intent.getBooleanExtra(EXTRA_IMPORT_AGP_PREVIEW, false)

    private fun switchTab(route: String) {
        if (route == selectedRoute || isMainPageTransitionRunning) return
        val index = navItems.indexOfFirst { it.route == route }
        if (index == -1) return

        // 离开「我的」时收起导入/导出页：否则切回来会停在半路，用户以为入口没了。
        if (route != ROUTE_PROFILE) archiveController?.close()

        if (route == ROUTE_TEST) {
            routeBeforeTest = selectedRoute
            testPage = TestPage.Landing
            commonUiTestCategory = null
            applyTestSoftInputMode()
        } else {
            restoreSoftInputMode()
            TabPersistence.saveIndex(this, MAIN_NAV_KEY, index)
        }
        Log.d("MainNavigation", "switch $selectedRoute -> $route")
        selectedRoute = route
        isMainPageTransitionRunning = true
        window.decorView.postDelayed(
            { isMainPageTransitionRunning = false },
            PAGE_ENTER_DURATION_MILLIS.toLong(),
        )
    }

    private fun returnFromTest() {
        switchTab(routeBeforeTest)
    }

    private fun addTestHeightRecord(count: Int) = addTestBodyRecords(isWeight = false, count = count)

    private fun addTestWeightRecord(count: Int) = addTestBodyRecords(isWeight = true, count = count)

    private fun addTestBodyRecords(isWeight: Boolean, count: Int) {
        ProfilePrefs.createDefaultIfEmpty(this)
        val random = kotlin.random.Random(System.nanoTime())
        val now = System.currentTimeMillis()
        val records = List(count) {
            val timestamp = now - random.nextLong(TEST_DATA_RANGE_MILLIS)
            BodyRecord(
                date = formatBodyRecordDateTime(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()),
                value = if (isWeight) random.nextDouble(52.0, 82.0).toFloat() else random.nextDouble(150.0, 185.0).toFloat(),
                unit = if (isWeight) "kg" else "cm",
                recordedAtMillis = timestamp,
            )
        }
        if (isWeight) {
            BodyMetricsRepository.current(this).update { metrics ->
                metrics.copy(weightRecords = (metrics.weightRecords + records).sortedBy { it.recordedAtMillis })
            }
        } else {
            BodyMetricsRepository.current(this).update { metrics ->
                metrics.copy(heightRecords = (metrics.heightRecords + records).sortedBy { it.recordedAtMillis })
            }
        }
        profileViewModel.refresh()
        recordViewModel.refresh()
        Toast.makeText(this, if (isWeight) R.string.test_gm_weight_added else R.string.test_gm_height_added, Toast.LENGTH_SHORT).show()
    }

    private fun addTestMedicationRecord(count: Int) {
        ProfilePrefs.createDefaultIfEmpty(this)
        val random = kotlin.random.Random(System.nanoTime())
        val now = System.currentTimeMillis()
        repeat(count) { index ->
            val item = MedicationCatalogItem(
                id = "test_medication_${now}_$index",
                name = "测试药品 ${random.nextInt(100, 1000)}",
                defaultDoseValue = random.nextInt(1, 3).toFloat(),
                defaultDoseUnit = getString(R.string.test_gm_medication_unit),
                defaultMethod = getString(R.string.test_gm_medication_method),
            )
            MedicationPrefs.upsertCatalogItem(this, item)
            MedicationPrefs.addRecord(this, MedicationRecord(
                id = "test_${now}_$index",
                timestamp = now - random.nextLong(TEST_DATA_RANGE_MILLIS),
                medicationName = item.name,
                medicationId = item.id,
                doseValue = item.defaultDoseValue,
                doseUnit = item.defaultDoseUnit,
                specValue = item.specValue,
                specUnitCategory = item.specUnitCategory,
                specUnitId = item.specUnitId,
                method = item.defaultMethod,
            ))
        }
        recordViewModel.refresh()
        Toast.makeText(this, R.string.test_gm_medication_added, Toast.LENGTH_SHORT).show()
    }

    private fun addTestNutritionFoods(count: Int) {
        ProfilePrefs.createDefaultIfEmpty(this)
        val random = kotlin.random.Random(System.nanoTime())
        val dtos = List(count) { index ->
            val suffix = "${System.currentTimeMillis()}_${random.nextInt(10_000, 100_000)}_$index"
            val ingredientId = "custom:test_ingredient_$suffix"
            listOf(
                FoodDto(
                id = ingredientId,
                kind = "ingredient",
                names = mapOf("zh" to listOf("测试食材 ${index + 1}"), "en" to listOf("Test ingredient ${index + 1}")),
                categoryTags = listOf("food.vegetable"),
                nutritionTables = mapOf("standard.100g_edible" to FoodNutrientTableDto(
                    FoodQuantityDto(100.0, "weight", "g"),
                    mapOf(
                        "ENERGY" to FoodAmountDto(34.0, "energy", "kcal"),
                        "PROTEIN" to FoodAmountDto(2.8, "weight", "g"),
                        "FAT" to FoodAmountDto(0.4, "weight", "g"),
                        "CHO" to FoodAmountDto(6.6, "weight", "g"),
                    ),
                )),
                healthMetrics = FoodHealthMetricsDto(
                    glycemicIndex = FoodMetricDto(15.0, "GI"),
                    glycemicLoadPer100g = FoodMetricDto(1.0, "GL"),
                    inflammatoryPotential = FoodMetricDto(-0.4, "DII"),
                ),
                ),
                FoodDto(
                id = "custom:test_food_$suffix",
                kind = "food",
                names = mapOf("zh" to listOf("测试食物 ${index + 1}"), "en" to listOf("Test food ${index + 1}")),
                categoryTags = listOf("food.vegetable"),
                derivedFrom = FoodDerivationDto(ingredientId, "steamed"),
                ),
                FoodDto(
                id = "custom:test_dish_$suffix",
                kind = "dish",
                names = mapOf("zh" to listOf("测试菜肴 ${index + 1}"), "en" to listOf("Test dish ${index + 1}")),
                components = listOf(DishComponentDto(ingredientId, FoodQuantityDto(200.0, "weight", "g"))),
                cuisine = "chinese",
                servesPeople = 2,
                recipeSteps = listOf(RecipeStepDto("清洗并蒸熟测试食材", random.nextInt(5, 21))),
                ),
            )
        }.flatten()
        nutritionViewModel.addTestFoods(dtos)
        Toast.makeText(this, R.string.test_gm_nutrition_added, Toast.LENGTH_SHORT).show()
    }

    private fun addTestSearchHistories(count: Int) {
        ProfilePrefs.createDefaultIfEmpty(this)
        val random = kotlin.random.Random(System.nanoTime())
        val entries = buildSet {
            while (size < count) add("测试搜索词 ${random.nextInt(1000, 10_000)}")
        }.toList()
        val encoded = serializeSearchHistory(entries)
        UserPrefs.current(this).apply {
            putString("record_search_history_v1", encoded)
            putString("nutrition_search_history_v1", encoded)
        }
        recordViewModel.refresh()
        nutritionViewModel.refreshUser()
        Toast.makeText(this, "已为当前用户添加 $count 条随机搜索历史", Toast.LENGTH_SHORT).show()
    }

    private fun addYesterdayGlucoseSeries(count: Int) = addTestGlucoseSeries(daysAgo = 1, count = count, messageRes = R.string.test_gm_yesterday_glucose_added)
    private fun addTodayGlucoseSeries(count: Int) = addTestGlucoseSeries(daysAgo = 0, count = count, messageRes = R.string.test_gm_today_glucose_added)

    private fun addTodayWaterRecords(count: Int) {
        ProfilePrefs.createDefaultIfEmpty(this)
        val repository = WaterRepository.fromContext(this)
        val random = kotlin.random.Random(System.nanoTime())
        val dayStart = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val drinks = listOf(
            "food:water:drinking" to "饮用水",
            "food:beverage:tea" to "茶水",
            "food:beverage:milk_tea" to "奶茶",
        )
        repeat(count) { index ->
            val drink = drinks[index % drinks.size]
            repository.add(WaterRecord(
                id = "test_water_${System.nanoTime()}_$index",
                timestamp = dayStart + random.nextLong(24 * 60 * 60 * 1000L),
                beverageId = drink.first,
                beverageName = drink.second,
                volumeMl = random.nextInt(150, 451).toDouble(),
            ))
        }
        recordViewModel.refresh()
        Toast.makeText(this, "已为当前用户添加 $count 条随机今日饮水记录", Toast.LENGTH_SHORT).show()
    }

    private fun addTestGlucoseSeries(daysAgo: Long, count: Int, messageRes: Int) {
        ProfilePrefs.createDefaultIfEmpty(this)
        val dayStart = java.time.LocalDate.now().minusDays(daysAgo).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val repository = BloodGlucoseRepository.fromContext(this)
        val random = kotlin.random.Random(System.nanoTime())
        val records = repository.load() + (0 until count).map { index ->
            val minutes = random.nextInt(24 * 60)
            val hour = minutes / 60.0
            val mealResponse = listOf(8.0, 13.0, 19.0).sumOf { mealHour ->
                2.4 * kotlin.math.exp(-((hour - mealHour) * (hour - mealHour)) / 1.8)
            }
            val circadian = 0.35 * kotlin.math.sin((hour - 5.0) * Math.PI / 12.0)
            val value = ((5.25 + mealResponse + circadian + random.nextDouble(-0.28, 0.28)) * 10.0).toInt() / 10.0
            BloodGlucoseRecord("test_glucose_${daysAgo}_${System.nanoTime()}_$index", dayStart + minutes * 60_000L, value, null, 0, "test")
        }
        repository.save(records)
        recordViewModel.refresh()
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun handleRecordAction(actionId: RecordActionId) {
        recordViewModel.recordActionOpened(actionId)
        when (actionId) {
            RecordActionId.Height -> openHeightDetail()
            RecordActionId.Weight -> openWeightDetail()
            RecordActionId.BloodGlucose -> startActivity(Intent(this, BloodGlucoseActivity::class.java))
            RecordActionId.BloodPressure -> startActivity(Intent(this, BloodPressureActivity::class.java))
            RecordActionId.Disease -> startActivity(Intent(this, DiseaseRecordActivity::class.java))
            RecordActionId.Water -> startActivity(Intent(this, WaterRecordActivity::class.java))
            RecordActionId.Medication -> startActivity(Intent(this, MedicationListActivity::class.java))
            RecordActionId.Container -> startActivity(Intent(this, ContainerRecordActivity::class.java))
            RecordActionId.Waist -> openCircumferenceDetail()
            RecordActionId.Sleep -> startActivity(Intent(this, SleepRecordActivity::class.java))
            RecordActionId.Diet -> startActivity(Intent(this, DietRecordActivity::class.java))
            RecordActionId.Price -> startActivity(Intent(this, PriceRecordActivity::class.java))
            RecordActionId.Period,
            RecordActionId.Exercise,
            RecordActionId.Bowel,
            RecordActionId.Habit,
            RecordActionId.Feeling,
            RecordActionId.BloodType,
            RecordActionId.Allergy,
            RecordActionId.BodyFat,
            RecordActionId.Teeth,
            RecordActionId.Vision,
            RecordActionId.Hearing,
            RecordActionId.HeartRate,
            RecordActionId.Temperature,
            -> Unit
        }
    }

    private fun handleRecordAddAction(actionId: RecordActionId) {
        recordViewModel.recordActionOpened(actionId)
        when (actionId) {
            RecordActionId.Height -> openBodyMetricEditor(isHeight = true)
            RecordActionId.Weight -> openBodyMetricEditor(isHeight = false)
            RecordActionId.BloodGlucose -> startActivity(Intent(this, BloodGlucoseActivity::class.java).putExtra(BloodGlucoseActivity.EXTRA_OPEN_EDITOR, true))
            RecordActionId.BloodPressure -> startActivity(Intent(this, BloodPressureActivity::class.java).putExtra(BloodPressureActivity.EXTRA_OPEN_EDITOR, true))
            RecordActionId.Waist -> openCircumferenceDetail(selectMetricForNewRecord = true)
            RecordActionId.Medication -> openMedicationRecord()
            RecordActionId.Water -> startActivity(Intent(this, WaterRecordActivity::class.java).putExtra(WaterRecordActivity.EXTRA_OPEN_EDITOR, true))
            RecordActionId.Container -> startActivity(Intent(this, ContainerRecordActivity::class.java).putExtra(ContainerRecordActivity.EXTRA_OPEN_EDITOR, true))
            RecordActionId.Sleep -> startActivity(Intent(this, SleepRecordActivity::class.java).putExtra(SleepRecordActivity.EXTRA_OPEN_EDITOR, true))
            RecordActionId.Diet -> startActivity(Intent(this, DietRecordActivity::class.java).putExtra(DietRecordActivity.EXTRA_OPEN_EDITOR, true))
            RecordActionId.Price -> startActivity(Intent(this, PriceRecordActivity::class.java).putExtra(PriceRecordActivity.EXTRA_OPEN_EDITOR, true))
            else -> Unit
        }
    }

    private fun openBodyMetricEditor(isHeight: Boolean) {
        val category = if (isHeight) UnitCategoryType.Length else UnitCategoryType.Weight
        val unit = AppPrefs.getUnit(this, category.id, category.defaultUnitId)
        val intent = Intent(this, BodyMetricRecordActivity::class.java)
            .putExtra(BodyMetricRecordActivity.EXTRA_IS_HEIGHT, isHeight)
            .putExtra(BodyMetricRecordActivity.EXTRA_UNIT_ID, unit)
            .putExtra(BodyMetricRecordActivity.EXTRA_CATEGORY, category.id)
        if (isHeight) heightRecordLauncher.launch(intent) else weightRecordLauncher.launch(intent)
    }

    private fun openMedicationRecord() {
        if (MedicationPrefs.getCatalog(this).isEmpty()) {
            Toast.makeText(this, R.string.medication_catalog_empty, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, MedicationRecordActivity::class.java))
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

    private fun openCircumferenceDetail(selectMetricForNewRecord: Boolean = false) {
        val profile = ProfilePrefs.load(this)
        circumferenceDetailLauncher.launch(Intent(this, CircumferenceDetailActivity::class.java).apply {
            putExtra(CircumferenceDetailActivity.EXTRA_RECORDS, java.util.HashMap(profile.circumferenceRecords.mapValues { ArrayList(it.value) }))
            putExtra(CircumferenceDetailActivity.EXTRA_SELECT_METRIC_FOR_NEW_RECORD, selectMetricForNewRecord)
        })
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun androidx.activity.result.ActivityResult.bodyRecordsResult(): List<BodyRecord>? {
        if (resultCode != RESULT_OK) return null
        return data?.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>
    }

    @Suppress("DEPRECATION")
    private fun androidx.activity.result.ActivityResult.bodyRecordResult(): BodyRecord? {
        if (resultCode != RESULT_OK) return null
        return data?.getSerializableExtra(BodyMetricRecordActivity.EXTRA_RECORD) as? BodyRecord
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

    companion object {
        const val MAIN_NAV_KEY = "main_nav"
        const val BACK_EXIT_WINDOW_MS = 2_000L
        const val TEST_DATA_RANGE_MILLIS = 90L * 24 * 60 * 60 * 1_000
        const val ROUTE_NUTRITION = "nutrition"
        const val ROUTE_RECORD = "record"
        const val ROUTE_PROFILE = "profile"
        const val ROUTE_TEST = "test"
        const val EXTRA_OPEN_NUTRITION_EDITOR_KIND = "open_nutrition_editor_kind"
        const val EXTRA_OPEN_NUTRITION_DETAIL_ID = "open_nutrition_detail_id"
        const val EXTRA_CREATED_CUSTOM_FOOD_ID = "created_custom_food_id"
        const val EXTRA_IMPORT_AGP_PREVIEW = "import_agp_preview"
    }

    private enum class TestPage { Landing, Commands, Features, CommonUi, CrossSection }
}
