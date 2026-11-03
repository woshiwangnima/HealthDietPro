package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.archive.stableJsonString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_DISPLAY_LINE_LENGTH = 200

private fun displayLine(line: String): String = when {
    line.isEmpty() -> " "
    line.length <= MAX_DISPLAY_LINE_LENGTH -> line
    else -> line.take(MAX_DISPLAY_LINE_LENGTH) + "..."
}

/**
 * 只读 JSON 文本面板：行号 + 大括号折叠 + 横向滚动。
 *
 * 「明文预览」的原文视图就用它——预览最怕的是「只给你一段不可折叠的 2 万行字」，
 * 行号负责定位，折叠负责把 `attachments` 那种几十兆的 base64 收起来。
 */
@Composable
internal fun JsonTextPanel(content: String, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val formatted by produceState(initialValue = "", content) {
        value = withContext(Dispatchers.Default) { stableJsonString(content, prettyPrint = true) }
    }
    val lines = remember(formatted) { formatted.lines() }
    val foldRanges = remember(lines) { findFoldRanges(lines) }
    var collapsedStarts by remember(lines) { mutableStateOf(emptySet<Int>()) }
    val visibleLines = lines.indices.filter { index ->
        collapsedStarts.none { start -> index > start && index <= requireNotNull(foldRanges[start]) }
    }
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "${lines.size} ${stringResource(R.string.plain_text_preview_lines)} · ${formatted.length} ${stringResource(R.string.plain_text_preview_characters)}",
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            color = tokens.colors.panelRaised.copy(alpha = 0.4f),
        ) {
            val horizontalScroll = rememberScrollState()
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(visibleLines, key = { _, lineIndex -> lineIndex }) { _, index ->
                    val line = lines[index]
                    val foldEnd = foldRanges[index]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScroll)
                            .padding(vertical = 1.dp),
                    ) {
                        if (foldEnd != null) {
                            Icon(
                                imageVector = if (index in collapsedStarts) {
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = null,
                                modifier = Modifier.width(20.dp).clickable {
                                    collapsedStarts = if (index in collapsedStarts) {
                                        collapsedStarts - index
                                    } else {
                                        collapsedStarts + index
                                    }
                                },
                                tint = tokens.colors.textMuted,
                            )
                        } else {
                            Text("", modifier = Modifier.width(20.dp))
                        }
                        Text(
                            text = (index + 1).toString(),
                            modifier = Modifier
                                .width(44.dp)
                                .background(tokens.colors.panelRaised)
                                .padding(end = 8.dp),
                            fontFamily = FontFamily.Monospace,
                            color = tokens.colors.textMuted,
                        )
                        Text(
                            text = displayLine(line),
                            fontFamily = FontFamily.Monospace,
                            style = tokens.type.bodyS,
                        )
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
        if ((line.trimStart().startsWith("}") || line.trimStart().startsWith("]")) && stack.isNotEmpty()) {
            ranges[stack.removeLast()] = index
        }
    }
    return ranges
}
