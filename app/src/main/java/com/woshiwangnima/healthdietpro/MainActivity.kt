package com.woshiwangnima.healthdietpro

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityMainBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.ui.nutrition.NutritionFragment
import com.woshiwangnima.healthdietpro.ui.profile.ProfileEditActivity
import com.woshiwangnima.healthdietpro.ui.profile.ProfileFragment
import com.woshiwangnima.healthdietpro.ui.record.RecordFragment
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabItem
import com.woshiwangnima.healthdietpro.ui.widget.tab.ToggleBar
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
        binding.navBar.select(2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.main.applySystemBarInsets()

        val tabs = listOf(
            TabItem(R.drawable.ic_nav_nutrition, getString(R.string.nav_nutrition)),
            TabItem(R.drawable.ic_nav_record, getString(R.string.nav_record)),
            TabItem(R.drawable.ic_nav_profile, getString(R.string.nav_profile))
        )
        binding.navBar.setTabs(tabs)
        binding.navBar.restore("main_nav", 0)
        binding.navBar.listener = { idx, _ -> switchTab(idx) }
        switchTab(binding.navBar.selectedIndex)

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

    private fun switchTab(index: Int) {
        if (index == currentIndex) return
        currentIndex = index

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragments[index])
            .commit()
    }
}