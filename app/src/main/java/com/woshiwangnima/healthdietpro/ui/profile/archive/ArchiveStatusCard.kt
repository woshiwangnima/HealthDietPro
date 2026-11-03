package com.woshiwangnima.healthdietpro.ui.profile.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppColorTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPillIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.archive.ArchiveNode
import com.woshiwangnima.healthdietpro.model.archive.UserArchiveIntegrity
import com.woshiwangnima.healthdietpro.model.archive.UserArchiveSnapshot
import kotlin.math.roundToInt

private enum class ArchivePart { RECORDS, PREFERENCES, ATTACHMENTS }

private data class PartSlice(val part: ArchivePart, val bytes: Long)

private fun splitParts(snapshot: UserArchiveSnapshot): List<PartSlice> {
    val files = snapshot.files
    fun pick(part: ArchivePart, match: (ArchiveNode) -> Boolean) = PartSlice(
        part = part,
        bytes = files.filter(match).sumOf(ArchiveNode::byteSize),
    )
    return listOf(
        pick(ArchivePart.RECORDS) { it.domainId != null && it.domainId != "user_preferences" },
        pick(ArchivePart.PREFERENCES) { it.domainId == "user_preferences" },
        // 附件节点没有 domainId，落在 attachments/ 目录树里。
        pick(ArchivePart.ATTACHMENTS) { it.domainId == null },
    )
}

@Composable
private fun ArchivePart.label(): String = stringResource(
    when (this) {
        ArchivePart.RECORDS -> R.string.archive_part_records
        ArchivePart.PREFERENCES -> R.string.archive_domain_user_preferences
        ArchivePart.ATTACHMENTS -> R.string.archive_folder_attachments
    },
)

@Composable
private fun ArchivePart.color(): Color {
    val colors = LocalAppTokens.current.colors
    return when (this) {
        ArchivePart.RECORDS -> colors.primary
        ArchivePart.PREFERENCES -> colors.info
        ArchivePart.ATTACHMENTS -> colors.warning
    }
}

/**
 * 本机档案的概览卡，固定在「导出 / 导入」切换栏上方，两个分区共用。
 *
 * 环图只表达「本机领域文件的纳入率」，堆叠条只表达「还原成目录后的体积构成」——两者口径不同，
 * 不混在一张图里：前者分母是「已纳入 + 未纳入的本机领域文件」，不以注册表全部领域为分母
 * （没有数据的领域不写文件，那样算会让完整档案天然显示不满 100%）；后者取自 snapshot 的文件树。
 * 未纳入档案的孤儿文件没有字节数，绝不伪造进堆叠条，只用红字单独报告。
 */
@Composable
internal fun ArchiveStatusCard(state: ArchiveTransferUiState, onReload: () -> Unit) {
    val tokens = LocalAppTokens.current
    val snapshot = state.snapshot
    var selectedPart by remember { mutableStateOf<ArchivePart?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AppSectionCard(
        iconRes = R.drawable.ic_shield,
        title = stringResource(R.string.archive_status_title),
        action = {
            AppPillIconTextButton(
                text = stringResource(R.string.archive_reload),
                iconRes = R.drawable.ic_refresh,
                onClick = onReload,
                enabled = !state.loading,
            )
        },
    ) {
        if (snapshot == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
            ) {
                CoverageRing(
                    ratio = 0f,
                    ringColor = tokens.colors.disabled,
                    centerValue = "-",
                    centerLabel = stringResource(R.string.archive_status_coverage),
                )
                Text(
                    text = stringResource(
                        if (state.loading) R.string.archive_status_loading else R.string.archive_status_missing,
                    ),
                    style = tokens.type.bodyS,
                    color = if (state.loading) tokens.colors.textMuted else tokens.colors.danger,
                )
            }
            return@AppSectionCard
        }

        val slices = remember(snapshot) { splitParts(snapshot) }
        // 分母是「本机实际存在的领域文件」，不是注册表全部领域：
        // 没有数据的领域不会写文件，用注册表当分母会让一份完整档案天然显示不满 100%。
        val present = snapshot.files.count { it.domainId != null }
        val uncovered = state.unregisteredFiles.size
        val denominator = (present + uncovered).coerceAtLeast(1)
        val ratio = present.toFloat() / denominator

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
        ) {
            CoverageRing(
                ratio = ratio,
                ringColor = if (uncovered > 0) tokens.colors.danger else state.integrity.ringColor(tokens.colors),
                centerValue = "${(ratio * 100).roundToInt()}%",
                centerLabel = stringResource(R.string.archive_status_coverage),
            )
            Column(modifier = Modifier.weight(1f)) {
                IntegrityBadge(state.integrity)
                Spacer(Modifier.height(tokens.spacing.s2))
                StatLine(
                    label = stringResource(R.string.archive_label_size),
                    value = formatArchiveSize(snapshot.totalBytes),
                )
                StatLine(
                    label = stringResource(R.string.archive_status_file_count),
                    value = "${snapshot.fileCount} ${stringResource(R.string.archive_label_files)}",
                )
                StatLine(
                    label = stringResource(R.string.archive_status_domain_count),
                    value = "$present / $denominator",
                )
            }
        }

        Spacer(Modifier.height(tokens.spacing.s3))
        CompositionHeader(expanded) { expanded = !expanded }
        Spacer(Modifier.height(tokens.spacing.s1))
        CompositionBar(slices, selectedPart)
        CompositionLegend(slices, selectedPart) { selectedPart = it }

        if (state.unregisteredFiles.isNotEmpty()) {
            Spacer(Modifier.height(tokens.spacing.s1))
            UncoveredFilesLine(state.unregisteredFiles)
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            ArchiveDetails(snapshot, state.integrity)
        }
    }
}

