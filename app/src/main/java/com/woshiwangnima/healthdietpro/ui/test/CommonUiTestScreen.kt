package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
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
import com.woshiwangnima.healthdietpro.common.ui.TextInputField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputKind
import com.woshiwangnima.healthdietpro.common.ui.NumericInputSpec
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.WaterGlassProgress
import com.woshiwangnima.healthdietpro.common.range.RangeBand

internal enum class CommonUiTestCategory(val chineseName: String, val className: String) {
    Dropdown("下拉选择", "AppDropdownField"),
    ActionButton("操作按钮", "AppIconTextButton"),
    ConfirmDialog("确认对话框", "ConfirmDialog"),
    DataTable("数据表格", "AppDataTable"),
    Chart("图表", "ComposeBaseChart"),
    HydrationVisuals("饮水可视化", "AnimatedDonutChart / WaterGlassProgress"),
    SegmentedTabs("等宽分段标签", "EqualWidthSegmentedTabs"),
    InputField("输入框", "TextInputField / NumericInputField"),
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
    BaseScreen(title = "通用UI功能测试", onBack = onBack, includeStatusBarPadding = false) { padding -> Column(
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
    var textValue by remember { mutableStateOf("") }
    var positiveInteger by remember { mutableStateOf("3") }
    var signedInteger by remember { mutableStateOf("-2") }
    var decimalValue by remember { mutableStateOf("5.6") }
    var boundedValue by remember { mutableStateOf("70.0") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    BaseScreen(title = "${category.chineseName}配置测试", onBack = onBack, includeStatusBarPadding = false) { padding -> Column(
        modifier = modifier.fillMaxSize().imePadding().pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus(); keyboardController?.hide() })
        }.verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
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
            CommonUiTestCategory.HydrationVisuals -> HydrationVisualSamples()
            CommonUiTestCategory.SegmentedTabs -> {
                Text("选择不同标签以查看滑动指示条和文字颜色动画。")
                EqualWidthSegmentedTabs(
                    tabs = listOf(
                        EqualWidthTab.text("概览"),
                        EqualWidthTab.text("排行"),
                        EqualWidthTab.text("估算"),
                    ),
                    selectedIndex = selectedTab,
                    onSelected = { selectedTab = it },
                )
            }
            CommonUiTestCategory.InputField -> {
                Text("文本输入：多行内容，标题在未聚焦时位于输入框内。")
                TextInputField("备注", textValue, { textValue = it }, tooltip = "点击信息图标可展开或收起字段说明。")
                Text("正整数：不允许负号，外部右侧步长为 1。")
                NumericInputField("服用次数", positiveInteger, { positiveInteger = it }, NumericInputSpec(NumericInputKind.Integer, range = RangeBand(min = 0.0, max = 12.0, value = Unit), example = "3", step = 1.0))
                Text("带符号整数：支持正负号、范围校验和步长。")
                NumericInputField("相对分钟", signedInteger, { signedInteger = it }, NumericInputSpec(NumericInputKind.Integer, allowNegative = true, range = RangeBand(min = -180.0, max = 180.0, value = Unit), example = "-30", step = 5.0))
                Text("带符号小数：范围、示例均统一显示两位精度。")
                NumericInputField("血糖值", decimalValue, { decimalValue = it }, NumericInputSpec(NumericInputKind.Decimal, allowNegative = true, range = RangeBand(min = -33.30, minInclusive = false, max = 33.30, maxInclusive = false, value = Unit), example = "5.60", decimalPlaces = 2, tooltip = "单位为 mmol/L。"))
                Text("带步长小数：外部右侧按钮按 0.1 调整，并限制范围。")
                NumericInputField("体重", boundedValue, { boundedValue = it }, NumericInputSpec(NumericInputKind.Decimal, range = RangeBand(min = 20.0, max = 300.0, value = Unit), example = "70.0", decimalPlaces = 1, step = 0.1))
            }
        }
    } }
}

@Composable
private fun HydrationVisualSamples() {
    Text("环形图会在进入页面时依次绘制饮品构成；玻璃杯液面会持续左右晃动。")
    WaterGlassProgress(
        progress = .68f,
        valueLabel = "1,700 ml",
        supportingLabel = "今日目标 2,500 ml",
        modifier = Modifier.fillMaxWidth(),
    )
    AnimatedDonutChart(
        segments = listOf(
            DonutChartSegment("water", "饮用水", 1_250f),
            DonutChartSegment("tea", "茶饮", 300f),
            DonutChartSegment("milk", "牛奶", 150f),
        ),
        centerValue = "1,700 ml",
        centerLabel = "今日记录",
        modifier = Modifier.fillMaxWidth(),
    )
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
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp),
    )
    Text("基类表格 + 拖动排序库压力演示：共 300 条复杂行。按住每行最左侧手柄拖动，跨屏时会自动滚动；该演示不写入任何业务存档。")
    ReorderableDataTableDemo()
}

private data class ReorderableDemoRow(
    val id: String,
    val beverage: String,
    val volume: String,
    val category: String,
    val updatedAt: String,
)

@Composable
private fun ReorderableDataTableDemo() {
    var rows by remember {
        mutableStateOf(
            List(300) { index ->
                ReorderableDemoRow(
                    id = "demo-$index",
                    beverage = "第 ${index + 1} 条复杂饮品预设：${listOf("冷萃咖啡", "无糖乌龙茶", "电解质饮料", "低脂牛奶", "柠檬气泡水")[index % 5]}",
                    volume = "${150 + index % 8 * 50} ${if (index % 9 == 0) "ml（含长说明）" else "ml"}",
                    category = listOf("日常饮水", "运动补水", "餐后饮品", "自定义来源")[index % 4],
                    updatedAt = "2026-08-${(index % 28 + 1).toString().padStart(2, '0')}  ${"%02d".format(index % 24)}:${"%02d".format(index % 60)}",
                )
            },
        )
    }
    com.woshiwangnima.healthdietpro.common.ui.AppDataTable(
        rows = rows,
        columns = listOf(
            com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn<ReorderableDemoRow>("beverage", { com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText("饮品与预设") }, com.woshiwangnima.healthdietpro.common.ui.ColumnWidth.Flex(1.5f, 180.dp)) { com.woshiwangnima.healthdietpro.common.ui.AppDataTableText(it.beverage, maxLines = 2) },
            com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn<ReorderableDemoRow>("volume", { com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText("容量") }, com.woshiwangnima.healthdietpro.common.ui.ColumnWidth.Fixed(120.dp)) { com.woshiwangnima.healthdietpro.common.ui.AppDataTableText(it.volume, maxLines = 2) },
            com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn<ReorderableDemoRow>("category", { com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText("分类") }, com.woshiwangnima.healthdietpro.common.ui.ColumnWidth.Fixed(120.dp)) { com.woshiwangnima.healthdietpro.common.ui.AppDataTableText(it.category, maxLines = 2) },
            com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn<ReorderableDemoRow>("updated", { com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText("更新时间") }, com.woshiwangnima.healthdietpro.common.ui.ColumnWidth.Fixed(140.dp)) { com.woshiwangnima.healthdietpro.common.ui.AppDataTableText(it.updatedAt, maxLines = 2) },
        ),
        modifier = Modifier.fillMaxWidth().height(480.dp).padding(vertical = 8.dp),
        rowKey = { _, row -> row.id },
        showPager = false,
        initialRowsPerPage = rows.size,
        reorder = com.woshiwangnima.healthdietpro.common.ui.AppDataTableReorder(
            onMove = { from, to -> rows = rows.toMutableList().apply { add(to, removeAt(from)) } },
        ),
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
