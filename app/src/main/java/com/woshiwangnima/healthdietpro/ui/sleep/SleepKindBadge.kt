package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind

/**
 * 类别文本卡：类别代表色实心底 + 白字（夜间睡眠靛蓝 / 小憩暖橙）。
 *
 * 【记录】列表卡、【统计】当日明细行、【记录】页顶部的「最近一次」三处共用这一个组件，
 * 而不是各画一份「同样的圆角 + 同样的底色」：三处里只要有一处换了圆角或改了透明度，
 * 同一句「夜间睡眠」就会在两屏之间读出两种意思（小结：形状相同代表它们是同一类标签）。
 *
 * 实心（而不是淡底 + 同色字）的原因：这是卡片上唯一的类别信息，一列卡片扫下来
 * 靠色块就能分组；淡底 merg到卡背景里之后再靠文字分辨，等于把颜色这层信息浪费掉。
 */
@Composable
internal fun SleepKindBadge(
    kind: SleepKind,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    Text(
        text = stringResource(kind.labelRes()),
        style = tokens.type.caption,
        color = tokens.sleep.onSleep,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(tokens.sleep.forKind(kind == SleepKind.NIGHT_SLEEP))
            .padding(horizontal = tokens.spacing.s2, vertical = 2.dp),
    )
}
