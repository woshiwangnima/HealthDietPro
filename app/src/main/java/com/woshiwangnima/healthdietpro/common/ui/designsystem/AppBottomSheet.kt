package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.rememberOverlayWindowFit
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 底部抽屉的三档高度（移动规范 05 第二节「三分屏高度法则」）：
 * 30% 单选/短提示、60% 中等复杂度、80% 多步骤与长图文。
 * 高度按屏幕高度百分比计算，不写死像素，换机型不跑偏。
 */
enum class AppSheetHeight(val fraction: Float) {
    /** 30%：短提示、单选、两项以内的操作集合。 */
    Peek(0.30f),

    /** 60%：中等复杂度的列表或表单。 */
    Half(0.60f),

    /** 80%：多步骤、长图文、需要滚动的说明。 */
    Full(0.80f),
}

private val SheetMaxWidth = 560.dp

/** 超出边界后的跟手衰减系数：拖过满档/拖过底部时只走 32% 位移，形成「拉不动」的阻尼手感。 */
private const val RubberBandFactor = 0.32f

/** 松手时按速度外推的距离（秒），让「快速甩」比「慢慢拖」更容易跨档。 */
private const val FlingProjectionSeconds = 0.12f

/** 单档抽屉的下滑关闭速度阈值（px/s）。 */
private const val DismissVelocity = 1200f

/** 拖到最低档这个比例以下松手，直接关闭而不是吸回。 */
private const val DismissHeightRatio = 0.55f

/**
 * 底部半屏抽屉底层容器（移动规范 05 §一、§二、§三）。
 *
 * **高度与吸附**：面板的**可见高度**就是当前档位高度，面板底边始终停在系统导航条之上。
 * 松手时按「当前位置 + 速度外推」吸附到最近的 [snapPoints]；拖到最低档 55% 以下或快速下甩则关闭。
 *
 * 高度随档位变化的理由（2026-10-30 修）：早先的实现是「面板恒为满档高度、整体向下位移」表示收放，
 * 面板底边会跑到屏幕下方，可见区域的下沿于是变成屏幕底边——列表最后一行正好落在系统导航栏下面，
 * 三键导航时既看不见也点不中。改成按当前高度布局后，底边永远在导航条之上，多余内容靠内部滚动。
 * 代价是拖拽过程中内容会随高度重新布局（滚动位置由各自的 `scrollState` 保存，不受影响）。
 *
 * **手势**：拖拽条与标题区可直接拖；内容区滚到顶后继续下拉才带动抽屉
 * （`onPostScroll` 只在列表到顶时接管），内容还没到顶时下拉只滚内容。
 * 向上拖过满档、向下拖过屏幕底部都会进入阻尼区。
 *
 * **滚动**：内容区外层是固定高度 + `weight(1f)`，约束有界，长内容一定可以滚。
 * 底部内边距取 `safeDrawing` 下沿，三键导航（48 dp 级）与手势条（24 dp 级）都不会遮挡最后一行。
 *
 * @param snapPoints 停靠档位，默认 Peek/Half/Full 三档；只用一个档位时退化为「全高 + 下滑关闭」。
 * @param initialSnap 打开时停在哪一档，默认最低档。
 * @param contentScrollable 内容区是否由本组件接管滚动。传 false 表示内容自己会滚（`LazyColumn`），
 *   此时上下渐隐也不画——渐隐读的是本组件那套 `scrollState`，与列表自己的滚动位置无关。
 */
