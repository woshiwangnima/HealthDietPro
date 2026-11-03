package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 分层金字塔的一层，**与业务无关**：文案、配色、宽度全部由调用方决定。
 *
 * [weight] 是这一层**底边**占可用宽度的比例（0, 1]。组件把上一层的 weight 当作本层顶边，
 * 于是逐层变宽自然连成金字塔；想做漏斗（上宽下窄）就把 weight 递减，想做等高柱就全传 1。
 * [color] 给最终填充色（需要淡化的层由调用方自己 `copy(alpha = …)`），[labelColor] 给层内文字色，
 * [marker] 非空时拼在标题前，用来标「你在这里」之类的定位信息。
 * [valueLabel] 为空时层内只留一行标签——紧凑布局把数值区间挪到旁边的说明栏时用。
 */
@Immutable
data class PyramidBand(
    val id: String,
    val label: String,
    val valueLabel: String,
    val weight: Float,
    val color: Color,
    val labelColor: Color,
    val marker: String? = null,
)

/**
 * 通用分层金字塔：**上下位置本身就是序位**，比环形图更适合回答「我在整体哪一层」。
 *
 * 扩展点全部在参数上，组件不认识名次、营养或任何业务字段：
 *
 * - 层数任意（[bands] 给几条就画几层），层高 [bandHeight] 默认 48 dp，满足 `hitMin`；
 * - [highlightedId] 是「当前项所在层」，额外描一圈细边、且永不因点选而被淡化；
 * - [selectedBandId] + [onBandSelect] 是点选联动：选中某层后轮廓换成主色并加粗到 `stroke`，
 *   其余层降到 38% 不透明度；再点一次同一层取消。相关信息由调用方在旁边的说明栏里展示，
 *   组件本身不写死任何文案（§「数据驱动的图表 + 图例联动」）；
 * - [compact] 让层内只留一行、字号收小，用于「左图右文」的窄栏；[bandHeight] 可同步压到 44～46 dp；
 * - 首次绘制按 [AppMotion.slowMs] 从中心线逐层长出（上层先、下层跟上），切数据不重播动画。
 *
 * 梯形用 `Canvas` 画 `Path` 而不是裁切 `Box`：相邻层的斜边要连续，圆角矩形拼不出金字塔轮廓。
 * 文字用 Compose `Text` 叠在画布之上，才能跟随字号缩放、被无障碍服务读到。
 */
@Composable
fun AppPyramidChart(
    bands: List<PyramidBand>,
    highlightedId: String?,
    selectedBandId: String?,
    onBandSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    bandHeight: Dp = 48.dp,
    /** 紧凑模式：层内标签用 caption 字号，且只渲染一行，适合窄栏与旁边的说明文字配对。 */
    compact: Boolean = false,
) {
    val tokens = LocalAppTokens.current
    val progress = remember { Animatable(0f) }
    // 只在层数变化时重播：切指标 / 切范围时塔形不变，反复长一遍只会拖慢「切条件 → 看结果」。
    LaunchedEffect(bands.size) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(tokens.motion.slowMs, easing = AppEaseOut))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        bands.forEachIndexed { index, band ->
            val topWeight = if (index == 0) band.weight * 0.78f else bands[index - 1].weight
            // 交错展开：第 i 层稍晚起步，整体仍在同一段 slowMs 内收尾。
            val local = ((progress.value - index * 0.09f) / 0.55f).coerceIn(0f, 1f)
            val dim = selectedBandId != null && selectedBandId != band.id && band.id != highlightedId
            val isSelected = band.id == selectedBandId
            val isHighlighted = band.id == highlightedId
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bandHeight)
                    .alpha(if (dim) 0.38f else 1f)
                    .clickable(role = Role.Button) {
                        onBandSelect(if (selectedBandId == band.id) null else band.id)
                    }
                    .semantics {
                        contentDescription = listOfNotNull(band.marker, band.label, band.valueLabel)
                            .filter { it.isNotEmpty() }
                            .joinToString(" ")
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cx = size.width / 2f
                    // 层间留一条细缝：五层连成整块会读成一个实心三角，分不清档位边界。
                    val gap = 1.5.dp.toPx()
                    val top = gap
                    val bottom = size.height - gap
                    val topHalf = size.width * topWeight * local / 2f
                    val bottomHalf = size.width * band.weight * local / 2f
                    val trapezoid = Path().apply {
                        moveTo(cx - topHalf, top)
                        lineTo(cx + topHalf, top)
                        lineTo(cx + bottomHalf, bottom)
                        lineTo(cx - bottomHalf, bottom)
                        close()
                    }
                    drawPath(trapezoid, color = band.color)
                    // 当前层常驻细描边；点选中的层换成主色 + 双倍宽度，「点了哪一层」在窄栏里也一眼可读。
                    val outlineWidth = when {
                        isSelected -> tokens.radii.stroke
                        isHighlighted -> tokens.radii.highlight
                        else -> null
                    }
                    if (outlineWidth != null) {
                        drawPath(
                            path = trapezoid,
                            color = if (isSelected) tokens.colors.primary else tokens.colors.borderStrong,
                            style = Stroke(width = outlineWidth.toPx()),
                        )
                    }
                }
                Column(
                    modifier = Modifier.alpha(local),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = listOfNotNull(band.marker, band.label).joinToString(" · "),
                        style = if (compact) tokens.type.caption else tokens.type.bodyS,
                        color = band.labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (band.valueLabel.isNotEmpty()) {
                        Text(
                            text = band.valueLabel,
                            style = tokens.type.caption,
                            color = band.labelColor.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
