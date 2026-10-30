package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterChip
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPillIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadiiDefault
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSpacingDefault
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTypeDefault
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.concentricRadius
import com.woshiwangnima.healthdietpro.model.container.ContainerCategory
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.ContainerRepository
import com.woshiwangnima.healthdietpro.model.container.capacityMlAtHeightPercent
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val CONTAINER_IMAGE_ROTATION_MS = 3_000L

/**
 * 容器列表：添加 → 两组筛选 → 卡片列表。
 *
 * 旧版把两组筛选各自封进一条 40 dp 的 `Surface` 灰条，条内再塞一个 28 dp 的小胶囊，
 * 结果是「条 → 胶囊」两层底叠加、标签比选项还显眼，且 28 dp 的可点高度低于 §2.2 的 48 dp。
 * 现在标签退成 `caption` 小字，选项直接用 `AppFilterChip`（视觉 32 dp、热区 48 dp）。
 *
 * 空态分两种：一条都没有（去添加）、筛完没有（改筛选），文案与出口都不同。
 */
@Composable
internal fun ContainerListScreen(
    containers: List<ContainerRecord>,
    scenarioTags: List<String>,
    selectedCategories: Set<ContainerCategory>,
    selectedScenarioTags: Set<String>,
    onToggleCategory: (ContainerCategory) -> Unit,
    onToggleScenarioTag: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ContainerRecord) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    var pendingDelete by remember { mutableStateOf<ContainerRecord?>(null) }
    val context = LocalContext.current
    val filtered = remember(containers, selectedCategories, selectedScenarioTags) {
        containers.filter { container ->
            (selectedCategories.isEmpty() || container.category in selectedCategories) &&
                (selectedScenarioTags.isEmpty() || container.scenarioTags.any { it in selectedScenarioTags })
        }
    }
    BaseScreen(
        title = stringResource(R.string.container_title),
        onBack = onBack,
        actions = {
            AppPillIconTextButton(
                text = stringResource(R.string.container_settings_title),
                iconRes = R.drawable.ic_settings,
                onClick = onSettings,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding)
                .padding(horizontal = tokens.spacing.page),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
        ) {
            AppIconTextButton(
                stringResource(R.string.container_add),
                R.drawable.ic_add,
                onAdd,
                Modifier.fillMaxWidth(),
            )
            val categoryTags = ContainerCategory.entries
                .filter { it != ContainerCategory.CUSTOM }
                .map { it.name to stringResource(it.labelRes()) }
            ContainerFilterRow(
                title = stringResource(R.string.container_system_categories),
                tags = categoryTags,
                selected = selectedCategories.map { it.name }.toSet(),
                onToggle = { onToggleCategory(ContainerCategory.valueOf(it)) },
            )
            if (scenarioTags.isNotEmpty()) {
                ContainerFilterRow(
                    title = stringResource(R.string.container_scenario_tags),
                    tags = scenarioTags.map { it to it },
                    selected = selectedScenarioTags,
                    onToggle = onToggleScenarioTag,
                )
            }
            if (filtered.isEmpty()) {
                val emptyAll = containers.isEmpty()
                AppEmptyState(
                    title = stringResource(if (emptyAll) R.string.container_empty else R.string.container_filter_empty),
                    description = stringResource(
                        if (emptyAll) R.string.container_empty_hint else R.string.container_filter_empty_hint,
                    ),
                    iconRes = R.drawable.ic_container,
                    action = if (emptyAll) {
                        { AppIconTextButton(stringResource(R.string.container_add), R.drawable.ic_add, onAdd) }
                    } else {
                        null
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = tokens.spacing.s4),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                ) {
                    items(filtered, key = { it.id }) { container ->
                        ContainerListCard(container, onEdit, { pendingDelete = container })
                    }
                }
            }
        }
    }
    pendingDelete?.let { container ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.container_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.container_delete_confirm_message,
                        container.name.ifBlank { context.getString(container.category.labelRes()) },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(container.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.body_record_delete), color = tokens.colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
            },
        )
    }
}

/** 一组筛选：一行 `caption` 标签 + 一行可横滚的 `AppFilterChip`。 */
@Composable
private fun ContainerFilterRow(
    title: String,
    tags: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            modifier = Modifier.padding(start = tokens.spacing.s1),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tags.forEach { (id, label) ->
                AppFilterChip(
                    text = label,
                    selected = id in selected,
                    onClick = { onToggle(id) },
                )
            }
        }
    }
}

@Composable
private fun ContainerListCard(
    container: ContainerRecord,
    onEdit: (ContainerRecord) -> Unit,
    onRequestDelete: (ContainerRecord) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .then(Modifier.clickable { onEdit(container) }),
    ) {
        ContainerCardRow(container, showDelete = true, onRequestDelete = { onRequestDelete(container) })
    }
}

