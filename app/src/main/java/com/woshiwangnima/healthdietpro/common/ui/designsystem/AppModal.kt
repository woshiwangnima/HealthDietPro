package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

/** 模态尺寸上限（通用规范表 5.5.1-1：S 480 / M 720 / L 960）。手机上取屏宽，等于全宽。 */
enum class AppModalSize(val maxWidth: Dp) {
    S(480.dp),
    M(720.dp),
    L(960.dp),
}

/**
 * 模态对话框底层容器（通用规范 §5.2 / §5.3 / §5.5.1）。
 *
 * - 遮罩 `color.scrim`（60% 黑），点遮罩关闭；内容超过一屏时只让内容区滚动，标题与操作区固定。
 * - 右上角关闭按钮热区 48 dp；返回键与遮罩等价于关闭。
 * - 含输入字段的表单必须传 `dismissOnScrimClick = false`，只允许「取消 / 关闭」显式退出，
 *   否则手滑点到遮罩就会把已填内容全清掉（移动规范 07 踩坑九 / 检查项 09）。
 * - 打开动效：缩放 96% → 100% + 淡入，时长 `motion.base`（180 ms，ease-out）；
 *   系统关闭动画时 Compose 会按无障碍设置退化。
 * - 容器用 `panel`，内部区块卡用 `panelRaised`，保持「底 → 面板 → 浮起」的方向。
 *
 * 长图文说明类内容不要用居中弹窗，改用 `AppBottomSheet`（移动规范 05 §一）。
 */
@Composable
fun AppModal(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    size: AppModalSize = AppModalSize.M,
    dismissOnScrimClick: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = tokens.motion.baseMs, easing = AppEaseOut),
        label = "appModalEnter",
    )
    // 浮层距安全区边缘的最小距离 = space.5（§2.2），高度上限留出背景轮廓（§2.8）。
    val scrollState = rememberScrollState()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.colors.scrim.copy(alpha = tokens.colors.scrim.alpha * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (dismissOnScrimClick) onDismiss() },
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = tokens.spacing.s4, vertical = tokens.spacing.s5),
            contentAlignment = Alignment.Center,
        ) {
            val availableHeight = maxHeight
            Column(
                modifier = modifier
                    .widthIn(max = size.maxWidth)
                    .fillMaxWidth()
                    .heightIn(max = availableHeight)
                    .graphicsLayer {
                        alpha = progress
                        val scale = 0.96f + 0.04f * progress
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(shape)
                    .background(tokens.colors.panel)
                    .border(tokens.radii.stroke, tokens.colors.border, shape)
                    // 吞掉卡内点击，避免落到遮罩的「点外部关闭」上。
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = tokens.spacing.s5, end = tokens.spacing.s2, top = tokens.spacing.s2),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(top = tokens.spacing.s3)) {
                        Text(
                            text = title,
                            style = tokens.type.titleS,
                            color = tokens.colors.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = tokens.spacing.s5, vertical = tokens.spacing.s4),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        content = content,
                    )
                    // 内容超出一屏时给上下 24 px 渐隐，提示还能继续滚（§4.8）。
                    Canvas(modifier = Modifier.matchParentSize()) {
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
                if (actions != null) {
                    HorizontalDivider(color = tokens.colors.border, thickness = tokens.radii.highlight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = tokens.spacing.s4, vertical = tokens.spacing.s3),
                        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
        }
    }
}
