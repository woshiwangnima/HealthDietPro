package com.woshiwangnima.healthdietpro.ui.price

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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

    AlertDialog(
        modifier = Modifier.width((LocalConfiguration.current.screenWidthDp - 16).dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.price_region_pick_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = when (step) {
                            RegionStep.PROVINCE -> stringResource(R.string.price_region_step_province)
                            RegionStep.CITY -> province?.name.orEmpty()
                            RegionStep.DISTRICT -> listOf(province?.name, cityName).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 阻断型浮层右上角必须有可见关闭入口（§5.5.1）。
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.compose_confirm_dialog_cancel))
                }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (step == RegionStep.PROVINCE) {
                    if (!profileRegion.isEmpty()) {
                        // 默认地址就是资料里的地址，这里给它一个随时回得去的入口，
                        // 免得手动改到外地后只能重新逐级点省市区。
                        Column(Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { onPicked(profileRegion) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_region),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.price_region_use_profile))
                            }
                            Text(
                                text = profileRegion.display(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 40.dp, end = 8.dp, bottom = 4.dp),
                            )
                        }
                    }
                    TextButton(
                        onClick = onLocate,
                        enabled = !locating,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_region),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (locating) R.string.price_region_locating else R.string.price_region_use_location,
                            ),
                        )
                    }
                    locationError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
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
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
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
                            Text(name, style = MaterialTheme.typography.bodyLarge)
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
        },
        // 选到区（或主动停在省 / 市）即完成，没有第二步确认，底部不再放「取消」。
        confirmButton = {},
    )
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
