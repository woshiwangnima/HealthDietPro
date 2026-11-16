package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woshiwangnima.healthdietpro.R

/**
 * 既有测试指令（体征 / 健康 / 营养与搜索）的独立页面。
 *
 * 这三组只按「条数」造数据，页内共用一只数量框；一条指令 = 一颗按钮，点完弹 Toast 汇报结果。
 * 与记便签 / 记物价那种「每条指令自带参数」的形态分开，是因为它们的可调维度本来就只有数量。
 */

@Composable
internal fun BodyRecordsCommandsScreen(
    onAddHeightRecord: (Int) -> Unit,
    onAddWeightRecord: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TestCommandPage(
        title = "测试指令 · 体征记录",
        description = "为当前用户生成随机身高 / 体重记录，用于验证体征曲线的取点与刻度。",
        onBack = onBack,
        modifier = modifier,
    ) {
        TestCountCard(
            title = "体征记录",
            description = "每次执行都在今天附近补一批随机值。",
            iconRes = R.drawable.ic_chart,
            buttons = listOf(
                TestCountAction("添加随机身高记录", R.drawable.ic_add, onAddHeightRecord),
                TestCountAction("添加随机体重记录", R.drawable.ic_add, onAddWeightRecord),
            ),
        )
    }
}

@Composable
internal fun HealthRecordsCommandsScreen(
    onAddMedicationRecord: (Int) -> Unit,
    onAddYesterdayGlucose: (Int) -> Unit,
    onAddTodayGlucose: (Int) -> Unit,
    onAddTodayWater: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TestCommandPage(
        title = "测试指令 · 健康记录",
        description = "为当前用户生成随机用药、血糖与饮水记录，覆盖今日 / 昨日的分布。",
        onBack = onBack,
        modifier = modifier,
    ) {
        TestCountCard(
            title = "健康记录",
            description = "血糖与饮水都落在当天，便于直接看当日汇总卡。",
            iconRes = R.drawable.ic_blood_glucose,
            buttons = listOf(
                TestCountAction("添加随机药品和用药记录", R.drawable.ic_add, onAddMedicationRecord),
                TestCountAction("添加随机昨日血糖记录", R.drawable.ic_add, onAddYesterdayGlucose),
                TestCountAction("添加随机今日血糖记录", R.drawable.ic_add, onAddTodayGlucose),
                TestCountAction("添加随机今日饮水记录", R.drawable.ic_add, onAddTodayWater),
            ),
        )
    }
}

@Composable
internal fun NutritionSearchCommandsScreen(
    onAddNutritionFoods: (Int) -> Unit,
    onAddSearchHistories: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TestCommandPage(
        title = "测试指令 · 营养与搜索",
        description = "为当前用户生成随机的自定义食材 / 食物 / 菜肴，以及搜索历史。",
        onBack = onBack,
        modifier = modifier,
    ) {
        TestCountCard(
            title = "营养与搜索数据",
            description = "一次执行会同时补食材、以它为原料的食物与菜肴；搜索历史写进记录页与营养页两个入口。",
            iconRes = R.drawable.ic_nav_nutrition,
            buttons = listOf(
                TestCountAction("添加随机食材、食物和菜肴", R.drawable.ic_add, onAddNutritionFoods),
                TestCountAction("添加随机搜索历史", R.drawable.ic_add, onAddSearchHistories),
            ),
        )
    }
}
