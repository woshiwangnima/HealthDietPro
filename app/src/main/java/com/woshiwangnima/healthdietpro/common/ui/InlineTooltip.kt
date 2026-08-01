package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A click-to-toggle inline explanation for controls with domain-specific icons. */
@Composable
internal fun InlineTooltip(
    message: String,
    modifier: Modifier = Modifier,
    trigger: @Composable (expanded: Boolean, onClick: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        trigger(expanded) { expanded = !expanded }
        if (expanded) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
