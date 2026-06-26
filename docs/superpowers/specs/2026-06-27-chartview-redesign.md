# ChartView 重设计规范

## 概述

将体重/身高历史图表的 `BaseChartActivity` 重构为独立的 `ChartView : LinearLayout` 自定义 ViewGroup，可嵌入任意 Activity/Fragment 的 XML 布局中。渲染引擎从 MPAndroidChart 替换为纯 Canvas 自绘，彻底解决坐标系统混乱导致的各类 BUG。

## 组件结构

```
ChartView (LinearLayout vertical)
├── 控制条 (controlsRow)
│   ├── 图表样式 Spinner (折线图/贝塞尔曲线/自然样条/后置阶梯/前置阶梯)
│   ├── 时间范围 Spinner (全部/1天/1周/1月/3月/6月/1年, 按数据跨度动态过滤)
│   └── 全屏按钮 [⛶]
├── Y轴控制行 (yAxisRow)
│   └── Y轴: [0 %] ~ [100 %] (百分比输入框, 支持浮点)
├── ChartCanvas (View) — 图表内容区
│   └── Canvas 自绘: 网格→各系列曲线→坐标轴→十字线
├── 时间轴滑动区 (timelineArea)
│   ├── 进度条 + 位置滑块 (progressBarBg + thumb)
│   └── 拖拽区 + 左右箭头按钮 (dragIndicator + arrows)
└── 图例行 (legendRow) — 动态渲染，每个 series 一个图例项
```

## 绘制渲染管道

`ChartCanvas.onDraw(Canvas)` 按层级：

1. `drawBackground()` — 图表区背景色
2. `drawGridLines()` — Y轴水平网格线（NiceScale 算法自动刻度）
3. `drawSeries()` — 遍历所有 ChartSeries，每个绘制：曲线 + 数据点形状
4. `drawYAxis()` — Y轴刻度数字 + 单位标签
5. `drawXAxis()` — X轴时间标签 + 刻度线（间隔按 labelInterval 设定）
6. `drawCrosshair()` — 十字虚线 + 交点圆 + 信息气泡

## 坐标系统

用统一相对时间戳，消除坐标混乱。

- **X轴**: `chartMinX = 0` (可见窗口最左), `chartMaxX = visibleRange` (可见窗口最右)
- **Y轴**: `chartMinY` / `chartMaxY` = 用户设定百分比换算值
- **像素映射**: `screenPx = (value - axisMin) / (axisRange) * chartPixelSize`

## 数据列模型

```kotlin
enum class LineStyle { LINEAR, BEZIER, SPLINE, STEPPED_FRONT, STEPPED_BACK }

enum class LineType { SOLID, DASHED, DOTTED }

enum class PointShape { CIRCLE, TRIANGLE, SQUARE, DIAMOND, CROSS }

enum class PointFill { FILLED, HOLLOW }

data class ChartSeries(
    val points: List<DataPoint>,      // 数据点列表
    val label: String,                 // 图例标签（如 "张三 体重"）
    val color: Int,                    // 线条/数据点颜色
    val lineStyle: LineStyle,          // 折线/贝塞尔/样条/阶梯
    val lineType: LineType,            // 实线/虚线/点线
    val pointShape: PointShape,        // 数据点形状
    val pointFill: PointFill           // 实心/空心
)
```

## 图表样式（5种线型）

| 样式 | 算法 | 描述 |
|------|------|------|
| `LINEAR` | `Path.lineTo()` 逐段连线 | 折线图 |
| `BEZIER` | 三次贝塞尔插值 `Path.cubicTo()` | 平滑曲线-贝塞尔 |
| `SPLINE` | 自然三次样条插值 | 平滑曲线-自然样条（通过所有点，二阶导连续） |
| `STEPPED_FRONT` | Y = `entries[i].y` (前置保持) | 前置阶梯线 |
| `STEPPED_BACK` | Y = `entries[i+1].y` (后置保持) | 后置阶梯线 |

## 图表样式 Spinner 行为

全局样式选择下拉菜单：为所有 series 统一设置 `lineStyle`。各 series 的 `color`、`pointShape`、`pointFill` 保持独立不变。

## X轴时间标签策略

默认根据 `visibleRange` 自动选择间隔。可通过 `setLabelInterval()` 手动覆盖。

