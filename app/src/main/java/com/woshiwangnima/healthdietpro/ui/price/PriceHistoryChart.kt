package com.woshiwangnima.healthdietpro.ui.price

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppTextIconButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.price.formatPriceRange
import com.woshiwangnima.healthdietpro.model.price.formatPriceValue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

/** 图表上的一个点：横轴是日期，[low]–[high] 是当天该单位见到的价格区间。 */
@Immutable
internal data class PriceChartPoint(
    val recordedAt: Long,
    val low: Double,
    val high: Double,
    /** 当天、该单位下合并了几条原始记录；点按浮动窗里写「合并 N 条」。 */
    val sampleCount: Int = 1,
    /** 当天出现过的地区（已去重），点按浮动窗用它回答「这是哪儿的价」。 */
    val regions: List<String> = emptyList(),
)

/** 图表上的一条线：一个售卖单位一条，颜色与图例文字由调用方给。 */
@Immutable
internal data class PriceChartSeries(
    val label: String,
    val color: Color,
    val points: List<PriceChartPoint>,
)

/**
 * 视窗：横轴与纵轴都是「占完整范围的比例」。
 *
 * 用比例而不是绝对坐标，是因为缩放/拖动只需要在 0–1 里夹紧，
 * 数据换了（筛选变了、又记了几笔）也不用换算旧的像素位置。
 */
@Immutable
internal data class ChartViewport(
    val xStart: Float = 0f,
    val xSpan: Float = 1f,
    val yStart: Float = 0f,
    val ySpan: Float = 1f,
) {
    val isFull: Boolean
        get() = xStart <= 0.0001f && xSpan >= 0.9999f && yStart <= 0.0001f && ySpan >= 0.9999f
}

/** 选中的点：哪条线上的第几个点。 */
@Immutable
internal data class ChartSelection(val seriesIndex: Int, val pointIndex: Int)

/** 浮动窗的三行文字：日期、价格区间 + 单位、地区 + 合并条数。 */
@Immutable
private data class ChartTooltip(
    val title: String,
    val value: String,
    val meta: String,
)

