package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.config.NavConfig
import com.woshiwangnima.healthdietpro.databinding.ActivityHeightDetailBinding
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartFragment
import com.woshiwangnima.healthdietpro.ui.profile.list.DataListFragment
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class HeightDetailActivity : BaseBackActivity() {

    private lateinit var binding: ActivityHeightDetailBinding
    private var records: MutableList<BodyRecord> = mutableListOf()
    private var currentTab = 0

    override fun getTitleText(): String = "身高历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeightDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        @Suppress("UNCHECKED_CAST")
        records = (intent.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
        val unit = intent.getStringExtra("unit") ?: "cm"
        val category = "height"

        setupBottomBar()
        switchTab(0, unit, category)
        setupTabListeners(unit, category)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })
    }

    private fun saveAndFinish() {
        intent.putExtra("records", ArrayList(records))
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setupBottomBar() {
        val screenHeight = resources.displayMetrics.heightPixels
        val density = resources.displayMetrics.density
        val barHeight = NavConfig.calculateBarHeightPx(screenHeight.toInt(), density)
        val params = binding.bottomBar.layoutParams
        params.height = barHeight
        binding.bottomBar.layoutParams = params
    }

    private fun switchTab(index: Int, unit: String, category: String) {
        currentTab = index
        val fragment: Fragment = if (index == 0) {
            ChartFragment.newInstance(ArrayList(records), unit, category)
        } else {
            DataListFragment.newInstance(ArrayList(records), unit, category, isHeight = true).also {
                it.onRecordsChanged = { this.records = it.records }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
        binding.tabChart.isSelected = index == 0
        binding.tabData.isSelected = index == 1
    }

    private fun setupTabListeners(unit: String, category: String) {
        binding.tabChart.setOnClickListener { switchTab(0, unit, category) }
        binding.tabData.setOnClickListener { switchTab(1, unit, category) }
    }
}
