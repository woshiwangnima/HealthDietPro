package com.woshiwangnima.healthdietpro.ui.widget.tab.demo

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.ui.widget.tab.FilterBar
import com.woshiwangnima.healthdietpro.ui.widget.tab.MultiLevelTabCoordinator
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabItem
import com.woshiwangnima.healthdietpro.ui.widget.tab.ToggleBar

class DemoTabsActivity : BaseBackActivity() {

    override fun getTitleText(): String = "Tab Demo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_tabs)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setupToolbar(toolbar)

        val bottomBar = findViewById<ToggleBar>(R.id.bottomBar)
        val leftBar = findViewById<ToggleBar>(R.id.leftBar)
        val topFilter = findViewById<FilterBar>(R.id.topFilter)
        val stateText = findViewById<TextView>(R.id.stateText)

        bottomBar.maxVisible = 4
        bottomBar.setTabs(listOf(
            TabItem(label = "首页"), TabItem(label = "分类"), TabItem(label = "发布"),
            TabItem(label = "消息"), TabItem(label = "我的")
        ))

        leftBar.orientation = LinearLayout.VERTICAL
        leftBar.setTabs(listOf(
            TabItem(label = "概览"), TabItem(label = "详情"), TabItem(label = "设置"), TabItem(label = "其它")
        ))

        topFilter.setTabs(listOf(
            TabItem(label = "全部"), TabItem(label = "A"), TabItem(label = "B"),
            TabItem(label = "C"), TabItem(label = "D"), TabItem(label = "E")
        ))

        val coordinator = MultiLevelTabCoordinator(this, "demo")
        coordinator.registerLevel(1, bottomBar)
        coordinator.registerLevel(2, leftBar)
        coordinator.registerFilter(3, topFilter)

        fun renderState(l1: Int, l2: Int, l3: Set<Int>) {
            stateText.text = "L1(底): $l1\nL2(左): $l2\nL3(顶)筛选: $l3"
        }
        coordinator.onLevelChanged = { level, sel ->
            val l1 = bottomBar.selectedIndex
            val l2 = leftBar.selectedIndex
            val l3 = topFilter.selected
            renderState(l1, l2, l3)
        }
        renderState(bottomBar.selectedIndex, leftBar.selectedIndex, topFilter.selected)
    }
}