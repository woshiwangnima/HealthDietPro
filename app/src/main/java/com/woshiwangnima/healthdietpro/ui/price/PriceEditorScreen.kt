package com.woshiwangnima.healthdietpro.ui.price

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.price.PriceGroup
import com.woshiwangnima.healthdietpro.model.price.PriceItemSource
import com.woshiwangnima.healthdietpro.model.price.formatPriceRange
import com.woshiwangnima.healthdietpro.model.price.priceIdentityKey
import com.woshiwangnima.healthdietpro.model.price.PriceRecord
import com.woshiwangnima.healthdietpro.model.price.formatPriceValue
import com.woshiwangnima.healthdietpro.model.price.groupByPrice
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import com.woshiwangnima.healthdietpro.model.region.RegionRepository
import com.woshiwangnima.healthdietpro.model.region.RegionSnapshot
import com.woshiwangnima.healthdietpro.util.location.CurrentLocationProvider
import java.util.UUID

/**
 * 售卖单位快捷项。文本可自由改写，这里只做一档常用值，少打字。
 * 中文 / 非中文各一套：非中文环境下列表里出现「斤」既看不懂、也不该被默认选中。
 */
private fun sellUnitPresets(language: String): List<String> = if (language.startsWith("zh")) {
    listOf("斤", "公斤", "500 克", "克", "个", "只", "份", "盒", "袋", "瓶", "把", "升")
} else {
    listOf("jin", "kg", "500 g", "g", "piece", "pack", "box", "bag", "bottle", "bunch", "L")
}

/** 物价单位（币种）快捷项，同样可自由输入。 */
private fun priceUnitPresets(language: String): List<String> = if (language.startsWith("zh")) {
    listOf("元", "角")
} else {
    listOf("CNY", "USD", "EUR", "JPY", "HKD")
}

/**
 * 记物价编辑器。
 *
 * 版式与【食材 / 食物 / 菜肴详情】一致：四张 `AppSectionCard` 分别管物品、价格、地区与日期、备注，
 * 卡内用「行 + 发丝分隔线」承载可点项（不是灰底输入框），只有真正的输入控件才带描边，
 * 避免卡里再套一层块状底、出现两种卡片明度（§2.7 结构层）。
 */