| visibleRange | 标签间隔 |
|-------------|---------|
| ≤ 1小时 | 5分钟 |
| ≤ 6小时 | 30分钟 |
| ≤ 1天 | 2小时 |
| ≤ 1周 | 1天 |
| ≤ 1月 | 3天 |
| ≤ 6月 | 1周 |
| ≤ 1年 | 1月 |
| > 1年 / 全部 | 3月 |

## Y轴刻度：NiceScale 算法

```kotlin
fun niceScale(minV: Float, maxV: Float, maxTicks: Int = 5): Pair<Float, Float> {
    val range = niceNum(maxV - minV, false)
    val step = niceNum(range / (maxTicks - 1), true)
    val niceMin = floor(minV / step) * step
    val niceMax = ceil(maxV / step) * step
    return niceMin to niceMax
}
```

## 时间轴交互

- **触摸拖拽**: 水平滑动更新 `windowStart`
- **左右按钮**: 每次移动 `visibleRange * 0.3`
- **按钮禁用判定**: `windowStart > firstTs + tolerance` (左), `windowStart + visibleRange < lastTs - tolerance` (右), `tolerance = visibleRange * 0.01`
- **显隐逻辑**: 用户交互后显示 4 秒，超时自动淡出

## 对外 API

```kotlin
class ChartView : LinearLayout {

    // === 数据 ===
    fun setSeries(series: List<ChartSeries>, unitLabel: String)

    // === 图表样式（全局） ===
    fun setLineStyle(style: LineStyle)      // 应用到所有 series
    fun getLineStyle(): LineStyle

    // === 可见窗口 ===
    fun setVisibleRange(millis: Long)       // 外部设置可见时间窗口
    fun getVisibleRange(): Long             // 读取当前可见窗口

    // === X轴标签间隔 ===
    fun setLabelInterval(millis: Long)      // 手动设置标签间隔，0=自动
    fun getLabelInterval(): Long            // 读取当前间隔

    // === Y轴范围 ===
    fun setYAxisRange(minPct: Float, maxPct: Float)
    fun getYAxisMinPct(): Float
    fun getYAxisMaxPct(): Float

    // === 全屏 ===
    fun toggleFullscreen()
    fun isFullscreen(): Boolean
    fun setOnFullscreenListener(listener: ((Boolean) -> Unit)?)
}
```

## BUG 修复映射

| Bug | 根因 | 修复 |
|-----|------|------|
| 十字线位置偏移 | MPAndroidChart 坐标映射混乱 | Canvas 统一相对坐标 |
| 时间轴滑块偏移 | 全量/过滤数据时间基准混用 | 统一数据基准 |
| 左右按钮禁用失效 | ARROW_TOLERANCE_MS 绝对定值 | 动态容差 visibleRange*0.01 |
| X轴标签间隔不准 | 依赖 MPAndroidChart granularity | 自算间隔 |
| Y轴被覆盖 | fitScreen() 覆盖 axisMinimum | Canvas 直接使用用户范围 |
| 次小时级选项无用 | 硬编码所有选项 | 按数据跨度动态过滤 |

## 图例自动生成

遍历 `series` 列表，每个生成一行：
```
[线段] [数据点形状] 标签文字
```
线段样式 = series.lineType，颜色 = series.color
数据点形状 = series.pointShape + series.pointFill

## 迁移影响

### 新增文件
- `ui/profile/chart/ChartView.kt` — 主 ViewGroup
- `ui/profile/chart/ChartCanvas.kt` — 图表绘制 View
- `ui/profile/chart/ChartMath.kt` — 计算工具（扩展 ChartSegmentMath）

### 修改文件
- `weight/HeightChartActivity.kt` — 改为嵌入 ChartView
- `ChartFragment.kt` — 改为嵌入 ChartView
- `WeightDetailActivity.kt` / `HeightDetailActivity.kt` — 相应调整
- `activity_base_chart.xml` — 可能废弃或重构
- `arrays.xml` — 新增图表样式数组（5 种）
- `build.gradle.kts` — 移除 MPAndroidChart 依赖

### 删除文件
- `BaseChartActivity.kt` — 由 ChartView 替代
- `ChartCrosshairView` — 内移到 ChartCanvas
- `LineStyle.kt` — 内移到 ChartView
