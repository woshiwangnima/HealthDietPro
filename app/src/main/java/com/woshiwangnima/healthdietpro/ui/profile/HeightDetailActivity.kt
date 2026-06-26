package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.config.NavConfig
import com.woshiwangnima.healthdietpro.databinding.ActivityHeightDetailBinding
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.ui.profile.chart.HeightChartActivity
import com.woshiwangnima.healthdietpro.ui.profile.list.DataListFragment
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class HeightDetailActivity : BaseBackActivity() {

    private lateinit var binding: ActivityHeightDetailBinding
    private var records: MutableList<BodyRecord> = mutableListOf()
    private var unit: String = UnitCategory.DEFAULT_UNIT_LENGTH
    private var category: String = UnitCategory.ID_LENGTH
    private var currentTab = -1

    override fun getTitleText(): String = "身高历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeightDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        @Suppress("UNCHECKED_CAST")
        records = (intent.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
        unit = intent.getStringExtra("unit") ?: UnitCategory.DEFAULT_UNIT_LENGTH
        category = UnitCategory.ID_LENGTH

        setupBottomBar()
        switchTab(1)
        setupTabListeners()
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

    private fun switchTab(index: Int) {
        if (index == currentTab || index == 0) return
        currentTab = index
        val fragment = DataListFragment.newInstance(ArrayList(records), unit, category, isHeight = true).also {
            it.onRecordsChanged = { this.records = it.records }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
        updateTabSelection()
    }

    private fun updateTabSelection() {
        val bgSelected = ContextCompat.getDrawable(this, R.drawable.tab_selected_bg)
        val bgDefault = ContextCompat.getDrawable(this, R.drawable.tab_default_bg)
        binding.tabChart.background = if (currentTab == 0) bgSelected else bgDefault
        binding.tabData.background = if (currentTab == 1) bgSelected else bgDefault
        binding.tabChart.isSelected = currentTab == 0
        binding.tabData.isSelected = currentTab == 1
    }

    private fun setupTabListeners() {
        binding.tabChart.setOnClickListener {
            val intent = Intent(this, HeightChartActivity::class.java).apply {
                putExtra(HeightChartActivity.EXTRA_RECORDS, ArrayList(records))
                putExtra(HeightChartActivity.EXTRA_UNIT, unit)
            }
            startActivity(intent)
        }
        binding.tabData.setOnClickListener { switchTab(1) }
    }
}