@Composable
internal fun PriceEditorScreen(
    existing: PriceRecord?,
    viewModel: PriceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    // 物品库异步加载，界面要跟着刷新（选择器里按 foods 数量做 remember key）。
    val priceState by viewModel.uiState.collectAsStateWithLifecycle()

    val provinceRepo = remember { ProvinceRepository.fromContext(context) }
    val regionRepo = remember { RegionRepository.fromContext(context) }
    val locationProvider = remember { CurrentLocationProvider(context) }

    var itemSource by rememberSaveable(existing?.id) { mutableStateOf(existing?.itemSource ?: PriceItemSource.CUSTOM) }
    var itemId by rememberSaveable(existing?.id) { mutableStateOf(existing?.itemId.orEmpty()) }
    var itemName by rememberSaveable(existing?.id) { mutableStateOf(existing?.itemName.orEmpty()) }
    var priceText by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.let { formatPriceValue(it.price) }.orEmpty())
    }
    // 最高价可空：只填一个数就是单值，填了才是区间。
    var priceMaxText by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.priceMax?.let { formatPriceValue(it) }.orEmpty())
    }
    var priceUnit by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.priceUnit ?: priceUnitPresets(language).first())
    }
    var sellUnit by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.sellUnit ?: sellUnitPresets(language).first())
    }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var dateMillis by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.recordedAt ?: todayStartMillis())
    }
    // 新建记录的默认地区 = 用户在个人资料里设置的地区，而不是现场定位。
    // 记物价大多记的是「我所在城市」的日常价，用户已经告诉过 App 自己住哪，不该每次再定位一遍；
    // 临时在外地买菜时，才在地区选择器里点「使用当前定位」覆盖。
    val profileRegion = remember { runCatching { ProfilePrefs.load(context).region }.getOrDefault(RegionSnapshot()) }
    // 新建时的初始地区，用于判断「用户有没有动过表单」。
    val initialRegion = remember(existing?.id) {
        if (existing != null) {
            RegionSnapshot(
                lng = existing.lng,
                lat = existing.lat,
                provinceCode = existing.provinceCode,
                provinceName = existing.provinceName,
                cityCode = existing.cityCode,
                cityName = existing.cityName,
                districtCode = existing.districtCode,
                districtName = existing.districtName,
            )
        } else {
            profileRegion.takeUnless { it.isEmpty() } ?: RegionSnapshot()
        }
    }
    // RegionSnapshot 不是 Parcelable，用普通 remember（配置变更后按重新选择处理）。
    var region by remember(existing?.id) { mutableStateOf(initialRegion) }

    var showItemPicker by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    fun locate() {
        locating = true
        locationError = null
        locationProvider.getCurrentLocation { result ->
            locating = false
            when (result) {
                is CurrentLocationProvider.Result.Ok -> {
                    val snapshot = regionRepo.resolve(result.lng, result.lat, provinceRepo)
                    if (snapshot.provinceCode.isNotEmpty()) {
                        region = snapshot
                    } else {
                        locationError = context.getString(R.string.price_region_location_failed_unknown)
                    }
                }
                is CurrentLocationProvider.Result.Err -> {
                    locationError = context.getString(R.string.price_region_location_failed)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else locationError = context.getString(R.string.price_region_permission_denied)
    }

    // 不再自动定位：进来直接用资料里的地区，定位只由地区选择器里的「使用当前定位」触发。
    val priceValue = priceText.trim().takeIf(String::isNotBlank)?.toDoubleOrNull()
    val priceMaxValue = priceMaxText.trim().takeIf(String::isNotBlank)?.toDoubleOrNull()
    // 填了字却解析不出数字（比如误输入两个小数点），或上限小于下限，都算无效。
    val priceMaxUnparsed = priceMaxText.isNotBlank() && priceMaxValue == null
    val priceRangeInvalid = !priceMaxUnparsed && priceMaxValue != null &&
        priceValue != null && priceMaxValue < priceValue
    // 只填一个数就是单值：填在哪一格都算（只填上限时，它就是这条记录的价格）。
    val singlePrice = priceValue ?: priceMaxValue
    val valid = itemName.isNotBlank() && singlePrice != null && singlePrice > 0.0 &&
        !priceMaxUnparsed && !priceRangeInvalid &&
        sellUnit.isNotBlank() && priceUnit.isNotBlank()

    fun buildRecord(): PriceRecord? {
        val price = singlePrice?.takeIf { it > 0.0 } ?: return null
        if (itemName.isBlank() || sellUnit.isBlank()) return null
        return PriceRecord(
            id = existing?.id ?: UUID.randomUUID().toString(),
            itemSource = itemSource,
            itemId = itemId,
            itemName = itemName.trim(),
            price = price,
            // 两格都填了才是区间；只填一格、或上下限相等时按单值存。
            priceMax = if (priceValue != null) priceMaxValue?.takeIf { it > price } else null,
            priceUnit = priceUnit.trim(),
            sellUnit = sellUnit.trim(),
            provinceCode = region.provinceCode,
            provinceName = region.provinceName,
            cityCode = region.cityCode,
            cityName = region.cityName,
            districtCode = region.districtCode,
            districtName = region.districtName,
            lng = region.lng,
            lat = region.lat,
            recordedAt = normalizeRecordTimestamp(dateMillis, RecordTimePrecision.DATE),
            note = note.trim(),
            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    val current = buildRecord()
    // 新建时不能只比「有记录 vs 没记录」——那样空表单一进来就算有改动，返回会莫名弹「放弃修改」。
    val dirtyNew = itemName.isNotBlank() || priceText.isNotBlank() || priceMaxText.isNotBlank() ||
        note.isNotBlank() || region != initialRegion ||
        sellUnit != sellUnitPresets(language).first() || priceUnit != priceUnitPresets(language).first() ||
        dateMillis != todayStartMillis()
    val hasChanges = if (existing != null) current != existing else dirtyNew
    val saveEnabled = valid && hasChanges
    // 同物品的历史价格（最多 3 条，按条件合并后），在「价格」卡下方单独成卡做对照。
    // 用 groupByPrice 而不是逐条列出：列表页卡片已经是区间，编辑页对照也要是同一个口径。
    val recentSameItem = remember(itemName, priceState.records, existing?.id) {
        priceState.records
            .filter { it.itemName == itemName.trim() && it.id != existing?.id }
            .groupByPrice()
            .take(3)
    }
    // 同地区 / 同日期 / 同物品 / 同单位的历史记录，保存后会与本条合并成一条区间卡片。
    // 用身份键算，而不是逐字段比较，避免和列表页的合并口径出现偏差。
    val mergePeers = remember(
        itemSource,
        itemId,
        itemName,
        region,
        dateMillis,
        sellUnit,
        priceUnit,
        priceState.records,
        existing?.id,
    ) {
        if (itemName.isBlank() || sellUnit.isBlank() || priceUnit.isBlank()) {
            emptyList()
        } else {
            val key = priceIdentityKey(
                itemSource = itemSource,
                itemId = itemId,
                itemName = itemName,
                provinceCode = region.provinceCode,
                cityCode = region.cityCode,
                districtCode = region.districtCode,
                recordedAt = normalizeRecordTimestamp(dateMillis, RecordTimePrecision.DATE),
                sellUnit = sellUnit,
                priceUnit = priceUnit,
            )
            priceState.records.filter { it.id != existing?.id && it.identityKey == key }
        }
    }
    // 合并后的区间预览：已有记录的上下限 + 当前输入，给用户一个「保存后会变成什么」的答案。
    val mergedPreview = if (mergePeers.isEmpty()) {
        null
    } else {
        val currentLow = singlePrice?.takeIf { it > 0.0 }
        val lows = mergePeers.map { it.priceLow } + listOfNotNull(currentLow)
        val highs = mergePeers.map { it.priceHigh } +
            listOfNotNull(currentLow?.let { low -> maxOf(low, priceMaxValue ?: low) })
        formatPriceRange(lows.min(), highs.max()) + " " + priceUnit.trim() + "/" + sellUnit.trim()
    }
    val regionText = listOf(region.provinceName, region.cityName, region.districtName)
        .filter(String::isNotBlank)
        .joinToString(" - ")

    fun commit(record: PriceRecord) {
        viewModel.upsert(record)
        onBack()
    }

    // 同条件多条不再是「重复」：它们会合并成一条区间，所以保存不再拦截确认。
    fun requestSave() {
        val record = buildRecord() ?: return
        commit(record)
    }

    fun requestBack() {
        if (hasChanges) showDiscardDialog = true else onBack()
    }

    BackHandler(onBack = ::requestBack)

    BaseScreen(
        title = stringResource(if (existing == null) R.string.price_add else R.string.price_edit),
        onBack = ::requestBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = spacing.page,
                    end = spacing.page,
                    top = spacing.s3,
                    bottom = spacing.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.s4),
            ) {
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_food_ingredient,
                        title = stringResource(R.string.price_section_item),
                        contentPadding = PaddingValues(bottom = spacing.s2),
                    ) {
                        PriceNavRow(
                            label = stringResource(R.string.price_item),
                            value = itemName,
                            placeholder = stringResource(R.string.price_item_free_hint),
                            trailing = if (itemName.isBlank()) {
                                null
                            } else {
                                {
                                    Text(
                                        text = stringResource(itemSource.labelRes()),
                                        style = tokens.type.caption,
                                        color = colors.textMuted,
                                    )
                                }
                            },
                            onClick = { viewModel.ensureFoodsLoaded(); showItemPicker = true },
                        )
                    }
                }

                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_price,
                        title = stringResource(R.string.price_section_price),
                    ) {
                        // 至少填一格：只填一个数 = 单值，填两个才是「这天这儿多少钱到多少钱」。
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                            verticalAlignment = Alignment.Top,
                        ) {
                            EditorTextField(
                                label = stringResource(R.string.price_value_min),
                                value = priceText,
                                onValueChange = { priceText = it },
                                // 两格都不标「必填 / 可选」：规则是「至少填一格」，不是哪一格必填。
                                required = false,
                                showRequirementMarker = false,
                                numeric = true,
                                modifier = Modifier.weight(1f),
                            )
                            EditorTextField(
                                label = stringResource(R.string.price_value_max),
                                value = priceMaxText,
                                onValueChange = { priceMaxText = it },
                                required = false,
                                showRequirementMarker = false,
                                numeric = true,
                                supportingTextOverride = if (priceMaxUnparsed) {
                                    { ErrorHint(stringResource(R.string.price_value_invalid)) }
                                } else {
                                    null
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (priceRangeInvalid) {
                            Spacer(Modifier.height(spacing.s1))
                            ErrorHint(stringResource(R.string.price_value_range_invalid))
                        }
                        Spacer(Modifier.height(spacing.s3))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                            verticalAlignment = Alignment.Top,
                        ) {
                            EditorTextField(
                                label = stringResource(R.string.price_sell_unit),
                                value = sellUnit,
                                onValueChange = { sellUnit = it },
                                required = true,
                                modifier = Modifier.weight(1f),
                            )
                            AppDropdownField(
                                label = stringResource(R.string.price_unit),
                                value = priceUnit,
                                options = priceUnitPresets(language).map { AppDropdownOption(it, it) },
                                onSelect = { priceUnit = it.id },
                                modifier = Modifier.weight(0.72f),
                            )
                        }
                        Spacer(Modifier.height(spacing.s2))
                        QuickUnitRow(
                            presets = sellUnitPresets(language),
                            selected = sellUnit,
                            onSelect = { sellUnit = it },
                        )
                        Spacer(Modifier.height(spacing.s3))
                        // 说明「区间怎么填」与「多条怎么合并」——这两件事在用户眼里是同一个问题。
                        Text(
                            text = stringResource(R.string.price_price_hint),
                            style = tokens.type.caption,
                            color = colors.textMuted,
                        )
                        if (mergedPreview != null) {
                            Spacer(Modifier.height(spacing.s2))
                            Text(
                                text = stringResource(
                                    R.string.price_merge_preview,
                                    mergePeers.size,
                                    mergedPreview,
                                ),
                                style = tokens.type.caption,
                                color = colors.primary,
                            )
                        }
                    }
                }

                // 同一物品在其他地区/其他日期的历史价格，录入时直接对照，避免拍脑袋填。
                if (recentSameItem.isNotEmpty()) {
                    item {
                        AppSectionCard(
                            iconRes = R.drawable.ic_time,
                            title = stringResource(R.string.price_recent_same_item),
                            contentPadding = PaddingValues(bottom = spacing.s2),
                        ) {
                            recentSameItem.forEachIndexed { index, past ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = spacing.s3),
                                        thickness = tokens.radii.highlight,
                                        color = colors.border,
                                    )
                                }
                                PriceHistoryRow(past)
                            }
                        }
                    }
                }

                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_region,
                        title = stringResource(R.string.price_section_place_time),
                        contentPadding = PaddingValues(bottom = spacing.s2),
                    ) {
                        PriceNavRow(
                            label = stringResource(R.string.price_region),
                            value = regionText,
                            placeholder = stringResource(R.string.price_region_unset),
                            trailing = if (locating) {
                                {
                                    Text(
                                        text = stringResource(R.string.price_region_locating),
                                        style = tokens.type.caption,
                                        color = colors.textMuted,
                                    )
                                }
                            } else {
                                null
                            },
                            onClick = { showRegionPicker = true },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.s3),
                            thickness = tokens.radii.highlight,
                            color = colors.border,
                        )
                        PriceNavRow(
                            label = stringResource(R.string.price_date),
                            value = formatRecordTimestamp(dateMillis, RecordTimePrecision.DATE),
                            placeholder = "",
                            trailing = if (dateMillis != todayStartMillis()) {
                                {
                                    // 「今天」自动填充：只有日期被改到别的日子时才出现，避免一个永远点不动的按钮。
                                    TodayResetChip(onClick = { dateMillis = todayStartMillis() })
                                }
                            } else {
                                null
                            },
                            onClick = { showDatePicker = true },
                        )
                        Spacer(Modifier.height(spacing.s1))
                        Text(
                            text = stringResource(R.string.price_region_source_hint),
                            style = tokens.type.caption,
                            color = colors.textMuted,
                            modifier = Modifier.padding(horizontal = spacing.s3, vertical = spacing.s1),
                        )
                    }
                }

                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_description,
                        title = stringResource(R.string.price_section_note),
                    ) {
                        EditorTextField(
                            label = stringResource(R.string.price_note),
                            value = note,
                            onValueChange = { note = it },
                            required = false,
                            singleLine = false,
                        )
                    }
                }
            }
            FormSaveBar(stringResource(R.string.price_save), saveEnabled, ::requestSave)
        }
    }

    if (showItemPicker) {
        PriceItemPickerDialog(
            language = language,
            search = { viewModel.searchFoods(it) },
            foodsCount = priceState.foods.size,
            onDismiss = { showItemPicker = false },
            onPick = { selection ->
                itemSource = selection.source
                itemId = selection.itemId
                itemName = selection.name
                showItemPicker = false
            },
        )
    }
    if (showRegionPicker) {
        PriceRegionPickerDialog(
            initial = region,
            profileRegion = profileRegion,
            locating = locating,
            locationError = locationError,
            onDismiss = { showRegionPicker = false },
            onLocate = {
                if (locationProvider.hasPermission()) locate()
                else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onPicked = {
                region = it
                showRegionPicker = false
            },
        )
    }
    if (showDatePicker) {
        ComposeDatePickerDialog(
            initialMillis = dateMillis,
            onDismiss = { showDatePicker = false },
            onDatePicked = { date ->
                dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                showDatePicker = false
            },
        )
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onBack,
            onSave = ::requestSave,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
        )
    }
}

