package com.woshiwangnima.healthdietpro.ui.price

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSheetHeight
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import com.woshiwangnima.healthdietpro.model.region.RegionRepository
import com.woshiwangnima.healthdietpro.model.region.RegionSnapshot

/** 地区选择的当前层级。 */
private enum class RegionStep { PROVINCE, CITY, DISTRICT }

/**
 * 地区选择弹层：省 → 市 → 区。
 *
 * 顶部优先给「使用资料中的地区」——那是用户自己设过的常驻地址，比现场定位更贴日常记价的场景；
 * 「使用当前定位」由外层触发（定位需要 Activity 的权限回调），这里只提供入口；
 * 手动选择支持只选到省或市（小城市没有区级数据时不会卡住）。
 */
@Composable
internal fun PriceRegionPickerDialog(
    initial: RegionSnapshot,
    profileRegion: RegionSnapshot,
    locating: Boolean,
    locationError: String?,
    onDismiss: () -> Unit,
    onLocate: () -> Unit,
    onPicked: (RegionSnapshot) -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalAppTokens.current
    val provinceRepo = remember { ProvinceRepository.fromContext(context) }
    val regionRepo = remember { RegionRepository.fromContext(context) }

    var step by remember { mutableStateOf(RegionStep.PROVINCE) }
    var province by remember {
        mutableStateOf(initial.provinceCode.takeIf(String::isNotBlank)?.let(provinceRepo::findByCode))
    }
    var cityCode by remember { mutableStateOf(initial.cityCode) }
    var cityName by remember { mutableStateOf(initial.cityName) }

    val cities = remember(province?.code) { province?.let { regionRepo.citiesOf(it.code) }.orEmpty() }
    val districts = remember(cityCode) { if (cityCode.isBlank()) emptyList() else regionRepo.districtsOf(cityCode) }

    // 三级下钻是「长内容 + 列表」，按 §5.2 改走底部抽屉：能拉到满屏看完整省份列表，
    // 也不会像居中对话框那样被系统栏顶掉。收起靠下滑或点遮罩。
    AppBottomSheet(
        title = stringResource(R.string.price_region_pick_title),
        subtitle = when (step) {
            RegionStep.PROVINCE -> stringResource(R.string.price_region_step_province)
            RegionStep.CITY -> province?.name.orEmpty()
            RegionStep.DISTRICT -> listOf(province?.name, cityName).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
        },
        onDismiss = onDismiss,
        initialSnap = AppSheetHeight.Full,
        contentScrollable = false,
    ) {
        Column(Modifier.fillMaxWidth().weight(1f)) {
                if (step == RegionStep.PROVINCE) {
                    if (!profileRegion.isEmpty()) {
                        // 默认地址就是资料里的地址，这里给它一个随时回得去的入口，
                        // 免得手动改到外地后只能重新逐级点省市区。
                        // 「使用资料中的地区 / 使用当前定位」是两个并列的二级动作，
                        // 统一成「图标 + 文字 + 描边」的同一款按钮（§5.1）：原先是两行纯文字，
                        // 既读不出能按，也跟页面上的其他动作按钮不同款。
                        Column(Modifier.fillMaxWidth()) {
                            AppOutlinedIconTextButton(
                                text = stringResource(R.string.price_region_use_profile),
                                iconRes = R.drawable.ic_region,
                                onClick = { onPicked(profileRegion) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = profileRegion.display(),
                                style = tokens.type.bodyS,
                                // 地区名是这条入口的实际取值（点了就是它），不是补充注脚：给正文色。
                                color = tokens.colors.text,
                                // 小注跟在通栏胶囊下面：贴着胶囊左边缘收一档内缩，不再沿用旧文字按钮
                                // 的 40 dp 图标位（按钮改成描边胶囊后那个缩进就成了悬空）。
                                modifier = Modifier.padding(
                                    start = tokens.spacing.s3,
                                    end = tokens.spacing.s2,
                                    bottom = tokens.spacing.s2,
                                ),
                            )
                        }
                    }
                    AppOutlinedIconTextButton(
                        text = stringResource(
                            if (locating) R.string.price_region_locating else R.string.price_region_use_location,
                        ),
                        iconRes = R.drawable.ic_region,
                        onClick = onLocate,
                        enabled = !locating,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    locationError?.let {
                        Text(
                            text = it,
                            style = LocalAppTokens.current.type.bodyS,
                            color = LocalAppTokens.current.colors.danger,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                }
                val items: List<Pair<String, String>> = when (step) {
                    RegionStep.PROVINCE -> provinceRepo.all().sortedBy { it.code }.map { it.code to it.name }
                    RegionStep.CITY -> cities.map { it.code to it.name }
                    RegionStep.DISTRICT -> districts.map { it.code to it.name }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(items, key = { it.first }) { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (step) {
                                        RegionStep.PROVINCE -> {
                                            province = provinceRepo.findByCode(code)
                                            cityCode = ""
                                            cityName = ""
                                            step = RegionStep.CITY
                                        }
                                        RegionStep.CITY -> {
                                            cityCode = code
                                            cityName = name
                                            if (regionRepo.districtsOf(code).isEmpty()) {
                                                onPicked(snapshotOf(province, cityCode, cityName, "", ""))
                                            } else {
                                                step = RegionStep.DISTRICT
                                            }
                                        }
                                        RegionStep.DISTRICT -> {
                                            onPicked(snapshotOf(province, cityCode, cityName, code, name))
                                        }
                                    }
                                }
                                // 触屏行高 ≥ 56 dp（§4.6-3）。
                                .heightIn(min = 56.dp)
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // 颜色必须显式给 `text`：这个 `Text` 不在 Surface / 按钮里，
                            // 不写 color 就会落到 `LocalContentColor` 的默认值（纯黑），
                            // 深色面板（#0D0D0D）上肉眼几乎看不见。
                            Text(
                                text = name,
                                style = tokens.type.body,
                                color = tokens.colors.text,
                            )
                        }
                    }
                    if (step != RegionStep.PROVINCE) {
                        item(key = "__back_or_skip") {
                            TextButton(
                                onClick = {
                                    when (step) {
                                        RegionStep.CITY -> {
                                            // 只到省：省级也有价格参考意义。
                                            onPicked(snapshotOf(province, "", "", "", ""))
                                        }
                                        RegionStep.DISTRICT -> {
                                            onPicked(snapshotOf(province, cityCode, cityName, "", ""))
                                        }
                                        RegionStep.PROVINCE -> Unit
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(
                                    stringResource(
                                        if (step == RegionStep.CITY) R.string.price_region_only_province else R.string.price_region_only_city,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
    }
}

private fun snapshotOf(
    province: com.woshiwangnima.healthdietpro.model.region.Province?,
    cityCode: String,
    cityName: String,
    districtCode: String,
    districtName: String,
): RegionSnapshot = RegionSnapshot(
    provinceCode = province?.code.orEmpty(),
    provinceName = province?.name.orEmpty(),
    cityCode = cityCode,
    cityName = cityName,
    districtCode = districtCode,
    districtName = districtName,
)
