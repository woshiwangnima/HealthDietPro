package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.archive.stableJsonString

/** Reusable read-only preview for structured plaintext before an export action. */
@Composable
internal fun PlainTextPreviewScreen(title: String, content: String, onBack: () -> Unit) {
    val formatted = remember(content) { stableJsonString(content, prettyPrint = true) }
    val lines = remember(formatted) { formatted.lines() }
    val foldRanges = remember(lines) { findFoldRanges(lines) }
    var collapsedStarts by remember(lines) { mutableStateOf(emptySet<Int>()) }
    val visibleLines = lines.indices.filter { index -> collapsedStarts.none { start -> index > start && index <= requireNotNull(foldRanges[start]) } }
    // This preview is rendered inside MainActivity's Scaffold, which already consumes status bars.
    BaseScreen(title = title, onBack = onBack, includeStatusBarPadding = false) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text("${lines.size} ${androidx.compose.ui.res.stringResource(R.string.plain_text_preview_lines)} · ${formatted.length} ${androidx.compose.ui.res.stringResource(R.string.plain_text_preview_characters)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(modifier = Modifier.fillMaxSize().padding(top = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                val horizontalScroll = rememberScrollState()
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(visibleLines, key = { _, lineIndex -> lineIndex }) { _, index ->
                        val line = lines[index]
                        val foldEnd = foldRanges[index]
                        Row(Modifier.fillMaxWidth().horizontalScroll(horizontalScroll).padding(vertical = 1.dp)) {
                            if (foldEnd != null) {
                                Icon(if (index in collapsedStarts) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.width(20.dp).clickable { collapsedStarts = if (index in collapsedStarts) collapsedStarts - index else collapsedStarts + index }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("", modifier = Modifier.width(20.dp))
                            }
                            Text((index + 1).toString(), modifier = Modifier.width(44.dp).background(MaterialTheme.colorScheme.surfaceVariant).padding(end = 8.dp), fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(line.ifEmpty { " " }, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun findFoldRanges(lines: List<String>): Map<Int, Int> {
    val stack = ArrayDeque<Int>()
    val ranges = mutableMapOf<Int, Int>()
    lines.forEachIndexed { index, line ->
        if (line.trimEnd().endsWith("{") || line.trimEnd().endsWith("[")) stack.addLast(index)
        if ((line.trimStart().startsWith("}") || line.trimStart().startsWith("]")) && stack.isNotEmpty()) ranges[stack.removeLast()] = index
    }
    return ranges
}
