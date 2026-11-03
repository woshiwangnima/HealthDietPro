package com.woshiwangnima.healthdietpro.ui.profile.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.JsonTextPanel
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.archive.ARCHIVE_ATTACHMENTS_FOLDER
import com.woshiwangnima.healthdietpro.model.archive.ArchiveCompatibility
import com.woshiwangnima.healthdietpro.model.archive.ArchiveDomains
import com.woshiwangnima.healthdietpro.model.archive.ArchiveNode
import com.woshiwangnima.healthdietpro.model.archive.ArchiveNodeKind
import com.woshiwangnima.healthdietpro.model.archive.UserArchiveSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 明文预览页：**文件树 + 原文** 两个视图共用一份快照。
 *
 * 之前的预览只有「一整段 JSON」：导出的档案把附件压成 base64 塞在同一个文件里，
 * 用户翻三百行也看不出「这份档案里到底有什么」。现在先按导入后真正落盘的目录结构
 * 展开（`body_metrics.json`、`attachments/foods/…`），再给一个「原文」页兜底看字节。
 */
@Composable
internal fun ArchivePreviewScreen(
    preview: ArchivePreviewState,
    busy: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var openedFile by remember(preview) { mutableStateOf<ArchiveNode?>(null) }
    val file = openedFile
    if (file != null) {
        ArchiveFileScreen(node = file, onBack = { openedFile = null })
        return
    }

    var tabIndex by remember(preview) { mutableStateOf(0) }
    var expanded by remember(preview) {
        mutableStateOf(preview.snapshot.root.children.filter(ArchiveNode::isFolder).map(ArchiveNode::path).toSet())
    }

    BaseScreen(
        title = if (preview.mode == ArchivePreviewMode.IMPORT) {
            stringResource(R.string.archive_import_preview_title)
        } else {
            stringResource(R.string.archive_preview_title)
        },
        onBack = onBack,
        includeStatusBarPadding = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LocalAppTokens.current.colors.bg),
        ) {
            Column(Modifier.weight(1f)) {
                ArchiveSnapshotSummary(preview.snapshot)
                EqualWidthSegmentedTabs(
                    tabs = listOf(
                        EqualWidthTab.text(stringResource(R.string.archive_tab_files), R.drawable.ic_folder),
                        EqualWidthTab.text(stringResource(R.string.archive_tab_raw), R.drawable.ic_description),
                    ),
                    selectedIndex = tabIndex,
                    onSelected = { tabIndex = it },
                    modifier = Modifier.padding(horizontal = LocalAppTokens.current.spacing.page),
                )
                if (tabIndex == 0) {
                    ArchiveTree(
                        root = preview.snapshot.root,
                        expanded = expanded,
                        onToggle = { path ->
                            expanded = if (path in expanded) expanded - path else expanded + path
                        },
                        onOpenFile = { openedFile = it },
                        modifier = Modifier.fillMaxSize().padding(top = LocalAppTokens.current.spacing.s2),
                    )
                } else {
                    JsonTextPanel(
                        content = preview.snapshot.rawJson,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = LocalAppTokens.current.spacing.s2)
                            .padding(horizontal = LocalAppTokens.current.spacing.page),
                    )
                }
            }
            ArchivePreviewActions(
                preview = preview,
                busy = busy,
                onExport = onExport,
                onImport = onImport,
            )
        }
    }
}

@Composable
private fun ArchivePreviewActions(
    preview: ArchivePreviewState,
    busy: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.colors.panel)
            .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s3),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        when (preview.mode) {
            ArchivePreviewMode.EXPORT -> {
                if (busy) {
                    ArchiveWorkingLine()
                }
                AppIconTextButton(
                    text = stringResource(R.string.archive_export_action),
                    iconRes = R.drawable.ic_export,
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
            }
            ArchivePreviewMode.IMPORT -> {
                val newUser = preview.importMode == ArchiveImportMode.NEW_USER
                Text(
                    text = stringResource(
                        when {
                            !preview.importable -> R.string.archive_import_newer_schema
                            newUser -> R.string.archive_import_mode_new_desc
                            else -> R.string.archive_import_overwrite_warning
                        },
                    ),
                    style = tokens.type.caption,
                    color = if (preview.importable && !newUser) tokens.colors.danger else tokens.colors.textMuted,
                )
                AppOutlinedIconTextButton(
                    text = stringResource(
                        if (newUser) R.string.archive_import_new_confirm else R.string.archive_import_confirm,
                    ),
                    iconRes = R.drawable.ic_import,
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = preview.importable && !busy,
                )
                if (busy) {
                    ArchiveWorkingLine()
                }
            }
        }
    }
}

/** 落盘/导入期间的唯一反馈：按钮禁用之外还得说明「在干什么」。 */
@Composable
private fun ArchiveWorkingLine() {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(tokens.spacing.s2))
        Text(
            text = stringResource(R.string.archive_busy),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
    }
}

/** 顶部摘要：来源、时间、体积、兼容性。导入预览里这几条是「要不要覆盖」的唯一依据。 */
@Composable
private fun ArchiveSnapshotSummary(snapshot: UserArchiveSnapshot) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s2),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
    ) {
        Text(
            text = snapshot.userName.ifBlank { stringResource(R.string.archive_unnamed_user) },
            style = tokens.type.titleS,
            color = tokens.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(
                stringResource(R.string.archive_label_app_version) + " " + snapshot.appVersion,
                formatArchiveTimestamp(snapshot.exportedAtMillis),
                formatArchiveSize(snapshot.totalBytes),
                "${snapshot.fileCount} ${stringResource(R.string.archive_label_files)}",
            ).joinToString(" · "),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
        Text(
            text = stringResource(R.string.archive_label_format_version) + " " + snapshot.formatVersion.toString() +
                " · " + stringResource(snapshot.compatibility.labelRes()),
            style = tokens.type.caption,
            color = if (snapshot.compatibility == ArchiveCompatibility.NEWER) {
                tokens.colors.danger
            } else {
                tokens.colors.textMuted
            },
        )
    }
}

