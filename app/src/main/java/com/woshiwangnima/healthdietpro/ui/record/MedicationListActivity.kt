package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.databinding.ActivityMedicationListBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class MedicationListActivity : BaseBackActivity() {

    override fun getTitleText(): String = "记用药"

    private lateinit var binding: ActivityMedicationListBinding
    private var currentTab = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        setupTabBar()
        switchTab(AppPrefs.getMedicationTab(this))
        /*
        binding.tabBar.setTabs(listOf(
            TabItem(R.drawable.ic_notification, "提醒"),
            TabItem(R.drawable.ic_list, "记录")
        ))
        binding.tabBar.applyEnlargedTabHeight(hasIcon = true)
        binding.tabBar.restore("tab_medication", 0)
        binding.tabBar.listener = { idx, _ -> switchTab(idx) }
        switchTab(binding.tabBar.selectedIndex)
        */
    }

    private fun setupTabBar() {
        val items = listOf(
            DetailTabItem("0", R.string.detail_tab_reminder, R.drawable.ic_notification),
            DetailTabItem("1", R.string.detail_tab_records, R.drawable.ic_list),
        )
        binding.tabBar.setContent {
            HealthDietProTheme {
                var selectedTab by remember {
                    mutableIntStateOf(AppPrefs.getMedicationTab(this@MedicationListActivity))
                }
                DetailTabBar(
                    items = items,
                    selectedId = selectedTab.toString(),
                    onSelected = { item ->
                        val index = item.id.toInt()
                        selectedTab = index
                        AppPrefs.setMedicationTab(this@MedicationListActivity, index)
                        switchTab(index)
                    },
                )
            }
        }
    }

    private fun switchTab(index: Int) {
        if (index == currentTab) return
        currentTab = index
        when (index) {
            0 -> showReminderTab()
            1 -> showRecordListTab()
        }
    }

    private fun showReminderTab() {
        binding.contentFrame.removeAllViews()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 0, 32, 0)
        }
        root.addView(TextView(this).apply {
            text = "提醒功能"
            textSize = 18f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "暂未开放，敬请期待"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        })
        binding.contentFrame.addView(root)
    }

    private fun showRecordListTab() {
        binding.contentFrame.removeAllViews()
        val fragment = MedicationListFragment.newInstance().also {
            it.onRecordsChanged = { /* 数据变化后无额外操作 */ }
        }
        supportFragmentManager.beginTransaction()
            .replace(com.woshiwangnima.healthdietpro.R.id.contentFrame, fragment)
            .commit()
    }
}
