package com.woshiwangnima.healthdietpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppModal
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppModalSize
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens

/**
 * 「清除缓存」确认弹层：版式对齐【导入导出用户数据】-【档案状态】-【内容构成】。
 *
 * 与档案卡相同的三段结构：**总量摘要 → 构成条 → 图例**。区别只有一处：档案卡用环图表达
 * 「领域纳入率」，缓存没有可比的覆盖率（全部已注册缓存都会被清理，天然 100%），
 * 所以不画环，只把总量做成大字，把空间留给真正有信息量的构成条。
 *
 * 构成条按每个已注册缓存的字节数分段；点图例任意一行，其余段降到 30%、
 * 该行文字换主色，再点一次取消。图例文案直接取 [CacheEntryUiState.labelRes]，
 * 新增模块注册缓存后无需改本文件即可出现在这里。
 */
@Composable
internal fun CacheClearDialog(
    state: AppSettingsUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val entries = remember(state.cacheEntries) {
        state.cacheEntries.filter { it.byteCount > 0L || it.itemCount > 0 || it.readFailed }
    }
    var selectedId by remember(state.cacheEntries) { mutableStateOf<String?>(null) }
    val totalItems = entries.sumOf { it.itemCount }

    AppModal(
        title = stringResource(R.string.settings_clear_cache_confirm_title),
        subtitle = stringResource(R.string.settings_clear_cache_confirm_message),
        size = AppModalSize.S,
        onDismiss = onDismiss,
        actions = {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.settings_clear_cache_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
            AppOutlinedIconTextButton(
                text = stringResource(R.string.settings_clear_cache_confirm_ok),
                iconRes = R.drawable.ic_broom_clean,
                enabled = !state.cacheLoading && entries.isNotEmpty(),
                onClick = onConfirm,
            )
        },
    ) {
        if (state.cacheLoading) CacheScanningLine()
        if (entries.isEmpty()) {
            if (!state.cacheLoading) {
                AppEmptyState(
                    iconRes = R.drawable.ic_broom_clean,
                    title = stringResource(R.string.settings_cache_empty_title),
                    description = stringResource(R.string.settings_cache_empty_hint),
                )
            }
            return@AppModal
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.cacheSizeText,
                    style = tokens.type.titleL,
                    color = tokens.colors.text,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.settings_cache_total_items, totalItems),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                )
            }
            SafeToClearBadge()
        }

        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
            Text(
                text = stringResource(R.string.settings_cache_composition),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
            )
            CacheCompositionBar(entries = entries, selectedId = selectedId)
        }

        CacheLegend(
            entries = entries,
            selectedId = selectedId,
            onSelect = { id -> selectedId = if (selectedId == id) null else id },
        )

        CacheSafetyNote()
    }
}

@Composable
private fun CacheScanningLine() {
    val tokens = LocalAppTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = tokens.colors.primary,
        )
        Text(
            text = stringResource(R.string.settings_cache_scanning),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun SafeToClearBadge() {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.radii.pill))
            .background(tokens.colors.success.copy(alpha = 0.14f))
            .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tokens.colors.success,
        )
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(
            text = stringResource(R.string.settings_cache_safe_badge),
            style = tokens.type.caption,
            color = tokens.colors.success,
            maxLines = 1,
        )
    }
}

@Composable
private fun CacheCompositionBar(
    entries: List<CacheEntryUiState>,
    selectedId: String?,
) {
    val tokens = LocalAppTokens.current
    val totalBytes = entries.sumOf { it.byteCount }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(tokens.radii.label))
            .background(tokens.colors.border),
    ) {
        if (totalBytes <= 0L) return@Row
        entries.forEachIndexed { index, entry ->
            if (entry.byteCount <= 0L) return@forEachIndexed
            val dim = selectedId != null && selectedId != entry.id
            Box(
                modifier = Modifier
                    .weight(entry.byteCount.toFloat() / totalBytes.toFloat())
                    .fillMaxHeight()
                    .background(
                        cacheCompositionColor(index).copy(alpha = if (dim) 0.30f else 1f),
                    ),
            )
        }
    }
}

@Composable
private fun CacheLegend(
    entries: List<CacheEntryUiState>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val tokens = LocalAppTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, entry ->
            val active = selectedId == entry.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.spacing.hitMin)
                    .clip(RoundedCornerShape(tokens.radii.label))
                    .background(
                        if (active) tokens.colors.primary.copy(alpha = 0.10f) else Color.Transparent,
                    )
                    .clickable { onSelect(entry.id) }
                    .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(cacheCompositionColor(index)),
                )
                Spacer(Modifier.width(tokens.spacing.s2))
                Column(modifier = Modifier.weight(1f)) {
                    TextOverflowText(
                        text = stringResource(entry.labelRes),
                        style = tokens.type.bodyS,
                        color = if (active) tokens.colors.primary else tokens.colors.text,
                        maxLines = 1,
                    )
                    TextOverflowText(
                        text = if (entry.readFailed) {
                            stringResource(R.string.settings_cache_entry_unreadable)
                        } else {
                            stringResource(R.string.settings_cache_entry_meta, entry.itemCount, entry.percent)
                        },
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(tokens.spacing.s2))
                Text(
                    text = if (entry.readFailed) "—" else entry.sizeText,
                    style = tokens.type.bodyS,
                    color = tokens.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CacheSafetyNote() {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .padding(tokens.spacing.s2),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tokens.colors.success,
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Text(
            text = stringResource(R.string.settings_cache_dialog_note),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
    }
}

/**
 * 构成色板：与营养素的代表色无关，缓存类别只是「放在一起可读」的七个高对比色。
 * 颜色只做辅助，图例每行仍带文案、大小与百分比；超过七类循环复用，不新增令牌。
 */
@Composable
private fun cacheCompositionColor(index: Int): Color {
    val tokens = LocalAppTokens.current
    val palette = listOf(
        tokens.colors.primary,
        tokens.colors.info,
        tokens.colors.warning,
        tokens.colors.success,
        tokens.chart.fiber,
        tokens.chart.other,
        tokens.chart.carbs,
    )
    return palette[index % palette.size]
}