@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    snapPoints: List<AppSheetHeight> = listOf(
        AppSheetHeight.Peek,
        AppSheetHeight.Half,
        AppSheetHeight.Full,
    ),
    initialSnap: AppSheetHeight? = null,
    dismissOnScrimClick: Boolean = true,
    contentScrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = tokens.motion.sheetMs, easing = AppEaseOut),
        label = "appSheetEnter",
    )
    val scrollState = rememberScrollState()
    // 面板让出系统导航条后是「悬浮」的，四角都收圆；手势导航贴底时下沿圆角不可见，不影响布局。
    val sheetShape = RoundedCornerShape(tokens.radii.sheet)
    val points = remember(snapPoints) {
        snapPoints.distinct().sortedBy { it.fraction }.ifEmpty { listOf(AppSheetHeight.Full) }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 正常情况 maxHeight 就是窗口高度；极少数宿主给出无界约束时退回屏幕高，避免 Infinity 传播。
            val measuredHeightPx = with(density) { maxHeight.toPx() }
            val windowHeightPx = if (measuredHeightPx.isFinite() && measuredHeightPx > 0f) {
                measuredHeightPx
            } else {
                with(density) { screenHeightDp.dp.toPx() }
            }
            // 浮层语义：三键（≈48 dp）与手势（≈24 dp）**都要**让出系统导航条，不能沿用页面「手势留 0」的规则。
            //
            // **刻意用窗口/屏幕坐标的 px，不用 dp**：Dialog 窗口在部分宿主里已被系统裁进安全区
            // （实测 `frame=[0,84][1439,3032]`），而 Compose 拿到的约束高度仍是整屏。
            // 这时「按导航条高度上抬」就抬多了，反过来「不抬」又会被导航条压住；正确做法是问
            // `rememberOverlayWindowFit` 要一个按屏幕坐标算出来的抬升量。px 是绝对值，不经二次换算。
            val windowFit = rememberOverlayWindowFit(windowHeightPx.roundToInt())
            val usableHeightPx = windowFit.usableHeightPx.takeIf { it > 0 } ?: windowHeightPx.roundToInt()
            // 档位高度按「可用高度」算：整屏高里含导航条，按它算 80% 会伸到导航条下面。
            val anchorsPx = points.map { usableHeightPx * it.fraction }
            val fullHeightPx = anchorsPx.last()
            val lowestPx = anchorsPx.first()
            val startFraction = (initialSnap ?: points.first()).fraction

            // 当前「露出高度」，px。面板高度与它同值，底边固定停在导航条之上。
            var sheetHeight by remember(usableHeightPx, startFraction) {
                mutableFloatStateOf(usableHeightPx * startFraction)
            }
            var settleJob by remember { mutableStateOf<Job?>(null) }

            fun animateHeightTo(target: Float) {
                settleJob?.cancel()
                settleJob = scope.launch {
                    animate(
                        initialValue = sheetHeight,
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) { value, _ -> sheetHeight = value }
                }
            }

            fun settle(velocityY: Float) {
                val projected = sheetHeight - velocityY * FlingProjectionSeconds
                // 只有一档时没有「下一档」可退，快速下甩就直接关；多档时快速下甩先落到更低档。
                val flingToDismiss = velocityY > DismissVelocity && lowestPx >= fullHeightPx - 1f
                if (projected < lowestPx * DismissHeightRatio || flingToDismiss) {
                    currentOnDismiss()
                    return
                }
                val target = anchorsPx.minByOrNull { abs(it - projected) } ?: fullHeightPx
                animateHeightTo(target)
            }

            fun dragBy(dy: Float) {
                settleJob?.cancel()
                val raw = sheetHeight - dy
                sheetHeight = when {
                    raw > fullHeightPx -> fullHeightPx + (raw - fullHeightPx) * RubberBandFactor
                    raw < 0f -> raw * RubberBandFactor
                    else -> raw
                }
            }

            // 手势回调做成「始终最新」：屏幕尺寸变化时 remember 里的旧闭包不会读到过期的锚点。
            val currentDrag by rememberUpdatedState<(Float) -> Unit> { dy -> dragBy(dy) }
            val currentSettle by rememberUpdatedState<(Float) -> Unit> { v -> settle(v) }
            val dragState = rememberDraggableState { delta -> currentDrag(delta) }

            val connection = remember(usableHeightPx, points) {
                object : NestedScrollConnection {
                    // 抽屉还没到最高档时，上滑先撑开抽屉，别让内容同时滚。
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (source != NestedScrollSource.UserInput) return Offset.Zero
                        if (available.y >= 0f || sheetHeight >= fullHeightPx) return Offset.Zero
                        val moved = min(fullHeightPx - sheetHeight, -available.y)
                        sheetHeight += moved
                        return Offset(0f, -moved)
                    }

                    // 内容已到顶还继续下拉，才把剩余位移转成抽屉位移（带阻尼）。
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        if (source != NestedScrollSource.UserInput) return Offset.Zero
                        if (available.y <= 0f || scrollState.canScrollBackward) return Offset.Zero
                        val raw = sheetHeight - available.y
                        sheetHeight = if (raw < 0f) raw * RubberBandFactor else raw
                        return Offset(0f, available.y)
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        if (sheetHeight < fullHeightPx - 0.5f || available.y > DismissVelocity) {
                            currentSettle(available.y)
                        }
                        return Velocity.Zero
                    }
                }
            }

            // 进场只做淡入。**任何位移/缩放都不要挂在面板上**：它们会让面板底边偏离
            // 「屏幕底 - 导航条高度」这个位置，动画中途或被打断时看起来就是沉进系统栏。
            // 位移改由上面的 offset 承担（px 精确值，只负责把面板整体抬到安全区下沿）。

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.colors.scrim.copy(alpha = tokens.colors.scrim.alpha * progress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (dismissOnScrimClick) currentOnDismiss() },
                    ),
            )

            Column(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    // 整个面板抬到系统导航条之上（三键 ≈48 dp，手势 ≈24 dp）。留白必须加在**面板外层**：
                    // 只加在滚动内容上时，面板背景照样铺到屏幕底、被 NavigationBar0 那条窗口压住，
                    // 表现为「抽屉沉进导航栏、底边和圆角看不见、系统键图标压字」。
                    .offset { IntOffset(0, -windowFit.bottomLiftPx) }
                    .widthIn(max = SheetMaxWidth)
                    .fillMaxWidth()
                    // **面板高度 = 当前可见高度**，而不是「恒为满档高度再整体下移」。
                    // 早先用固定高度 + 向下偏移，面板底边会跑到屏幕下方：可见区域的下沿始终是屏幕底边，
                    // 于是最后一行正好压在系统导航栏下面（三键导航时点不中、也看不见）。
                    // 改成随档位改高度后，面板底边永远停在导航条之上，其余内容靠内部滚动去看。
                    .height(with(density) { sheetHeight.coerceAtLeast(1f).toDp() })
                    .graphicsLayer { alpha = progress }
                    .nestedScroll(connection)
                    .clip(sheetShape)
                    .background(tokens.colors.panel)
                    .border(tokens.radii.stroke, tokens.colors.border, sheetShape)

                    // 吞掉面板内点击，避免穿透到遮罩触发「点外部关闭」。
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = tokens.spacing.s2, bottom = tokens.spacing.s1)
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> currentSettle(velocity) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = tokens.spacing.s1)
                            .clip(CircleShape)
                            .background(tokens.colors.borderStrong),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = tokens.spacing.s4, end = tokens.spacing.s2)
                        // 标题区同样可下拉，热区比 16 dp 的拖拽条宽得多。
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> currentSettle(velocity) },
                        ),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(top = tokens.spacing.s2)) {
                        Text(text = title, style = tokens.type.titleS, color = tokens.colors.text)
                        subtitle?.let {
                            Text(
                                text = it,
                                style = tokens.type.bodyS,
                                color = tokens.colors.textMuted,
                                modifier = Modifier.padding(top = tokens.spacing.s1),
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(tokens.spacing.hitMin),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.common_close),
                            modifier = Modifier.size(tokens.spacing.s5),
                            tint = tokens.colors.text,
                        )
                    }
                }
                HorizontalDivider(color = tokens.colors.border, thickness = tokens.radii.highlight)
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            // [contentScrollable] = false 时由调用方自己放 LazyColumn(weight(1f))：
                            // 懒加载列表在 verticalScroll 里拿到的是无界高度，会直接抛测量异常。
                            .then(if (contentScrollable) Modifier.verticalScroll(scrollState) else Modifier)
                            // 面板已整体让出导航条，这里只留常规内容留白。
                            .padding(
                                start = tokens.spacing.s4,
                                end = tokens.spacing.s4,
                                top = tokens.spacing.s4,
                                bottom = tokens.spacing.s4,
                            ),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        content = content,
                    )
                    if (contentScrollable) Canvas(modifier = Modifier.fillMaxSize()) {
                        val fadePx = 24.dp.toPx()
                        if (scrollState.canScrollBackward) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(tokens.colors.panel, Color.Transparent),
                                    startY = 0f,
                                    endY = fadePx,
                                ),
                            )
                        }
                        if (scrollState.canScrollForward) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, tokens.colors.panel),
                                    startY = this.size.height - fadePx,
                                    endY = this.size.height,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
