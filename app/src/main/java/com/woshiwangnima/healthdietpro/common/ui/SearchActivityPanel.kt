package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (history.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(historyTitle, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onClearHistory) { Icon(painterResource(R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error); Text(androidx.compose.ui.res.stringResource(R.string.search_activity_clear_all), color = MaterialTheme.colorScheme.error) }
                }
                history.forEach { entry ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(entry, modifier = Modifier.weight(1f).clickable { onSelectHistory(entry) })
                        IconButton(onClick = { onRemoveHistory(entry) }) { Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            if (recentItems.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(recentsTitle, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onClearRecents) { Icon(painterResource(R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error); Text(androidx.compose.ui.res.stringResource(R.string.search_activity_clear_all), color = MaterialTheme.colorScheme.error) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
            Row(Modifier.fillMaxWidth().clickable { onCollapse() }.padding(vertical = 2.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
                Text(androidx.compose.ui.res.stringResource(R.string.search_activity_collapse), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 2.dp))
            }
        }
    }
}
