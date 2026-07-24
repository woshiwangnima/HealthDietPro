package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

/** A node in a hierarchical (multi-level) tag tree. [tag] is the full dotted path id. */
internal data class TagNode(
    val tag: String,
    val label: String,
    val children: List<TagNode> = emptyList(),
)

/**
 * Reusable multi-level tag selector. Renders the currently selected tags as removable chips and
 * an "add" entry that opens a drill-down dialog. Multiple tags at any level can be selected.
 */
@Composable
internal fun MultiLevelTagSelector(
    title: String,
    roots: List<TagNode>,
    selectedTags: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }
    val labelByTag = remember(roots) { buildLabelIndex(roots) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        selectedTags.forEach { tag ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(labelByTag[tag] ?: tag, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.tag_selector_remove),
                        modifier = Modifier.size(18.dp).clickable { onSelectionChange(selectedTags - tag) },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { picking = true },
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.tag_selector_add), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (picking) {
        TagPickerDialog(
            roots = roots,
            selectedTags = selectedTags.toSet(),
            onToggle = { tag ->
                onSelectionChange(if (tag in selectedTags) selectedTags - tag else selectedTags + tag)
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun TagPickerDialog(
    roots: List<TagNode>,
    selectedTags: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var path by remember { mutableStateOf(emptyList<TagNode>()) }
    val currentNodes = if (path.isEmpty()) roots else path.last().children
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (path.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.tag_selector_back),
                        modifier = Modifier.size(20.dp).clickable { path = path.dropLast(1) },
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (path.isEmpty()) stringResource(R.string.tag_selector_add) else path.joinToString(" / ") { it.label },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                currentNodes.forEach { node ->
                    val selected = node.tag in selectedTags
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                node.label,
                                modifier = Modifier.weight(1f).clickable { onToggle(node.tag) },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (node.children.isNotEmpty()) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp).clickable { path = path + node },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tag_selector_done)) } },
    )
}

private fun buildLabelIndex(roots: List<TagNode>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    fun walk(nodes: List<TagNode>, prefix: String) {
        nodes.forEach { node ->
            val label = if (prefix.isEmpty()) node.label else "$prefix / ${node.label}"
            map[node.tag] = label
            walk(node.children, label)
        }
    }
    walk(roots, "")
    return map
}
