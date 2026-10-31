package com.woshiwangnima.healthdietpro.ui.price

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 走势图视窗换算：缩放要把手指下的日期/价格钉住，拖动要跟手且拖不出去。
 *
 * 这些数只能靠算式验证——多指缩放没法用 adb 稳定复现，
 * 而「缩一次图数据就跑到一边」这类问题恰恰只在算错时才出现。
 */
class PriceChartViewportTest {

    private val frame = ChartFrame(
        box = PlotBox(left = 0f, right = 1000f, top = 0f, bottom = 500f),
        dataLeft = 50f,
        dataRight = 950f,
        dateStart = 0L,
        dateEnd = 1000L,
        valueMin = 0.0,
        valueMax = 100.0,
        singleDate = false,
    )

    @Test
    fun zoomKeepsTheAnchorUnderTheFingers() {
        val zoomed = applyGesture(
            viewport = ChartViewport(),
            frame = frame,
            centroid = Offset(500f, 250f),
            pan = Offset.Zero,
            zoom = 2f,
        )

        assertEquals(0.5f, zoomed.xSpan, 0.0001f)
        assertEquals(0.5f, zoomed.ySpan, 0.0001f)
        // 锚点（正中）那一半的日期在缩放前后停在同一个比例位置。
        assertEquals(0.5f, zoomed.xStart + 0.5f * zoomed.xSpan, 0.0001f)
        assertEquals(0.5f, zoomed.yStart + 0.5f * zoomed.ySpan, 0.0001f)
    }

    @Test
    fun zoomIsClampedSoTheViewNeverBecomesMeaningless() {
        var viewport = ChartViewport()
        repeat(20) {
            viewport = applyGesture(viewport, frame, Offset(500f, 250f), Offset.Zero, 2f)
        }

        assertEquals(1f / 24f, viewport.xSpan, 0.0001f)
        assertEquals(1f / 8f, viewport.ySpan, 0.0001f)
    }

    @Test
    fun panDoesNothingWhenTheWholeRangeIsVisible() {
        // 全貌时往哪拖都没有余量：视窗必须原地不动，否则数据会被拖出画面。
        val panned = applyGesture(
            viewport = ChartViewport(),
            frame = frame,
            centroid = Offset(500f, 250f),
            pan = Offset(320f, 180f),
            zoom = 1f,
        )

        assertEquals(0f, panned.xStart, 0.0001f)
        assertEquals(0f, panned.yStart, 0.0001f)
        assertTrue(panned.isFull)
    }

    @Test
    fun panFollowsTheFingerInsideTheZoomedWindow() {
        val zoomed = ChartViewport(xStart = 0.4f, xSpan = 0.2f, yStart = 0.4f, ySpan = 0.2f)

        val panned = applyGesture(zoomed, frame, Offset(500f, 250f), Offset(100f, 50f), 1f)

        // 手指右拖 100px ÷ 1000px 画布宽 × 0.2 视窗 = 左移 0.02；下拖同理把视窗推向高价。
        assertEquals(0.38f, panned.xStart, 0.0001f)
        assertEquals(0.42f, panned.yStart, 0.0001f)
        assertEquals(0.2f, panned.xSpan, 0.0001f)
    }

    @Test
    fun panStopsAtTheEdgeInsteadOfLeavingTheData() {
        val zoomed = ChartViewport(xStart = 0.01f, xSpan = 0.2f)

        val panned = applyGesture(zoomed, frame, Offset(500f, 250f), Offset(300f, 0f), 1f)

        assertEquals(0f, panned.xStart, 0.0001f)
    }

    @Test
    fun fullDateWindowCoversTheLastPointExactly() {
        // 两天跨度是 10 位毫秒，用 Float 换算会把终点算小一两秒，末点的日期标签就没了。
        val min = 1_733_000_000_000L
        val max = 1_793_000_000_000L

        val window = dateWindow(min, max, xStart = 0f, xSpan = 1f)

        assertEquals(min, window.first)
        assertEquals(max, window.last)
    }

    @Test
    fun zoomedDateWindowStaysInsideTheData() {
        val min = 1_733_000_000_000L
        val max = 1_793_000_000_000L

        val leftHalf = dateWindow(min, max, xStart = 0f, xSpan = 0.5f)
        val rightHalf = dateWindow(min, max, xStart = 0.5f, xSpan = 0.5f)

        assertEquals(min, leftHalf.first)
        assertTrue(leftHalf.last in min..max)
        assertTrue(rightHalf.first >= leftHalf.last - 1)
        assertEquals(max, rightHalf.last)
    }

    @Test
    fun windowTouchingTheRightEdgeKeepsTheLastPoint() {
        // 放大到贴着右边界时，末点必须还在窗内：否则图上有画点，却说「这段区间没有记录」。
        val min = 1_733_000_000_000L
        val max = 1_793_000_000_000L

        val tail = dateWindow(min, max, xStart = 0.75f, xSpan = 0.25f)

        assertEquals(max, tail.last)
        assertTrue(tail.first < max)
    }

    @Test
    fun compactRegionDropsTheRepeatedMunicipalityName() {
        // 浮动窗只有一指宽，直辖市的省名会把「合并 N 条」挤掉。
        assertEquals("北京市 · 朝阳区", compactRegion("北京市 - 北京市 - 朝阳区"))
        assertEquals("安徽省 · 铜陵市 · 义安区", compactRegion("安徽省 - 铜陵市 - 义安区"))
        assertEquals("", compactRegion("   "))
    }
}
