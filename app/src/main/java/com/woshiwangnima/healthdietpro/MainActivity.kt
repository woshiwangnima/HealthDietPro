package com.woshiwangnima.healthdietpro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.AppBottomNavItem
import com.woshiwangnima.healthdietpro.common.ui.AppBottomNavigationBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.databinding.ActivityMainBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.ui.nutrition.NutritionFragment
import com.woshiwangnima.healthdietpro.ui.profile.ProfileEditActivity
import com.woshiwangnima.healthdietpro.ui.profile.ProfileFragment
import com.woshiwangnima.healthdietpro.ui.record.RecordFragment
import com.woshiwangnima.healthdietpro.ui.test.TestFragment
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabPersistence
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class MainActivity : BaseActivity(), TestFragment.TestAccessHost {

    private lateinit var binding: ActivityMainBinding

    private val navItems = listOf(
        MainNavItem(
            item = AppBottomNavItem(ROUTE_NUTRITION, R.string.nav_nutrition, R.drawable.ic_nav_nutrition),
            fragmentFactory = { NutritionFragment() },
        ),
        MainNavItem(
            item = AppBottomNavItem(ROUTE_RECORD, R.string.nav_record, R.drawable.ic_nav_record),
            fragmentFactory = { RecordFragment() },
        ),
        MainNavItem(
            item = AppBottomNavItem(ROUTE_PROFILE, R.string.nav_profile, R.drawable.ic_nav_profile),
            fragmentFactory = { ProfileFragment() },
        ),
        MainNavItem(
            item = AppBottomNavItem(ROUTE_TEST, R.string.nav_test, R.drawable.ic_nav_test),
            fragmentFactory = { TestFragment() },
        ),
    )

    private var selectedRoute by mutableStateOf(ROUTE_NUTRITION)
    private var routeBeforeTest = ROUTE_NUTRITION
    private var lastBackPressedAt = 0L
    private var onboardingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        AppPrefs.markFirstLaunchComplete(this)
        switchTab(ROUTE_PROFILE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.main.applySystemBarInsets()

        binding.navBar.setContent {
            HealthDietProTheme {
                AppBottomNavigationBar(
                    items = navItems.map { it.item },
                    selectedRoute = selectedRoute,
                    onItemClick = { switchTab(it.route) },
                )
            }
        }
        switchTab(restoredRoute())

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleDoubleBackExit()
            }
        })

        checkFirstLaunch()
    }

    override fun onResume() {
        super.onResume()
        try {
            window?.decorView?.windowInsetsController?.show(WindowInsets.Type.systemBars())
        } catch (_: Exception) {
        }
        if (::binding.isInitialized) {
            ViewCompat.requestApplyInsets(binding.main)
        }
    }

    private fun restoredRoute(): String {
        val index = TabPersistence.loadIndex(this, MAIN_NAV_KEY, 0)
        return navItems.getOrNull(index)?.item?.route ?: ROUTE_NUTRITION
    }

    private fun checkFirstLaunch() {
        if (!AppPrefs.isFirstLaunch(this)) return
        AlertDialog.Builder(this)
            .setTitle(R.string.onboarding_title)
            .setMessage(R.string.onboarding_message)
            .setCancelable(false)
            .setPositiveButton(R.string.onboarding_start) { _, _ ->
                val intent = Intent(this, ProfileEditActivity::class.java)
                onboardingLauncher.launch(intent)
            }
            .show()
    }

    private fun switchTab(route: String) {
        if (route == selectedRoute && supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            return
        }
        val index = navItems.indexOfFirst { it.item.route == route }
        if (index == -1) return

        if (route == ROUTE_TEST && selectedRoute != ROUTE_TEST) {
            routeBeforeTest = selectedRoute
        }
        selectedRoute = route
        if (route != ROUTE_TEST) {
            TabPersistence.saveIndex(this, MAIN_NAV_KEY, index)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, navItems[index].fragmentFactory())
            .commit()
    }

    override fun onTestAccessCancelled() {
        Log.d(TAG, "Test access cancelled; returning to $routeBeforeTest")
        switchTab(routeBeforeTest)
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

    private data class MainNavItem(
        val item: AppBottomNavItem,
        val fragmentFactory: () -> androidx.fragment.app.Fragment,
    )

    private companion object {
        const val TAG = "MainActivity"
        const val MAIN_NAV_KEY = "main_nav"
        const val BACK_EXIT_WINDOW_MS = 2_000L
        const val ROUTE_NUTRITION = "nutrition"
        const val ROUTE_RECORD = "record"
        const val ROUTE_PROFILE = "profile"
        const val ROUTE_TEST = "test"
    }
}