/**
 * 卡内可点行：标签 + 当前值 + 可选尾注 + 右箭头，整行热区最小 56 dp（§4.6-3 触屏行高）。
 * 卡内不放灰底块，避免与卡底形成第二层「面板」（§2.7 底材只分底 → 面板 → 浮起三级）。
 */
@Composable
private fun PriceNavRow(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = spacing.rowMinHeight)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = tokens.type.caption, color = colors.textMuted)
            Text(
                text = value.ifBlank { placeholder },
                style = tokens.type.body,
                color = if (value.isBlank()) colors.textMuted else colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(spacing.s2))
            trailing()
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(spacing.s5),
            tint = colors.textMuted,
        )
    }
}

/** 历史价格行：左边「地区 · 日期」，右边完整价格（区间），只读对照。 */
@Composable
private fun PriceHistoryRow(group: PriceGroup) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.hitMin)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = listOf(
                group.regionDisplay.ifBlank { stringResource(R.string.price_region_unset) },
                formatRecordTimestamp(group.recordedAt, RecordTimePrecision.DATE),
            ).joinToString(" · "),
            style = tokens.type.caption,
            color = colors.textMuted,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(spacing.s2))
        Text(
            text = group.priceDisplay,
            style = tokens.type.bodyS,
            color = colors.text,
            maxLines = 1,
        )
    }
}