/**
 * 记容器卡片内容：缩略图 + 名称 / 分类 / 容量 / 质量 / 场景标签，可选删除按钮。
 *
 * **共享组件**（容器设置、记饮食的容器选择抽屉都用它），因此颜色只读 `MaterialTheme`、
 * 间距与字号只读与主题无关的 `AppSpacingDefault` / `AppTypeDefault`：
 * 迁到 `AppTheme` 的屏取到令牌值，没迁的屏取到旧主题值，两边都不会串色（§8.1 的共享组件规则）。
 */
@Composable
internal fun ContainerCardRow(
    container: ContainerRecord,
    showDelete: Boolean,
    onRequestDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val volumeUnit = AppPrefs.getUnit(context, UnitCategoryType.Volume.id, UnitCategoryType.Volume.defaultUnitId)
    val weightUnit = AppPrefs.getUnit(context, UnitCategoryType.Weight.id, UnitCategoryType.Weight.defaultUnitId)
    val capacityMl = container.capacityMlAtHeightPercent(100.0) ?: container.capacityMl
    val mutedColor = LocalAppTokens.current.colors.textMuted
    Row(
        Modifier
            .fillMaxWidth()
            .padding(AppSpacingDefault.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacingDefault.s3),
    ) {
        ContainerThumbnail(container)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacingDefault.s1)) {
            Text(
                text = container.name.ifBlank { context.getString(container.category.labelRes()) },
                style = AppTypeDefault.body,
                color = LocalAppTokens.current.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(container.category.labelRes()),
                style = AppTypeDefault.caption,
                color = mutedColor,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    R.string.container_capacity_value,
                    fromMl(capacityMl, volumeUnit),
                    volumeUnitSymbol(volumeUnit),
                ),
                style = AppTypeDefault.caption,
                color = mutedColor,
                maxLines = 1,
            )
            container.emptyMassGrams?.let { grams ->
                Text(
                    text = stringResource(
                        R.string.container_empty_mass_value,
                        fromGrams(grams, weightUnit),
                        weightUnitSymbol(weightUnit),
                    ),
                    style = AppTypeDefault.caption,
                    color = mutedColor,
                    maxLines = 1,
                )
            }
            if (container.scenarioTags.isNotEmpty()) {
                Text(
                    text = container.scenarioTags.joinToString(" · "),
                    style = AppTypeDefault.caption,
                    color = LocalAppTokens.current.colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDelete) {
            IconButton(onClick = { onRequestDelete?.invoke() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.body_record_delete),
                    tint = LocalAppTokens.current.colors.danger,
                )
            }
        }
    }
}

@Composable
private fun ContainerThumbnail(container: ContainerRecord) {
    val paths = container.imagePaths
    var index by remember(container.id, paths) { mutableIntStateOf(0) }
    LaunchedEffect(container.id, paths) {
        if (paths.size > 1) {
            while (isActive) {
                delay(CONTAINER_IMAGE_ROTATION_MS)
                index = (index + 1) % paths.size
            }
        }
    }
    // 缩略图与卡片同心圆角：外 4 dp 卡角、内缩 0，内层取 4 dp（§2.5 同心圆角）。
    val thumbShape = RoundedCornerShape(concentricRadius(AppRadiiDefault.container, 0.dp))
    if (paths.isEmpty()) {
        Surface(Modifier.size(ContainerThumbSize).clip(thumbShape), color = LocalAppTokens.current.colors.panelRaised) {}
        return
    }
    val currentPath = paths[index.coerceIn(0, paths.size - 1)]
    val context = LocalContext.current
    val repository = remember { ContainerRepository.fromContext(context) }
    val bitmap = remember(currentPath) { repository.loadImage(currentPath) }
    Box(Modifier.size(ContainerThumbSize).clip(thumbShape)) {
        if (bitmap == null) {
            Surface(Modifier.fillMaxSize(), color = LocalAppTokens.current.colors.panelRaised) {}
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 列表缩略图边长：与两行文字的卡片高度接近，不抢标题。 */
private val ContainerThumbSize = 56.dp

internal fun ContainerCategory.labelRes(): Int = when (this) {
    ContainerCategory.CUP -> R.string.container_category_cup
    ContainerCategory.BOWL -> R.string.container_category_bowl
    ContainerCategory.PLATE -> R.string.container_category_plate
    ContainerCategory.SPOON -> R.string.container_category_spoon
    ContainerCategory.BOTTLE -> R.string.container_category_bottle
    ContainerCategory.CUSTOM -> R.string.container_category_custom
}
