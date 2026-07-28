package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

internal data class RecentSearchItem(val id: String, val title: String, @param:DrawableRes val iconRes: Int)

@Composable
internal fun SearchActivityPanel(
    history: List<String>,
    recentItems: List<RecentSearchItem>,
    historyTitle: String,
    recentsTitle: String,
    onSelectHistory: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenRecent: (RecentSearchItem) -> Unit,
    onRemoveRecent: (RecentSearchItem) -> Unit,
    onClearRecents: () -> Unit,
    onCollapse: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (history.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(historyTitle, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onClearHistory) { Icon(painterResource(R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error); Text(androidx.compose.ui.res.stringResource(R.string.search_activity_clear_all), color = MaterialTheme.colorScheme.error) }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    val historyListState = rememberLazyListState()
                    Column {
                        LazyColumn(state = historyListState, modifier = Modifier.heightIn(max = HISTORY_ROW_HEIGHT * MAX_VISIBLE_HISTORY_ENTRIES)) {
                            items(history) { entry ->
                                Row(Modifier.fillMaxWidth().height(HISTORY_ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry, modifier = Modifier.weight(1f).clickable { onSelectHistory(entry) })
                                    IconButton(modifier = Modifier.size(36.dp), onClick = { onRemoveHistory(entry) }) { Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                        if (history.size > MAX_VISIBLE_HISTORY_ENTRIES) {
                            ScrollHint(vertical = true)
                            SearchActivityScrollIndicator(historyListState, vertical = true)
                        }
                    }
                }
            }
            if (recentItems.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(recentsTitle, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onClearRecents) { Icon(painterResource(R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error); Text(androidx.compose.ui.res.stringResource(R.string.search_activity_clear_all), color = MaterialTheme.colorScheme.error) }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    val recentListState = rememberLazyListState()
                    Column {
                        LazyRow(state = recentListState, modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentItems, key = { it.id }) { item ->
                                Surface(modifier = Modifier.width(132.dp).clickable { onOpenRecent(item) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(painterResource(item.iconRes), null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        TextOverflowText(item.title, modifier = Modifier.weight(1f).padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp).clickable { onRemoveRecent(item) }, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        if (recentItems.size > 1) {
                            ScrollHint(vertical = false)
                            SearchActivityScrollIndicator(recentListState, vertical = false)
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { onCollapse() },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.search_activity_collapse), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ScrollHint(vertical: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (vertical) Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = androidx.compose.ui.res.stringResource(if (vertical) R.string.search_activity_scroll_vertical else R.string.search_activity_scroll_horizontal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (vertical) Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchActivityScrollIndicator(state: LazyListState, vertical: Boolean) {
    val progress by remember(state, vertical) {
        derivedStateOf {
            val layout = state.layoutInfo
            val total = layout.totalItemsCount
            val visible = layout.visibleItemsInfo.size
            if (total <= visible || visible == 0) 0f else {
                state.firstVisibleItemIndex.toFloat() / (total - visible).coerceAtLeast(1)
            }
        }
    }
    val visibleFraction by remember(state, vertical) {
        derivedStateOf {
            val layout = state.layoutInfo
            if (layout.totalItemsCount == 0) 1f else {
                (layout.visibleItemsInfo.size.toFloat() / layout.totalItemsCount).coerceIn(0.16f, 1f)
            }
        }
    }
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).height(4.dp).background(trackColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
    ) {
        val thumbWidth = maxWidth * visibleFraction
        val thumbOffset = (maxWidth - thumbWidth) * progress
        androidx.compose.foundation.layout.Box(
            Modifier.width(thumbWidth).offset(x = thumbOffset).height(4.dp).background(thumbColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
        )
    }
}

private val HISTORY_ROW_HEIGHT = 40.dp
private const val MAX_VISIBLE_HISTORY_ENTRIES = 5
