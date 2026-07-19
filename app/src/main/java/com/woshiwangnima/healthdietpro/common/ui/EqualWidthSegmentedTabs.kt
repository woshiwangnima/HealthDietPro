package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class EqualWidthTab(@param:StringRes val titleRes: Int)

@Composable
fun EqualWidthSegmentedTabs(
    tabs: List<EqualWidthTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabWidth = maxWidth / tabs.size.coerceAtLeast(1)
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex.coerceIn(0, tabs.lastIndex.coerceAtLeast(0)),
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = 520f,
            ),
            label = "segmentedTabFlow",
        )
        Surface(modifier = Modifier.fillMaxWidth().height(44.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
            Box(Modifier.fillMaxWidth().height(44.dp)) {
                Box(
                    Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(44.dp)
                        .padding(3.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(9.dp)),
                )
                Row(Modifier.fillMaxWidth().height(44.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = index == selectedIndex
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(durationMillis = 120),
                            label = "segmentedTabColor",
                        )
                        Box(
                            modifier = Modifier.weight(1f).height(44.dp).clickable { onSelected(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(tab.titleRes),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
