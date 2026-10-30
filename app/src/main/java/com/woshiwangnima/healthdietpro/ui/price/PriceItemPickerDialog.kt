package com.woshiwangnima.healthdietpro.ui.price

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSheetHeight
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.price.PriceItemSource

/** 物品选择结果：要么是库内食材，要么是用户当场写的名字。 */
internal data class PriceItemSelection(
    val source: PriceItemSource,
    val itemId: String,
    val name: String,
)

/**
 * 物品选择弹层：可直接挑库内食材/菜肴，也可以在搜索词上「用这个名字」记自定义物品。
 *
 * 走底部抽屉（§5.2 选择器）。列表是 `LazyColumn`，所以传 `contentScrollable = false`
 * 让抽屉把滚动权交出来——抽屉默认的 `verticalScroll` 容器会给懒加载列表一个无界高度。
 */
@Composable
internal fun PriceItemPickerDialog(
    language: String,
    search: (String) -> List<FoodItem>,
    /** 物品库数量。物品库是异步加载的，加载完成后要让结果重算一次，否则会一直显示「没有匹配」。 */
    foodsCount: Int,
    onDismiss: () -> Unit,
    onPick: (PriceItemSelection) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    val results = remember(keyword, foodsCount) { search(keyword) }
    val trimmed = keyword.trim()
    val customCandidate = trimmed.isNotBlank() && results.none { it.displayName(language) == trimmed }

    val tokens = LocalAppTokens.current
    AppBottomSheet(
        title = stringResource(R.string.price_item_pick_title),
        onDismiss = onDismiss,
        initialSnap = AppSheetHeight.Full,
        contentScrollable = false,
    ) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            if (keyword.isNotBlank()) {
                                IconButton(onClick = { keyword = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.nutrition_search_clear))
                                }
                            }
                            Icon(Icons.Filled.Search, contentDescription = null)
                        }
                    },
                    placeholder = { Text(stringResource(R.string.price_item_search_hint)) },
                )
                if (customCandidate) {
                    Surface(
                        onClick = {
                            onPick(PriceItemSelection(PriceItemSource.CUSTOM, "", trimmed))
                        },
                        color = tokens.colors.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.radii.label),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = tokens.spacing.s2)
                            .heightIn(min = tokens.spacing.hitMin),
                    ) {
                        Text(
                            text = stringResource(R.string.price_item_use_custom, trimmed),
                            style = LocalAppTokens.current.type.body,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (results.isEmpty() && !customCandidate) {
                    Text(
                        text = stringResource(R.string.price_item_no_match),
                        color = LocalAppTokens.current.colors.textMuted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(results, key = { it.id }) { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPick(
                                            PriceItemSelection(
                                                source = food.kind.toPriceItemSource(),
                                                itemId = food.id,
                                                name = food.displayName(language),
                                            ),
                                        )
                                    }
                                    // 触屏行高 ≥ 56 dp（§4.6-3），两行文字以外再补足热区。
                                    .heightIn(min = 56.dp)
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(Modifier.weight(1f)) {
                                    Column {
                                        Text(
                                            text = food.displayName(language),
                                            style = LocalAppTokens.current.type.body,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = stringResource(food.kind.labelRes()),
                                            style = LocalAppTokens.current.type.bodyS,
                                            color = LocalAppTokens.current.colors.textMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}

internal fun FoodKind.toPriceItemSource(): PriceItemSource = when (this) {
    FoodKind.INGREDIENT -> PriceItemSource.INGREDIENT
    FoodKind.FOOD -> PriceItemSource.FOOD
    FoodKind.DISH -> PriceItemSource.DISH
}

internal fun FoodKind.labelRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.price_item_kind_ingredient
    FoodKind.FOOD -> R.string.price_item_kind_food
    FoodKind.DISH -> R.string.price_item_kind_dish
}

internal fun PriceItemSource.labelRes(): Int = when (this) {
    PriceItemSource.INGREDIENT -> R.string.price_item_kind_ingredient
    PriceItemSource.FOOD -> R.string.price_item_kind_food
    PriceItemSource.DISH -> R.string.price_item_kind_dish
    PriceItemSource.CUSTOM -> R.string.price_item_kind_custom
}