/**
 * 物品价格走势图。
 *
 * 两种画法都用上，是因为一条记录可能本身就是一个区间：
 * - **折线**连的是每条记录的下限（单值记录时就是那个数），表示价格走向；
 * - **竖线**从下限拉到上限，表示当天见到的价格范围；单值记录时退化为一个点。
 *
 * 横轴按真实日期比例排布（不是等距索引）：两个月没记过，那段空白就该在图上留出来。
 *
 * **可缩放拖动**：物价记久了同一天挨着好几笔，全塞进一张固定宽度里点会叠成一坨。
 * 双指缩放把日期轴与价格轴一起放大，单指拖动平移；右上角出现「重置」回到全貌。
 * 两个轴都从中心夹紧到 0–1，所以怎么拖都不会把数据拖出视窗找不回来。
 *
 * **点按关键点**：点圆点弹浮动窗，给出这一天的日期、价格区间与地区——
 * 图上只能看出「涨了还是跌了」，具体是哪天、在哪儿买的必须点开才知道。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PriceHistoryChart(
    series: List<PriceChartSeries>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val zone = remember { ZoneId.systemDefault() }
    // 数据换了就回到全貌：上一份数据的视窗对这份没有意义，还可能把点全框在窗外。
    var viewport by remember(series) { mutableStateOf(ChartViewport()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 图例必须带文字：只靠颜色区分系列，对色觉障碍用户等于没有图例（§9.4）。
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                series.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(spacing.s2)) { drawCircle(item.color) }
                        Spacer(Modifier.width(spacing.s1))
                        Text(
                            text = item.label,
                            style = tokens.type.caption,
                            color = tokens.colors.textMuted,
                        )
                    }
                }
            }
            // 只在动过视窗时出现：没缩放时它是个点了没反应的按钮，白占地方。
            if (!viewport.isFull) {
                AppTextIconButton(
                    text = stringResource(R.string.price_history_chart_reset),
                    iconRes = R.drawable.ic_refresh,
                    onClick = { viewport = ChartViewport() },
                )
            }
        }
        Spacer(Modifier.height(spacing.s2))
        PriceHistoryPlot(
            series = series,
            zone = zone,
            viewport = viewport,
            onViewportChange = { viewport = it },
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun PriceHistoryPlot(
    series: List<PriceChartSeries>,
    zone: ZoneId,
    viewport: ChartViewport,
    onViewportChange: (ChartViewport) -> Unit,
    contentDescription: String,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selected by remember(series) { mutableStateOf<ChartSelection?>(null) }

    val frame = remember(series, viewport, canvasSize, density) {
        buildFrame(series, viewport, canvasSize, density)
    }
    // 手势回调不随重组重建（pointerInput 只在数据变化时重启），
    // 所以视窗与几何都从 rememberUpdatedState 里读当前值，而不是捕获重组那一刻的旧值。
    val currentFrame by rememberUpdatedState(frame)
    val currentViewport by rememberUpdatedState(viewport)
    val changeViewport by rememberUpdatedState(onViewportChange)

    val unsetRegion = stringResource(R.string.price_region_unset)
    val emptyView = stringResource(R.string.price_history_chart_view_empty)
    val selectedPoint = selected?.let { sel ->
        val point = series.getOrNull(sel.seriesIndex)?.points?.getOrNull(sel.pointIndex)
        if (point == null) null else sel to point
    }
    val tooltip = if (selectedPoint == null) {
        null
    } else {
        val (sel, point) = selectedPoint
        val merged = if (point.sampleCount > 1) {
            stringResource(R.string.price_group_count, point.sampleCount)
        } else {
            null
        }
        ChartTooltip(
            title = formatAxisDate(point.recordedAt, withYear = true, zone = zone),
            value = formatPriceRange(point.low, point.high) + " " + series[sel.seriesIndex].label,
            // 直辖市的省名和市名相同，原样拼出来是「北京市 - 北京市 - 朝阳区」，先把重复段收掉。
            meta = (
                point.regions.map(::compactRegion).joinToString("、").ifBlank { unsetRegion } +
                    (merged?.let { "\n" + it } ?: "")
                ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
            .onSizeChanged { canvasSize = it },
    ) {
        val currentFrameForDraw = frame
        if (currentFrameForDraw != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // 画布本身没有可读文本：读屏时至少要说清「这是哪个物品、几种单位的走势」。
                    .semantics { this.contentDescription = contentDescription }
                    // 先装点按：双指缩放不消费「按下」，所以轻点仍会落到这里；一旦开始拖动/缩放，
                    // 变换手势会把手势吃掉，点按自然不触发。
                    .pointerInput(series) {
                        detectTapGestures { tap ->
                            val f = currentFrame ?: return@detectTapGestures
                            selected = f.nearestPoint(series, tap, 24.dp.toPx())
                        }
                    }
                    .pointerInput(series) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val f = currentFrame ?: return@detectTransformGestures
                            // 一动手就把浮动窗收掉：留着它会挡住刚拖到的数据。
                            selected = null
                            val next = applyGesture(currentViewport, f, centroid, pan, zoom)
                            if (next != currentViewport) changeViewport(next)
                        }
                    },
            ) {
                drawChart(
                    frame = currentFrameForDraw,
                    series = series,
                    selected = selected,
                    tooltip = tooltip,
                    tokens = tokens,
                    measurer = measurer,
                    zone = zone,
                    emptyViewLabel = emptyView,
                )
            }
        }
    }
}

/** 绘图区：左边留纵轴刻度、下边留日期标签、最外侧再给首末点让出 10dp。 */
@Immutable
internal data class PlotBox(val left: Float, val right: Float, val top: Float, val bottom: Float) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
}

