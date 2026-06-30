package com.woshiwangnima.healthdietpro

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.config.NavConfig
import com.woshiwangnima.healthdietpro.databinding.ActivityMainBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.ui.nutrition.NutritionFragment
import com.woshiwangnima.healthdietpro.ui.profile.ProfileEditActivity
import com.woshiwangnima.healthdietpro.ui.profile.ProfileFragment
import com.woshiwangnima.healthdietpro.ui.record.RecordFragment
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private val fragments = listOf(
        NutritionFragment(),
        RecordFragment(),
        ProfileFragment()
    )

    private var currentIndex = -1
    private var onboardingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        AppPrefs.markFirstLaunchComplete(this)
        switchTab(2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.main.applySystemBarInsets()

        setupNavSizing()
        setupNavClickListeners()
        switchTab(0)
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        if (!AppPrefs.isFirstLaunch(this)) return
        AlertDialog.Builder(this)
            .setTitle("欢迎使用健康饮食计划")
            .setMessage("首次使用，请先填写您的个人信息，以便为您提供个性化的饮食建议。")
            .setCancelable(false)
            .setPositiveButton("开始填写") { _, _ ->
                val intent = Intent(this, ProfileEditActivity::class.java)
                onboardingLauncher.launch(intent)
            }
            .show()
    }

    private fun setupNavSizing() {
        val screenHeight = resources.displayMetrics.heightPixels
        val density = resources.displayMetrics.density
        val barHeightDp = (screenHeight * NavConfig.BOTTOM_BAR_HEIGHT_FRACTION / density).toInt()

        val barParams = binding.navBar.layoutParams
        barParams.height = dpToPxF(barHeightDp.toFloat()).toInt()
        binding.navBar.layoutParams = barParams

        val navItemIds = listOf(
            binding.navNutrition.id,
            binding.navRecord.id,
            binding.navProfile.id
        )
        val navItems = navItemIds.map { findViewById<android.view.View>(it) }
        val isOdd = navItems.size % 2 == 1

        if (isOdd) {
            val centerIndex = navItems.size / 2
            val centerItem = navItems[centerIndex]

            val sidePadding = 6f + 8f
            val sideContentHeight = 24f + 4f + 14f + sidePadding
            val centerContentHeight = 52f + 2f + 14f

            val sideTextY = (barHeightDp - sideContentHeight) / 2f + 6f + 24f + 4f
            val centerTextY = (barHeightDp - centerContentHeight) / 2f + 52f + 2f

            centerItem.translationY = dpToPxF(sideTextY - centerTextY)

            val params = centerItem.layoutParams
            params.height = dpToPxF((barHeightDp * NavConfig.CENTER_BUTTON_SCALE).toFloat()).toInt()
            centerItem.layoutParams = params
        }
    }

    private fun setupNavClickListeners() {
        binding.navNutrition.setOnClickListener { switchTab(0) }
        binding.navRecord.setOnClickListener { switchTab(1) }
        binding.navProfile.setOnClickListener { switchTab(2) }
    }

    private fun switchTab(index: Int) {
        if (index == currentIndex) return
        currentIndex = index

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragments[index])
            .commit()

        updateNavVisuals(index)
    }

    private fun updateNavVisuals(selectedIndex: Int) {
        val navIds = listOf(R.id.nav_nutrition, R.id.nav_record, R.id.nav_profile)
        for (i in navIds.indices) {
            val nav = findViewById<android.view.View>(navIds[i])
            nav.isSelected = (i == selectedIndex)
        }

        val icons = listOf(
            binding.navNutritionIcon,
            binding.navRecordIcon,
            binding.navProfileIcon
        )
        val labels = listOf(
            binding.navNutritionLabel,
            binding.navRecordLabel,
            binding.navProfileLabel
        )

        val selectedColor = ContextCompat.getColor(this, R.color.primary)
        val defaultColor = ContextCompat.getColor(this, R.color.on_surface_variant)

        icons.forEachIndexed { i, icon ->
            icon.setColorFilter(if (i == selectedIndex) selectedColor else defaultColor)
        }

        labels.forEachIndexed { i, label ->
            label.setTextColor(if (i == selectedIndex) selectedColor else defaultColor)
            label.setTypeface(null, if (i == selectedIndex) Typeface.BOLD else Typeface.NORMAL)
            label.setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.text_size_micro)
            )
        }

        updateCenterIconBackground(selectedIndex)
    }

    private fun updateCenterIconBackground(index: Int) {
        val icon = binding.navRecordIcon
        icon.background = if (index == 1) {
            ContextCompat.getDrawable(this, R.drawable.bg_nav_center_selected)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bg_nav_center)
        }
    }

    private fun dpToPxF(dp: Float): Float = dp * resources.displayMetrics.density
}
