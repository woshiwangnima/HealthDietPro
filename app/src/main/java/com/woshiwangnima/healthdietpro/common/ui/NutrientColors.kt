package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens

/**
 * 营养素代表色的**唯一获取入口**。
 *
 * 【营养档案】-【营养素供能】的环图、【记饮食】-【统计】-【当日摄入】的四张指标卡、
 * 【食材食物菜肴详情】-【排行】的金字塔，三处都从这里取色。色值由主题令牌
 * [com.woshiwangnima.healthdietpro.common.ui.designsystem.AppChartColors.forNutrient] 决定：
 * 深浅色各一套，并已按所在面板底校准对比度。
 *
 * 新增营养素或调整某一支颜色时只改令牌一处，不在页面里散落 `Color(0x…)`，
 * 否则同一营养素会在三个页面出现三种颜色，用户会把它们读成不同的东西。
 */
@Composable
internal fun nutrientColor(nutrientId: String): Color =
    LocalAppTokens.current.chart.forNutrient(nutrientId)