/** 一次绘制用到的全部换算：日期 → 横坐标、价格 → 纵坐标。 */
@Immutable
internal data class ChartFrame(
    val box: PlotBox,
    val dataLeft: Float,
    val dataRight: Float,
    val dateStart: Long,
    val dateEnd: Long,
    val valueMin: Double,
    val valueMax: Double,
    /** 只有一个日期时横轴没有跨度可拉伸，点固定放中间。 */
    val singleDate: Boolean,
) {
    fun xFor(millis: Long): Float =
        if (singleDate || dateEnd <= dateStart) {
            (dataLeft + dataRight) / 2f
        } else {
            dataLeft + (dataRight - dataLeft) *
                ((millis - dateStart).toFloat() / (dateEnd - dateStart).toFloat())
        }

    fun yFor(value: Double): Float {
        val span = (valueMax - valueMin).takeIf { it > 0.0 } ?: 1.0
        return box.bottom - box.height * ((value - valueMin) / span).toFloat()
    }

    /** 离手指最近的关键点；超出 [maxDistance] 视为点在空白处（用来关掉浮动窗）。 */
    fun nearestPoint(series: List<PriceChartSeries>, tap: Offset, maxDistance: Float): ChartSelection? {
        var best: ChartSelection? = null
        var bestDistance = maxDistance
        series.forEachIndexed { seriesIndex, line ->
            line.points.forEachIndexed { pointIndex, point ->
                val x = xFor(point.recordedAt)
                if (x < box.left - maxDistance || x > box.right + maxDistance) return@forEachIndexed
                val distance = hypot(x - tap.x, yFor(point.low) - tap.y)
                if (distance <= bestDistance) {
                    bestDistance = distance
                    best = ChartSelection(seriesIndex, pointIndex)
                }
            }
        }
        return best
    }
}

private fun buildFrame(
    series: List<PriceChartSeries>,
    viewport: ChartViewport,
    canvasSize: IntSize,
    density: Density,
): ChartFrame? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
    val points = series.flatMap { it.points }
    if (points.isEmpty()) return null

    return with(density) {
        val box = PlotBox(
            left = 46.dp.toPx(),
            right = canvasSize.width - 6.dp.toPx(),
            top = 10.dp.toPx(),
            bottom = canvasSize.height - 26.dp.toPx(),
        )
        // 首末点各留 10 dp：贴着边框画，点会被卡片圆角和轴线切掉一半。
        val edge = 10.dp.toPx()
        val axis = priceAxis(points)
        val minDate = points.minOf { it.recordedAt }
        val maxDate = points.maxOf { it.recordedAt }
        val singleDate = maxDate <= minDate
        val window = dateWindow(minDate, maxDate, viewport.xStart, viewport.xSpan)
        val dateStart = window.first
        val dateEnd = window.last
        val fullValueSpan = (axis.max - axis.min).takeIf { it > 0.0 } ?: 1.0
        val valueMin = axis.min + fullValueSpan * viewport.yStart
        val valueMax = valueMin + fullValueSpan * viewport.ySpan
        ChartFrame(
            box = box,
            dataLeft = box.left + edge,
            dataRight = box.right - edge,
            dateStart = dateStart,
            dateEnd = dateEnd,
            valueMin = valueMin,
            valueMax = valueMax,
            singleDate = singleDate,
        )
    }
}

/**
 * 视窗覆盖的日期区间（闭区间）。
 *
 * **必须用 Double 再取整**：两天跨度是 10 位毫秒，Float 只有 24 位尾数，
 * 全貌时算出的终点会比 `maxDate` 小一两秒，末点的日期标签与空态判断都会跟着错。
 */
internal fun dateWindow(minDate: Long, maxDate: Long, xStart: Float, xSpan: Float): LongRange {
    if (maxDate <= minDate) return minDate..maxDate
    val span = (maxDate - minDate).toDouble()
    // 用四舍五入而不是截断：截断会让「正好贴边」的视窗比末点早一两毫秒，
    // 末点落在窗外 → 图上明明有点，标签和「这段区间没有记录」却说没有。
    val start = (minDate + Math.round(span * xStart)).coerceIn(minDate, maxDate - 1)
    val end = (start + Math.round(span * xSpan)).coerceIn(start + 1, maxDate)
    return start..end
}

/** 缩放上限：横向 24 倍、纵向 8 倍。再放大会让刻度只剩一两个数，没信息量。 */
private const val MIN_X_SPAN = 1f / 24f
private const val MIN_Y_SPAN = 1f / 8f

/**
 * 把一次手势应用到视窗上。
 *
 * 缩放以两指中心为锚点：锚点下的那个日期/价格在缩放前后停在原地，
 * 否则每缩一次数据都会往两头跑，用户得反复找刚才看的位置。
 * 拖动按「内容跟手」走：手指往右拖，视窗往左移。
 */