@Composable
private fun CoverageRing(ratio: Float, ringColor: Color, centerValue: String, centerLabel: String) {
    val tokens = LocalAppTokens.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(ratio) {
        progress.animateTo(ratio.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing))
    }
    Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = tokens.colors.border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (progress.value > 0.001f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = tokens.type.title,
                color = tokens.colors.text,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = centerLabel,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun IntegrityBadge(integrity: UserArchiveIntegrity) {
    val tokens = LocalAppTokens.current
    val color = integrity.badgeColor(tokens.colors)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.radii.pill))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(
            text = stringResource(integrity.labelRes()),
            style = tokens.type.caption,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = value,
            style = tokens.type.numeric,
            color = tokens.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompositionBar(slices: List<PartSlice>, selected: ArchivePart?) {
    val tokens = LocalAppTokens.current
    val total = slices.sumOf(PartSlice::bytes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(tokens.radii.label))
            .background(tokens.colors.border),
    ) {
        if (total <= 0L) return@Row
        slices.filter { it.bytes > 0L }.forEach { slice ->
            val color = slice.part.color()
            Box(
                modifier = Modifier
                    .weight(slice.bytes.toFloat() / total.toFloat())
                    .fillMaxHeight()
                    .background(if (selected != null && selected != slice.part) color.copy(alpha = 0.3f) else color),
            )
        }
    }
}

@Composable
private fun CompositionLegend(
    slices: List<PartSlice>,
    selected: ArchivePart?,
    onSelect: (ArchivePart?) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val total = slices.sumOf(PartSlice::bytes).coerceAtLeast(1L)
    Row(modifier = Modifier.fillMaxWidth()) {
        slices.forEach { slice ->
            val active = selected == slice.part
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(tokens.radii.label))
                    .clickable { onSelect(if (active) null else slice.part) }
                    .padding(horizontal = tokens.spacing.s1, vertical = tokens.spacing.s2),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(slice.part.color()))
                    Spacer(Modifier.width(tokens.spacing.s1))
                    Text(
                        text = slice.part.label(),
                        style = tokens.type.caption,
                        color = if (active) tokens.colors.primary else tokens.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${formatArchiveSize(slice.bytes)} · " +
                        "${(slice.bytes * 100f / total.toFloat()).roundToInt()}%",
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun UncoveredFilesLine(files: List<String>) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radii.label))
            .background(tokens.colors.dangerFill.copy(alpha = 0.16f))
            .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tokens.colors.danger,
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Text(
            text = stringResource(R.string.archive_status_uncovered, files.size) +
                " · " + files.joinToString(", "),
            style = tokens.type.caption,
            color = tokens.colors.danger,
        )
    }
}

@Composable
private fun CompositionHeader(expanded: Boolean, onToggle: () -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.archive_status_composition),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(tokens.radii.label))
                .clickable(onClick = onToggle)
                .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (expanded) R.string.archive_status_hide_details else R.string.archive_status_show_details,
                ),
                style = tokens.type.caption,
                color = tokens.colors.primary,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                tint = tokens.colors.primary,
            )
        }
    }
}

@Composable
private fun ArchiveDetails(snapshot: UserArchiveSnapshot, integrity: UserArchiveIntegrity) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
    ) {
        DetailLine(
            label = stringResource(R.string.archive_label_user),
            value = snapshot.userName.ifBlank { stringResource(R.string.archive_unnamed_user) },
        )
        DetailLine(
            label = stringResource(R.string.archive_label_format_version),
            value = snapshot.formatVersion.toString(),
        )
        DetailLine(
            label = stringResource(R.string.archive_label_app_version),
            value = snapshot.appVersion,
        )
        DetailLine(
            label = stringResource(R.string.archive_label_generated_at),
            value = formatArchiveTimestamp(snapshot.exportedAtMillis),
        )
        DetailLine(
            label = stringResource(R.string.archive_status_integrity),
            value = stringResource(integrity.labelRes()),
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val tokens = LocalAppTokens.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = tokens.type.bodyS,
            color = tokens.colors.textMuted,
            modifier = Modifier.width(88.dp),
        )
        Text(
            text = value,
            style = tokens.type.bodyS,
            color = tokens.colors.text,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun UserArchiveIntegrity.ringColor(colors: AppColorTokens): Color = when (this) {
    UserArchiveIntegrity.HEALTHY -> colors.success
    UserArchiveIntegrity.MANIFEST_REBUILT -> colors.warning
    UserArchiveIntegrity.INCONSISTENT, UserArchiveIntegrity.MISSING -> colors.danger
}

private fun UserArchiveIntegrity.badgeColor(colors: AppColorTokens): Color = ringColor(colors)

private fun UserArchiveIntegrity.labelRes(): Int = when (this) {
    UserArchiveIntegrity.HEALTHY -> R.string.archive_status_healthy
    UserArchiveIntegrity.MANIFEST_REBUILT -> R.string.archive_status_manifest_rebuilt
    UserArchiveIntegrity.INCONSISTENT -> R.string.archive_status_inconsistent
    UserArchiveIntegrity.MISSING -> R.string.archive_status_missing
}
