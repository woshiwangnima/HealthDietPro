package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.ConfirmDialog
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab

internal enum class CommonUiTestCategory(val chineseName: String, val className: String) {
    Dropdown("下拉选择", "AppDropdownField"),
    ActionButton("操作按钮", "AppIconTextButton"),
    ConfirmDialog("确认对话框", "ConfirmDialog"),
    DataTable("数据表格", "AppDataTable"),
    Chart("图表", "ComposeBaseChart"),
    SegmentedTabs("等宽分段标签", "EqualWidthSegmentedTabs"),
}

@Composable
internal fun CommonUiTestScreen(
    category: CommonUiTestCategory?,
    onCategorySelected: (CommonUiTestCategory) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (category == null) {
        CommonUiCategoryList(onCategorySelected, onBack, modifier)
    } else {
        CommonUiVariantScreen(category, onBack, modifier)
    }
}

@Composable
private fun CommonUiCategoryList(
    onCategorySelected: (CommonUiTestCategory) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    BaseScreen(title = "通用UI功能测试", onBack = onBack) { padding -> Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("长按类名可复制 Kotlin 类名。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        CommonUiTestCategory.entries.forEach { category ->
            CopyableClassName(category.chineseName, category.className)
            AppIconTextButton("测试${category.chineseName}的不同配置", R.drawable.ic_nav_test, { onCategorySelected(category) }, Modifier.fillMaxWidth())
            HorizontalDivider()
        }
    } }
}

@Composable
private fun CommonUiVariantScreen(category: CommonUiTestCategory, onBack: () -> Unit, modifier: Modifier) {
    var selectedOption by remember { mutableStateOf("默认选项") }
    var showConfirm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    BaseScreen(title = "${category.chineseName}配置测试", onBack = onBack) { padding -> Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CopyableClassName(category.chineseName, category.className)
        when (category) {
            CommonUiTestCategory.Dropdown -> {
                Text("默认下拉框")
                AppDropdownField("普通选项", selectedOption, listOf("默认选项", "第二选项", "第三选项").map { AppDropdownOption(it, it) }, { selectedOption = it.id })
                Text("带分割线的下拉框")
                AppDropdownField("带分割线选项", selectedOption, listOf("默认选项", "第二选项", "第三选项").map { AppDropdownOption(it, it) }, { selectedOption = it.id }, showOptionDividers = true)
                AppDropdownField("禁用下拉框", "不可选择", emptyList(), {}, enabled = false)
            }
            CommonUiTestCategory.ActionButton -> {
                AppIconTextButton("主要操作按钮", R.drawable.ic_add, {}, Modifier.fillMaxWidth())
                AppOutlinedIconTextButton("次要操作按钮", R.drawable.ic_settings, {}, Modifier.fillMaxWidth())
                AppIconTextButton("禁用操作按钮", R.drawable.ic_save, {}, Modifier.fillMaxWidth(), enabled = false)
            }
            CommonUiTestCategory.ConfirmDialog -> {
                AppIconTextButton("显示确认对话框", R.drawable.ic_help, { showConfirm = true }, Modifier.fillMaxWidth())
                if (showConfirm) ConfirmDialog("确认对话框", "这是通用确认对话框的测试内容。", "确认", "取消", { showConfirm = false }, { showConfirm = false })
            }
            CommonUiTestCategory.DataTable -> DataTableSamples()
            CommonUiTestCategory.Chart -> com.woshiwangnima.healthdietpro.common.ui.ComposeChartPreviewSamples()
            CommonUiTestCategory.SegmentedTabs -> {
                Text("选择不同标签以查看滑动指示条和文字颜色动画。")
                EqualWidthSegmentedTabs(
                    tabs = listOf(
                        EqualWidthTab(R.string.nutrition_tab_profile),
                        EqualWidthTab(R.string.nutrition_tab_ranking),
                        EqualWidthTab(R.string.nutrition_tab_estimate),
                    ),
                    selectedIndex = selectedTab,
                    onSelected = { selectedTab = it },
                )
            }
        }
    } }
}

@Composable
private fun DataTableSamples() {
    val rows = listOf("第一行示例数据" to "正常", "第二行较长的示例数据" to "警告")
    com.woshiwangnima.healthdietpro.common.ui.AppDataTable(
        rows = rows,
        columns = listOf(
            com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn<Pair<String, String>>("name", { com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText("名称") }, com.woshiwangnima.healthdietpro.common.ui.ColumnWidth.Flex(1f, 120.dp)) { com.woshiwangnima.healthdietpro.common.ui.AppDataTableText(it.first) },
            com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn<Pair<String, String>>("status", { com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText("状态") }, com.woshiwangnima.healthdietpro.common.ui.ColumnWidth.Fixed(80.dp)) { com.woshiwangnima.healthdietpro.common.ui.AppDataTableText(it.second) },
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CopyableClassName(chineseName: String, className: String) {
    val clipboard = LocalClipboardManager.current
    Text(
        text = "$chineseName · $className",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { clipboard.setText(AnnotatedString(className)) }),
    )
}