internal fun applyGesture(
    viewport: ChartViewport,
    frame: ChartFrame,
    centroid: Offset,
    pan: Offset,
    zoom: Float,
): ChartViewport {
    val width = frame.box.width
    val height = frame.box.height
    if (width <= 0f || height <= 0f) return viewport
    val safeZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f

    val focusX = ((centroid.x - frame.box.left) / width).coerceIn(0f, 1f)
    val newXSpan = (viewport.xSpan / safeZoom).coerceIn(MIN_X_SPAN, 1f)
    val anchorDate = viewport.xStart + focusX * viewport.xSpan
    val xStart = (anchorDate - focusX * newXSpan - pan.x / width * newXSpan)
        .coerceIn(0f, 1f - newXSpan)

    // 屏幕上方是高价：focusY 从顶部量，换算成「从下限往上」的比例。
    val focusY = ((centroid.y - frame.box.top) / height).coerceIn(0f, 1f)
    val newYSpan = (viewport.ySpan / safeZoom).coerceIn(MIN_Y_SPAN, 1f)
    val anchorValue = viewport.yStart + (1f - focusY) * viewport.ySpan
    val yStart = (anchorValue - (1f - focusY) * newYSpan + pan.y / height * newYSpan)
        .coerceIn(0f, 1f - newYSpan)

    return ChartViewport(xStart = xStart, xSpan = newXSpan, yStart = yStart, ySpan = newYSpan)
}