@Composable
private fun ArchiveTree(
    root: ArchiveNode,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onOpenFile: (ArchiveNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(root, expanded) { visibleRows(root, expanded) }
    if (rows.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.archive_tree_empty),
                style = LocalAppTokens.current.type.body,
                color = LocalAppTokens.current.colors.textMuted,
            )
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(rows, key = { it.first.path }) { (node, depth) ->
            ArchiveTreeRow(
                node = node,
                depth = depth,
                expanded = node.path in expanded,
                onToggle = { onToggle(node.path) },
                onOpenFile = { onOpenFile(node) },
            )
        }
    }
}

private fun visibleRows(root: ArchiveNode, expanded: Set<String>): List<Pair<ArchiveNode, Int>> {
    val rows = mutableListOf<Pair<ArchiveNode, Int>>()
    fun walk(node: ArchiveNode, depth: Int) {
        node.children.forEach { child ->
            rows += child to depth
            if (child.isFolder && child.path in expanded) walk(child, depth + 1)
        }
    }
    walk(root, 0)
    return rows
}

@Composable
private fun ArchiveTreeRow(
    node: ArchiveNode,
    depth: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenFile: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.hitMin)
            .clickable { if (node.isFolder) onToggle() else onOpenFile() }
            .padding(
                start = tokens.spacing.page + tokens.spacing.s4 * depth,
                end = tokens.spacing.page,
                top = tokens.spacing.s1,
                bottom = tokens.spacing.s1,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.isFolder) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = tokens.colors.textMuted,
            )
        } else {
            Spacer(Modifier.width(18.dp))
        }
        Spacer(Modifier.width(tokens.spacing.s1))
        Icon(
            painter = painterResource(node.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (node.isFolder) tokens.colors.warning else tokens.colors.primary,
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Column(Modifier.weight(1f)) {
            Text(
                text = node.displayName(),
                style = tokens.type.body,
                color = tokens.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = node.displaySubtitle()
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (node.byteSize > 0L) {
            Spacer(Modifier.width(tokens.spacing.s2))
            Text(
                text = formatArchiveSize(node.byteSize),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
            )
        }
    }
}

@Composable
private fun ArchiveFileScreen(node: ArchiveNode, onBack: () -> Unit) {
    val tokens = LocalAppTokens.current
    BackHandler { onBack() }
    BaseScreen(title = node.displayName(), onBack = onBack, includeStatusBarPadding = false) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(tokens.colors.bg)
                .padding(horizontal = tokens.spacing.page),
        ) {
            Spacer(Modifier.height(tokens.spacing.s2))
            AppSectionGroup {
                ArchiveMetaLine(stringResource(R.string.archive_label_path), node.path)
                ArchiveMetaLine(stringResource(R.string.archive_label_size), formatArchiveSize(node.byteSize))
                node.recordCount?.let {
                    ArchiveMetaLine(
                        stringResource(R.string.archive_label_records),
                        stringResource(R.string.archive_file_records, it),
                    )
                }
            }
            val text = node.text
            if (text == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.archive_file_binary),
                        style = tokens.type.body,
                        color = tokens.colors.textMuted,
                    )
                }
            } else {
                JsonTextPanel(content = text, modifier = Modifier.fillMaxSize().padding(top = tokens.spacing.s2))
            }
        }
    }
}

@Composable
private fun ArchiveMetaLine(label: String, value: String) {
    val tokens = LocalAppTokens.current
    Row(Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1)) {
        Text(label, style = tokens.type.bodyS, color = tokens.colors.textMuted, modifier = Modifier.width(96.dp))
        Text(value, style = tokens.type.bodyS, color = tokens.colors.text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ArchiveNode.displayName(): String =
    if (isFolder && name == ARCHIVE_ATTACHMENTS_FOLDER) {
        stringResource(R.string.archive_folder_attachments)
    } else {
        name
    }

@Composable
private fun ArchiveNode.displaySubtitle(): String? {
    if (isFolder) return null
    val domain = domainId?.let { domainLabel(it) } ?: return null
    val count = recordCount?.let {
        // 偏好那一份的「条」是 key-value 项，不是业务记录。
        if (domainId == "user_preferences") {
            stringResource(R.string.archive_items_count, it)
        } else {
            stringResource(R.string.archive_file_records, it)
        }
    }
    return listOfNotNull(domain, count).joinToString(" · ")
}

/** 领域名同样取自注册表：以后新增领域，预览页不用跟着改。 */
@Composable
private fun domainLabel(domainId: String): String? =
    ArchiveDomains.firstOrNull { it.id == domainId }?.let { stringResource(it.labelRes) }

private fun ArchiveNode.iconRes(): Int = when {
    isFolder -> R.drawable.ic_folder
    kind == ArchiveNodeKind.IMAGE -> R.drawable.ic_photo
    else -> R.drawable.ic_description
}

private fun ArchiveCompatibility.labelRes(): Int = when (this) {
    ArchiveCompatibility.CURRENT -> R.string.archive_compat_current
    ArchiveCompatibility.OLDER -> R.string.archive_compat_older
    ArchiveCompatibility.NEWER -> R.string.archive_compat_newer
}

internal fun formatArchiveSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

internal fun formatArchiveTimestamp(millis: Long?): String {
    val value = millis ?: return "—"
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
}
