package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

/**
 * 「会吸顶的卡片」——把一张 [AppSectionCard] 切成**头**与**体**两片，中间不留缝。
 *
 * 为什么要切：用户希望卡片里的某个动作（例如【清除筛选】）在向下滚动时仍然点得到，
 * 而 Compose 里只有**列表直属**的条目能吸顶（`stickyHeader`），卡片内部的元素是跟着整张卡一起滚的。
 * 于是把卡拆成两片：头（标题 + 那个动作）交给 `stickyHeader`，体（其余内容）留在普通 item 里。
 *
 * 两片必须看起来仍是**一张卡**，靠三件事：
 *
 * 1. 列表**不加 item 间距**（`verticalArrangement` 留空），否则两片之间会露出页面底色；
 * 2. 头片只描「上 + 左右」，体片只描「下 + 左右」——见 [drawEdges]，
 *    这样接缝处不会出现一条横贯卡片的线，把「一张卡」读成「两张叠起来的卡」（真机上肉眼可辨）；
 * 3. 头片外面裹一圈**页面底色**：吸顶时它压在别的卡上，顶部那条留白不能漏出正在滚过的内容。
 */
@Composable
fun AppSplitCardHead(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val shape = RoundedCornerShape(
        topStart = tokens.radii.container,
        topEnd = tokens.radii.container,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            // 页面底色铺在卡面之外：吸顶时这圈留白要挡住底下滚过的卡片。
            .background(tokens.colors.bg)
            .padding(top = spacing.s3)
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .drawEdges(
                color = tokens.colors.border,
                stroke = tokens.radii.stroke,
                radius = tokens.radii.container,
                topHalf = true,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.s3,
                    end = spacing.s1,
                    top = spacing.s3,
                    bottom = spacing.s2,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(spacing.s5),
                tint = tokens.colors.primary,
            )
            Spacer(Modifier.width(spacing.s2))
            Text(
                text = title,
                style = tokens.type.titleS,
                color = tokens.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s3, end = spacing.s3, bottom = spacing.s3),
            content = content,
        )
    }
}

/**
 * 卡片的**下半片**：只有下圆角，接缝那条边不描线（见 [AppSplitCardHead]）。
 *
 * 内容自带内边距的习惯与 [AppSectionCard] 一致：左右与底部各留 s3，让控件与卡缘保持同样的距离。
 */
@Composable
fun AppSplitCardBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(
        bottomStart = tokens.radii.container,
        bottomEnd = tokens.radii.container,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .drawEdges(
                color = tokens.colors.border,
                stroke = tokens.radii.stroke,
                radius = tokens.radii.container,
                topHalf = false,
            )
            .padding(start = tokens.spacing.s3, end = tokens.spacing.s3, bottom = tokens.spacing.s3),
        content = content,
    )
}

/**
 * 只描「两条横向端 + 左右」的圆角边框：上半片描上边，下半片描下边。
 *
 * `Modifier.border` 永远画完整一圈——两片各自画满时，接缝处会出现两条并排的细线，
 * 一张卡就读成两张。这里自己画一遍，再用 [clipRect] 裁掉接缝那一侧的描边。
 */
private fun Modifier.drawEdges(
    color: Color,
    stroke: Dp,
    radius: Dp,
    topHalf: Boolean,
): Modifier = drawBehind {
    val width = stroke.toPx()
    val corner = CornerRadius(radius.toPx())
    val inset = width / 2f
    // 接缝那一侧的圆角必须跑到可视区外面：两片各带两个圆角时，接缝处会空出一段没有描边的
    // 弧线区，看上去就是「两张卡叠着放」而不是一张卡。把矩形往接缝方向多画一个圆角半径，
    // 再用 clipRect 裁掉，留下的就只有直边。
    val overflow = corner.x
    clipRect(
        left = 0f,
        top = if (topHalf) 0f else width,
        right = size.width,
        bottom = if (topHalf) size.height - width else size.height,
    ) {
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, if (topHalf) inset else inset - overflow),
            size = Size(size.width - width, size.height - width + overflow),
            cornerRadius = corner,
            style = Stroke(width = width),
        )
    }
}
