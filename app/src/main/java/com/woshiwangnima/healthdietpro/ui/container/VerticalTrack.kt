package com.woshiwangnima.healthdietpro.ui.container

/** Vertical inset (dp) reserved for the 100%/0% labels at the top/bottom of the track. */
internal const val TRACK_LABEL_INSET_DP = 18f

/**
 * Shared vertical-track geometry used by [VerticalPercentSlider] and [ContainerSideView].
 *
 * 单一坐标换算来源：滑块 thumb、预览红线与轮廓纵坐标都经同一 [VerticalTrack] 计算，
 * 保证右侧进度条高度与预览绘制严格对应；移植到其他宿主时只需调整 [TRACK_LABEL_INSET_DP]。
 *
 * @param heightPx the full height of the drawing area (px).
 * @param topInsetPx vertical inset reserved at the top (px).
 * @param bottomInsetPx vertical inset reserved at the bottom (px).
 */
internal class VerticalTrack(
    val heightPx: Float,
    val topInsetPx: Float,
    val bottomInsetPx: Float,
) {
    val trackTop: Float get() = topInsetPx
    val trackBottom: Float get() = (heightPx - bottomInsetPx).coerceAtLeast(trackTop)
    val trackHeight: Float get() = (trackBottom - trackTop).coerceAtLeast(1f)

    /** Percent (0..1, 1 = top, 0 = bottom) → y coordinate. */
    fun yForPercent(percent: Float): Float = trackBottom - trackHeight * percent.coerceIn(0f, 1f)

    /** y coordinate → percent (1 = top, 0 = bottom). */
    fun percentForY(y: Float): Float = (1f - (y - trackTop) / trackHeight).coerceIn(0f, 1f)
}
