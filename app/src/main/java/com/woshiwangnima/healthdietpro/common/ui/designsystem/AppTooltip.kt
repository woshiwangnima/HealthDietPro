package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** 淡入时长：够短，不拖尾。 */
private const val TooltipFadeInMs = 120

/** 稳定可读的停留时间，之后进入闪烁收尾。 */
private const val TooltipHoldMs = 2400L

/** 单次明暗交替的时长（一次「暗下去再亮回来」= 2 × 该值）。 */
private const val TooltipBlinkHalfMs = 260

/** 闪烁次数：两下足够被注意到，再多就成了干扰。 */
private const val TooltipBlinkCount = 2

/** 闪烁时降到的最低不透明度：不能全隐，否则读了一半的字先没了。 */
private const val TooltipBlinkMinAlpha = 0.25f

/**
 * 长按说明浮层的状态：**同时管「显示」与「锚点」，并自己盯着宿主生命周期**。
 *
 * 之所以不让调用方自己拿一个 `mutableStateOf(false)`：浮层是独立窗口 + 自带一段定时动画，
 * 而切到别的页面时宿主 Activity 只是 pause（composition 不销毁），动画会停在半路；
 * 等用户回到这一页，帧时钟恢复，动画从暂停处接着放——于是「已经关掉的说明又冒出来闪一下」。
 * 这个收尾只能由状态对象自己做，调用方没有机会漏掉。
 *
 * 用法固定为三步（见 [rememberAppTooltipState]）：
 * ```
 * val tooltip = rememberAppTooltipState()
 * Modifier.onGloballyPositioned { tooltip.updateAnchor(it.boundsInWindow()) }
 *     .combinedClickable(onLongClick = { tooltip.show() }, ...)
 * AppTooltipPopup(state = tooltip, text = description)
 * ```
 */
@Stable
class AppTooltipState internal constructor() {
    /** 触发元素在窗口里的位置（px）；每次布局刷新，浮层据此贴边定位。 */
    internal var anchorBounds by mutableStateOf(Rect.Zero)
        private set

    /** 浮层当前是否显示。对外只读，改它必须走 [show] / [dismiss]。 */
    var isVisible by mutableStateOf(false)
        private set

    /** 记录触发元素的窗口坐标；跟随滚动/旋转，浮层始终贴着它。 */
    fun updateAnchor(bounds: Rect) {
        anchorBounds = bounds
    }

    /** 长按触发。 */
    fun show() {
        isVisible = true
    }

    /** 收起（点外部、超时、宿主离开前台都走这里）。 */
    fun dismiss() {
        isVisible = false
    }
}

/**
 * 记住一个 [AppTooltipState]，并把它的生命周期绑到当前宿主（Activity / Fragment）上。
 *
 * 宿主一进入 `ON_PAUSE`（切页、进后台、被别的 Activity 挡住）就**立即收起**，不走闪烁收尾：
 * 那会儿用户已经看不到浮层了，闪给谁看；回退时也不该再补放一遍。
 */
@Composable
fun rememberAppTooltipState(): AppTooltipState {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(lifecycleOwner) { AppTooltipState() }
    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) state.dismiss()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

/**
 * 文字提示（工具提示 / Tooltip）：锚定在触发元素旁、只念一句解释、不可交互。
 *
 * 通用规范 §5.1-1 把 Tooltip 定为「非阻断 / 无遮罩 / 失焦或超时关闭」，§5.2-1 要求带锚点箭头、
 * 标题可省略。移动端没有悬停，触发方式取「长按」（§4.1 手势表：长按用于展示补充说明）。
 *
 * 说明文字不再恒显在行里：设置项本身只留标题与当前值，行高统一到 56 dp；
 * 需要解释时再长按，浮层贴在那一行上，不会把列表撑成「每行两行字」。
 *
 * 消失前闪两下（alpha 1 → 0.25 → 1 ×2）：浮层是自动收起的，不打个招呼就直接消失，
 * 用户会以为是自己误触关掉的，或者以为内容还没读完；闪烁等于「这条马上要走了」的提示。
 *
 * 只接受 [AppTooltipState]，不提供「传个布尔值 + 回调」的写法：那种写法会把生命周期收尾
 * 留给每个调用点，漏一个就复现「切页回来又亮一下」。见 [AppTooltipState] 的说明。
 */
