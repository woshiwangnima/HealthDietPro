package com.woshiwangnima.healthdietpro.ui.test

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens

/**
 * 测试指令的界面入口：**一个记录功能一层**。
 *
 * 以前所有指令挤在一页里，记便签的指令加上自带参数之后，页面长度与滚动位置都不再可控，
 * 也很难说清「这一条属于哪个功能」。现在这里只负责分组：每组自己的参数、自己的页面，
 * 点进去是一条与记录功能一一对应的路。
 */
internal enum class TestCommandGroup(
    val title: String,
    @DrawableRes val iconRes: Int,
    val summary: String,
) {
    BODY("体征记录", R.drawable.ic_chart, "身高、体重"),
    HEALTH("健康记录", R.drawable.ic_blood_glucose, "用药、血糖、饮水"),
    NUTRITION("营养与搜索", R.drawable.ic_nav_nutrition, "食材 / 食物 / 菜肴、搜索历史"),
    MEMO("记便签", R.drawable.ic_memo, "笔记文件夹、笔记（含语音与历史版本）、待办"),
    PRICE("记物价", R.drawable.ic_price, "物价记录、合并区间、物品走势"),
}

@Composable
internal fun TestCommandGroupsScreen(
    onOpen: (TestCommandGroup) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    BaseScreen(title = "测试指令", onBack = onBack, includeStatusBarPadding = false) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "为当前用户生成随机的本地测试数据，按记录功能分组。",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
            ActionSectionCard(title = "记录功能", titleIconRes = R.drawable.ic_nav_test) {
                TestCommandGroup.entries.forEach { group ->
                    GroupRow(group = group, onClick = { onOpen(group) })
                }
            }
        }
    }
}

/** 一个分组入口：图标 + 名称 + 一句范围说明 + 右箭头，整行可点。 */
@Composable
private fun GroupRow(group: TestCommandGroup, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radii.container))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(group.iconRes),
            contentDescription = null,
            tint = tokens.colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.text,
            )
            Text(
                text = group.summary,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}
