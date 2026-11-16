package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.model.price.PriceTestCommand

/**
 * 【测试指令 · 记物价】页。
 *
 * 参数都对着界面上的可见形态：合并区间卡（同键多条）、单位分行（斤 / 个不可比）、
 * 地区与月份的筛选、以及【物品历史】那条折线。造一堆单值同价记录是测不出这些的。
 */
@Composable
internal fun PriceTestScreen(
    onRun: (PriceTestCommand) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TestCommandPage(
        title = "测试指令 · 记物价",
        description = "为当前用户生成随机的物价记录：记录列表、统计区间、筛选与物品走势都用同一批数据。",
        onBack = onBack,
        modifier = modifier,
    ) {
        RecordCommands(onRun)
        TrendCommands(onRun)
    }
}

@Composable
private fun RecordCommands(onRun: (PriceTestCommand) -> Unit) {
    var count by remember { mutableStateOf("20") }
    var itemCount by remember { mutableStateOf("5") }
    var regionCount by remember { mutableStateOf("2") }
    var recentDays by remember { mutableStateOf("30") }
    var sellUnits by remember { mutableStateOf("斤,公斤,个") }
    var priceUnit by remember { mutableStateOf("元") }
    var rangePercent by remember { mutableStateOf("40") }
    var peersPerKey by remember { mutableStateOf("2") }
    var notePercent by remember { mutableStateOf("20") }
    var attachToLibrary by remember { mutableStateOf(false) }

    ActionSectionCard(title = "记物价 · 记录", titleIconRes = R.drawable.ic_price) {
        TestCommandBlock(
            title = "创建随机物价记录",
            description = "同一天同地区同物品同单位会重复几条并各自有价，列表里合并成一条区间卡；" +
                "地区、月份、单位的多样性由参数决定。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_add,
                    {
                        onRun(
                            PriceTestCommand.CreateRecords(
                                count = count.asIntOr(20),
                                itemCount = itemCount.asIntOr(5),
                                regionCount = regionCount.asIntOr(2),
                                sellUnits = sellUnits,
                                priceUnit = priceUnit,
                                recentDays = recentDays.asIntOr(30),
                                rangePercent = rangePercent.asIntOr(40),
                                peersPerKey = peersPerKey.asIntOr(1),
                                notePercent = notePercent.asIntOr(0),
                                attachToLibrary = attachToLibrary,
                            ),
                        )
                    },
                    Modifier.fillMaxWidth(),
                )
            },
            params = {
                TestParamRow {
                    TestParamNumberField("条数", count, { count = it }, Modifier.weight(1f))
                    TestParamNumberField("物品数", itemCount, { itemCount = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("地区数（0 不填）", regionCount, { regionCount = it }, Modifier.weight(1f))
                    TestParamNumberField("日期近几天", recentDays, { recentDays = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamTextField("售卖单位（逗号分隔）", sellUnits, { sellUnits = it }, Modifier.weight(1f))
                    TestParamTextField("物价单位", priceUnit, { priceUnit = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("区间记录占比（%）", rangePercent, { rangePercent = it }, Modifier.weight(1f))
                    TestParamNumberField("同键重复上限", peersPerKey, { peersPerKey = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("带备注占比（%）", notePercent, { notePercent = it }, Modifier.weight(1f))
                }
                TestParamSwitchRow(
                    "挂到库内食材 / 食物 / 菜肴（库里没有条目时用自定义名）",
                    attachToLibrary,
                    { attachToLibrary = it },
                )
            },
        )

        TestCommandBlock(
            title = "删除所有物价记录",
            description = "清空全部记录，统计与走势一并归零。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_delete,
                    { onRun(PriceTestCommand.DeleteAllRecords) },
                    Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

@Composable
private fun TrendCommands(onRun: (PriceTestCommand) -> Unit) {
    var itemName by remember { mutableStateOf("测试走势物品") }
    var days by remember { mutableStateOf("30") }
    var basePrice by remember { mutableStateOf("6.5") }
    var fluctuation by remember { mutableStateOf("12") }
    var regionCount by remember { mutableStateOf("1") }
    var sellUnit by remember { mutableStateOf("斤") }
    var priceUnit by remember { mutableStateOf("元") }

    ActionSectionCard(title = "记物价 · 走势", titleIconRes = R.drawable.ic_chart) {
        TestCommandBlock(
            title = "生成单个物品的连续走势",
            description = "逐日一条、价格随机游走，喂【统计 → 物品历史】的折线图；多地时同一天收成一段区间。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_add,
                    {
                        onRun(
                            PriceTestCommand.CreateTrend(
                                itemName = itemName,
                                days = days.asIntOr(30),
                                sellUnit = sellUnit,
                                priceUnit = priceUnit,
                                basePrice = basePrice,
                                fluctuationPercent = fluctuation.asIntOr(12),
                                regionCount = regionCount.asIntOr(1),
                            ),
                        )
                    },
                    Modifier.fillMaxWidth(),
                )
            },
            params = {
                TestParamRow {
                    TestParamTextField("物品名", itemName, { itemName = it }, Modifier.weight(1f))
                    TestParamTextField("基准价", basePrice, { basePrice = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("天数", days, { days = it }, Modifier.weight(1f))
                    TestParamNumberField("日波动率（%）", fluctuation, { fluctuation = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamTextField("售卖单位", sellUnit, { sellUnit = it }, Modifier.weight(1f))
                    TestParamTextField("物价单位", priceUnit, { priceUnit = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("地区数（0 不填）", regionCount, { regionCount = it }, Modifier.weight(1f))
                }
            },
        )
    }
}