@Composable
fun AppTooltipPopup(
    state: AppTooltipState,
    text: String,
) {
    if (!state.isVisible) return

    val tokens = LocalAppTokens.current
    val density = LocalDensity.current
    val anchorBounds = state.anchorBounds
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    // 上方空间不足两成屏高时改挂到行的下方，避免浮层被顶到状态栏底下。
    val placeAbove = anchorBounds.top > screenHeightPx * 0.18f
    val gapPx = with(density) { tokens.spacing.s2.roundToPx() }
    val marginPx = with(density) { tokens.spacing.s4.roundToPx() }

    // 兜底：宿主不在前台时（正常路径已被 ON_PAUSE 拦掉）不出浮层，也不留待「下次回来再放」。
    val isResumed = rememberIsHostResumed()
    if (!isResumed) {
        LaunchedEffect(state) { state.dismiss() }
        return
    }

    val alpha = remember { Animatable(0f) }

    // 浮层自己管生命周期：淡入 → 停留 → 闪两下 → 收起。
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(TooltipFadeInMs, easing = AppEaseOut))
        delay(TooltipHoldMs)
        repeat(TooltipBlinkCount) {
            alpha.animateTo(TooltipBlinkMinAlpha, animationSpec = tween(TooltipBlinkHalfMs, easing = AppEaseOut))
            alpha.animateTo(1f, animationSpec = tween(TooltipBlinkHalfMs, easing = AppEaseOut))
        }
        state.dismiss()
    }

    Popup(
        popupPositionProvider = remember(anchorBounds, placeAbove, gapPx, marginPx) {
            TooltipPositionProvider(
                anchor = anchorBounds,
                gapPx = gapPx,
                marginPx = marginPx,
                placeAbove = placeAbove,
            )
        },
        // 非阻断：不抢焦点、不拦返回键，点外部仍能操作页面，靠超时与再长按收起。
        onDismissRequest = { state.dismiss() },
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier.alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!placeAbove) {
                TooltipArrow(pointsUp = true, modifier = Modifier.offset(y = 1.dp))
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(tokens.spacing.s2, RoundedCornerShape(tokens.radii.label))
                    .clip(RoundedCornerShape(tokens.radii.label))
                    .background(tokens.colors.panelRaised)
                    .border(
                        tokens.radii.highlight,
                        tokens.colors.borderStrong,
                        RoundedCornerShape(tokens.radii.label),
                    )
                    .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
            ) {
                Text(
                    text = text,
                    style = tokens.type.caption,
                    color = tokens.colors.text,
                )
            }
            if (placeAbove) {
                TooltipArrow(pointsUp = false, modifier = Modifier.offset(y = (-1).dp))
            }
        }
    }
}

/** 宿主是否处于 `RESUMED`；跨页/后台期间为 false，用来兜住「浮层不该出现」的所有边角情况。 */
@Composable
private fun rememberIsHostResumed(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return resumed
}

/** 锚点箭头：指向触发元素；贴在气泡边框上，所以朝内那 1 dp 要压到边框上盖掉那条直线。 */
@Composable
private fun TooltipArrow(pointsUp: Boolean, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val arrowWidth = 12.dp
    val arrowHeight: Dp = 6.dp
    val fill = tokens.colors.panelRaised
    val stroke = tokens.colors.borderStrong
    Canvas(modifier = modifier.size(arrowWidth, arrowHeight)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (pointsUp) {
                moveTo(w / 2f, 0f)
                lineTo(0f, h)
                lineTo(w, h)
            } else {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w / 2f, h)
            }
            close()
        }
        drawPath(path, color = fill)
        // 只描外露的两条斜边，底边压在气泡边框上不再描，否则会出现一道横线。
        val outline = Path().apply {
            if (pointsUp) {
                moveTo(w / 2f, 0f)
                lineTo(0f, h)
                moveTo(w / 2f, 0f)
                lineTo(w, h)
            } else {
                moveTo(0f, 0f)
                lineTo(w / 2f, h)
                moveTo(w, 0f)
                lineTo(w / 2f, h)
            }
        }
        drawPath(outline, color = stroke, style = Stroke(width = 1.dp.toPx()))
    }
}

/**
 * 气泡定位：水平居中于触发行并按窗口左右留白收边，纵向优先挂在行上方。
 */
private class TooltipPositionProvider(
    private val anchor: Rect,
    private val gapPx: Int,
    private val marginPx: Int,
    private val placeAbove: Boolean,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val x = (anchor.center.x.roundToInt() - popupContentSize.width / 2).coerceIn(marginPx, maxX)
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
        val y = if (placeAbove) {
            (anchor.top.roundToInt() - gapPx - popupContentSize.height).coerceAtLeast(marginPx)
        } else {
            (anchor.bottom.roundToInt() + gapPx).coerceAtMost(maxY)
        }
        return IntOffset(x, y)
    }
}