/** 售卖单位快捷项：点一下填进输入框，也可以完全手写。 */
@Composable
private fun QuickUnitRow(presets: List<String>, selected: String, onSelect: (String) -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { unit ->
            val isSelected = unit == selected
            Box(
                modifier = Modifier
                    .heightIn(min = tokens.spacing.hitMin)
                    .clickable { onSelect(unit) },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(tokens.radii.label),
                    color = if (isSelected) tokens.colors.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                    border = BorderStroke(
                        width = if (isSelected) tokens.radii.stroke else tokens.radii.highlight,
                        color = if (isSelected) tokens.colors.primary else tokens.colors.border,
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = unit,
                            style = tokens.type.bodyS,
                            color = if (isSelected) tokens.colors.onPrimaryContainer else tokens.colors.textMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** 「今天」复位按钮：视觉 32 dp 高，外层补到 48 dp 热区。 */
@Composable
private fun TodayResetChip(onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    Box(
        modifier = Modifier
            .heightIn(min = tokens.spacing.hitMin)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(tokens.radii.label),
            color = tokens.colors.primaryContainer,
            border = BorderStroke(tokens.radii.highlight, tokens.colors.primary),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.spacing.s4),
                    tint = tokens.colors.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.price_date_today),
                    style = tokens.type.caption,
                    color = tokens.colors.onPrimaryContainer,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 字段级错误提示：等宽对齐在输入框下方，颜色用 danger（§2.3 错误只用一个颜色）。 */
@Composable
private fun ErrorHint(text: String) {
    val tokens = LocalAppTokens.current
    Text(
        text = text,
        style = tokens.type.caption,
        color = tokens.colors.danger,
    )
}

/** 今天 00:00；物价只记到日。 */
private fun todayStartMillis(): Long =
    java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