private fun DrawScope.drawChart(
    frame: ChartFrame,
    series: List<PriceChartSeries>,
    selected: ChartSelection?,
    tooltip: ChartTooltip?,
    tokens: AppTokens,
    measurer: TextMeasurer,
    zone: ZoneId,
    emptyViewLabel: String,
) {
    val colors = tokens.colors
    val box = frame.box
    val hairline = 1.dp.toPx()
    val labelStyle = tokens.type.caption.copy(color = colors.textMuted)

    // 横向网格 + 纵轴刻度：网格用边框色，比文字淡一档，不和数据抢。
    visibleTicks(frame.valueMin, frame.valueMax).forEach { tick ->
        val y = frame.yFor(tick)
        if (y < box.top - hairline || y > box.bottom + hairline) return@forEach
        drawLine(
            color = colors.border,
            start = Offset(box.left, y),
            end = Offset(box.right, y),
            strokeWidth = hairline,
        )
        val label = measurer.measure(formatPriceValue(tick), labelStyle)
        drawText(
            textLayoutResult = label,
            topLeft = Offset(
                x = box.left - 6.dp.toPx() - label.size.width,
                y = y - label.size.height / 2f,
            ),
        )
    }
    // 基线单独画深一点，作为「时间轴」的边界。
    drawLine(
        color = colors.borderStrong,
        start = Offset(box.left, box.bottom),
        end = Offset(box.right, box.bottom),
        strokeWidth = hairline,
    )

    drawDateLabels(frame, series, measurer, labelStyle, zone)

    val hasVisiblePoint = series.any { line ->
        line.points.any { it.recordedAt in frame.dateStart..frame.dateEnd }
    }
    if (!hasVisiblePoint) {
        // 拖到一段空档里时别留一片什么都没有的网格：说清是「这段没有记录」，右上角还有「重置」。
        val layout = measurer.measure(emptyViewLabel, labelStyle)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                x = (box.left + box.right) / 2f - layout.size.width / 2f,
                y = (box.top + box.bottom) / 2f - layout.size.height / 2f,
            ),
        )
        return
    }

    // 数据超出的部分要裁掉，否则缩放后线的两端会画到纵轴刻度和日期标签上。
    clipRect(left = box.left, top = box.top, right = box.right, bottom = box.bottom) {
        series.forEach { line ->
            val xs = line.points.map { frame.xFor(it.recordedAt) }
            // 区间竖线先画，折线与点压在它上面，一眼能分出「范围」和「读数」。
            line.points.forEachIndexed { index, point ->
                if (point.high <= point.low) return@forEachIndexed
                drawLine(
                    color = line.color.copy(alpha = 0.5f),
                    start = Offset(xs[index], frame.yFor(point.high)),
                    end = Offset(xs[index], frame.yFor(point.low)),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            if (line.points.size > 1) {
                val path = Path()
                line.points.forEachIndexed { index, point ->
                    val x = xs[index]
                    val y = frame.yFor(point.low)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = line.color,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            line.points.forEachIndexed { index, point ->
                val center = Offset(xs[index], frame.yFor(point.low))
                // 先画一圈面板底色的描边，点落在网格线上也能分离出来。
                drawCircle(color = colors.panelRaised, radius = 5.dp.toPx(), center = center)
                drawCircle(color = line.color, radius = 3.dp.toPx(), center = center)
            }
        }
    }

    val sel = selected ?: return
    val line = series.getOrNull(sel.seriesIndex) ?: return
    val point = line.points.getOrNull(sel.pointIndex) ?: return
    val x = frame.xFor(point.recordedAt)
    if (x < box.left || x > box.right) return
    val y = frame.yFor(point.low)
    if (y < box.top || y > box.bottom) return

    // 竖向指示线：把「点」和下面的日期轴连起来，否则点了半天不知道是哪一天。
    drawLine(
        color = colors.textMuted.copy(alpha = 0.55f),
        start = Offset(x, box.top),
        end = Offset(x, box.bottom),
        strokeWidth = hairline,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
    )
    drawCircle(color = line.color, radius = 7.dp.toPx(), center = Offset(x, y), style = Stroke(2.dp.toPx()))
    drawCircle(color = line.color, radius = 4.dp.toPx(), center = Offset(x, y))

    val tip = tooltip ?: return
    drawTooltip(frame, tip, x, y, tokens, measurer)
}

private fun DrawScope.drawTooltip(
    frame: ChartFrame,
    tip: ChartTooltip,
    pointX: Float,
    pointY: Float,
    tokens: AppTokens,
    measurer: TextMeasurer,
) {
    val colors = tokens.colors
    val box = frame.box
    val pad = tokens.spacing.s2.toPx()
    val gap = 2.dp.toPx()
    val maxTextWidth = 220.dp.toPx().toInt()
    val constraints = Constraints(maxWidth = maxTextWidth)
    val titleLayout = measurer.measure(
        text = tip.title,
        style = tokens.type.caption.copy(color = colors.text),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = constraints,
    )
    val valueLayout = measurer.measure(
        text = tip.value,
        style = tokens.type.bodyS.copy(color = colors.text),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = constraints,
    )
    val metaLayout = measurer.measure(
        text = tip.meta,
        style = tokens.type.caption.copy(color = colors.textMuted),
        // 地区 + 合并条数可能放不下一行：宁可折成两行，也别把「合并 N 条」截掉。
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        constraints = constraints,
    )

    val textWidth = maxOf(titleLayout.size.width, valueLayout.size.width, metaLayout.size.width)
    val width = (textWidth + pad * 2).coerceAtMost(maxTextWidth + pad * 2).toFloat()
    val height = titleLayout.size.height + valueLayout.size.height + metaLayout.size.height +
        gap * 2 + pad * 2
    val offset = 12.dp.toPx()

    // 默认放在点的上方，上方放不下就翻到下方；左右夹在绘图区内，不压到纵轴刻度。
    var left = pointX - width / 2f
    left = left.coerceIn(box.left, (box.right - width).coerceAtLeast(box.left))
    var top = pointY - offset - height
    if (top < box.top) top = pointY + offset
    top = top.coerceIn(box.top, (box.bottom - height).coerceAtLeast(box.top))

    drawRoundRect(
        color = colors.panelRaised,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(tokens.radii.container.toPx()),
    )
    drawRoundRect(
        color = colors.borderStrong,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(tokens.radii.container.toPx()),
        style = Stroke(1.dp.toPx()),
    )

    var lineTop = top + pad
    listOf(titleLayout, valueLayout, metaLayout).forEachIndexed { index, layout ->
        drawText(textLayoutResult = layout, topLeft = Offset(left + pad, lineTop))
        lineTop += layout.size.height + if (index < 2) gap else 0f
    }
}

/** 日期标签：按位置从左到右摆，放不下就跳过（宁可少一个标签，也不要叠字）。 */
private fun DrawScope.drawDateLabels(
    frame: ChartFrame,
    series: List<PriceChartSeries>,
    measurer: TextMeasurer,
    labelStyle: androidx.compose.ui.text.TextStyle,
    zone: ZoneId,
) {
    val box = frame.box
    val dates = series.asSequence()
        .flatMap { it.points.asSequence() }
        .map { it.recordedAt }
        .filter { it in frame.dateStart..frame.dateEnd }
        .distinct()
        .sorted()
        .toList()
    if (dates.isEmpty()) return
    val labelGap = 10.dp.toPx()
    // 缩放到只剩一两个日期时不需要抽稀，否则会只剩最后一个标签。
    val every = ((dates.size + 3) / 4).coerceAtLeast(1)
    var lastLabelRight = Float.NEGATIVE_INFINITY
    dates.forEachIndexed { index, date ->
        val isLast = index == dates.lastIndex
        if (index % every != 0 && !isLast) return@forEachIndexed
        val text = formatAxisDate(date, withYear = index == 0, zone = zone)
        val layout = measurer.measure(text, labelStyle)
        val centerX = frame.xFor(date)
        val left = (centerX - layout.size.width / 2f).coerceIn(box.left, box.right - layout.size.width)
        if (left < lastLabelRight + labelGap) return@forEachIndexed
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(left, box.bottom + 6.dp.toPx()),
        )
        lastLabelRight = left + layout.size.width
    }
}

/**
 * 当前视窗内的刻度：步长按**可见范围**重算，缩放后刻度数才会稳定在 3–5 条。
 * 用完整范围算的话，放大 10 倍后只剩一两条刻度，价格读不出来。
 */
private fun visibleTicks(min: Double, max: Double): List<Double> {
    val span = max - min
    if (!span.isFinite() || span <= 0.0) return emptyList()
    val step = niceStep(span / 4.0)
    val first = ceil(min / step) * step
    val ticks = mutableListOf<Double>()
    var value = first
    // 上限 12 条：极端比例下（步长取整后过小）也别把整张图刷成一排横线。
    while (value <= max + step * 1e-6 && ticks.size < 12) {
        ticks += value
        value += step
    }
    return ticks
}

/**
 * 纵轴**完整**范围：取整到「好看的步长」，上下各留 12% 余量。
 *
 * 只给范围不给刻度：刻度要按当前视窗（缩放后可能只是一小段）再算，见 [visibleTicks]。
 */
private data class PriceAxis(val min: Double, val max: Double)

private fun priceAxis(points: List<PriceChartPoint>): PriceAxis {
    if (points.isEmpty()) return PriceAxis(0.0, 1.0)
    val dataMin = points.minOf { it.low }
    val dataMax = points.maxOf { maxOf(it.low, it.high) }
    val span = (dataMax - dataMin).takeIf { it > 0.0 } ?: (dataMax * 0.2).takeIf { it > 0.0 } ?: 1.0
    val step = niceStep(span / 3.0)
    // 价格没有负数：下限压到 0 就更贴近 0。
    val min = floor((dataMin - span * 0.12) / step).coerceAtLeast(0.0) * step
    val max = ceil((dataMax + span * 0.12) / step) * step
    return PriceAxis(min = min, max = if (max <= min) min + step else max)
}

/** 1 / 2 / 5 × 10ⁿ 的步长，图表刻度的通用取法。 */
private fun niceStep(rough: Double): Double {
    if (rough <= 0.0 || !rough.isFinite()) return 1.0
    val exponent = floor(log10(rough))
    val base = 10.0.pow(exponent)
    val normalized = rough / base
    val step = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    val result = step * base
    return if (abs(result) < 1e-9) 1.0 else result
}

/**
 * 「北京市 - 北京市 - 朝阳区」→「北京市 · 朝阳区」：相邻重复段只留一个。
 *
 * 只在浮动窗里收，列表照旧用完整地区：列表一行放得下，而浮动窗只有一指宽，
 * 直辖市的省名会把「合并 N 条」直接挤没。
 */
internal fun compactRegion(display: String): String {
    val parts = display.split(" - ").filter(String::isNotBlank)
    val kept = mutableListOf<String>()
    parts.forEach { part -> if (kept.lastOrNull() != part) kept += part }
    return kept.joinToString(" · ")
}

private val YEAR_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")

/**
 * 横轴只给第一个标签带年份：其余写「月-日」就够，四位数年份重复四次会把轴挤满。
 */
private fun formatAxisDate(millis: Long, withYear: Boolean, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(if (withYear) YEAR_DATE_FORMATTER else SHORT_DATE_FORMATTER)
