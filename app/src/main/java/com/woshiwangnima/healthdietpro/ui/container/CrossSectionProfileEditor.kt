package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.NumericInputField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputKind
import com.woshiwangnima.healthdietpro.common.ui.NumericInputSpec
import com.woshiwangnima.healthdietpro.model.container.CrossSection
import com.woshiwangnima.healthdietpro.model.container.CrossSectionProfile
import com.woshiwangnima.healthdietpro.model.container.CrossSectionValidation

/**
 * Full cross-section profile editor: length unit + container total height + side view +
 * ordered key-point cards + add/delete + validation messages + live total volume.
 *
 * Key-point semantics: users only enter cross-sections at transition points. Volume below the
 * first point and above the last point extends in parallel (constant area). A single point is
 * a uniform prism (area × height).
 *
 * All linear inputs are shown in [lengthUnitId] and stored in the base unit cm via
 * `UnitConverter` (see the BodyRecord invariant).
 */
@Composable
internal fun CrossSectionProfileEditor(
    initial: CrossSectionProfile,
    onProfileChanged: (CrossSectionProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var points by remember(initial) { mutableStateOf(initial.points) }
    var lengthUnitId by remember(initial) { mutableStateOf(initial.lengthUnitId) }
    var totalHeightText by remember(initial, lengthUnitId) {
        mutableStateOf("%.1f".format(fromBaseCm(initial.totalHeightCm, lengthUnitId)))
    }
    var slicePreviewHeightCm by remember(initial) { mutableStateOf(initial.totalHeightCm) }
    val unit = lengthUnitSymbol(lengthUnitId)
    val totalHeightCm = totalHeightText.toDoubleOrNull()?.let { toBaseCm(it, lengthUnitId) }

    val currentProfile = remember(points, totalHeightCm, lengthUnitId) {
        totalHeightCm?.let { h -> runCatching { CrossSectionProfile(points, h, lengthUnitId) }.getOrNull() }
    }
    val validation = remember(currentProfile) { currentProfile?.validate().orEmpty() }
    val volume = remember(currentProfile) { currentProfile?.totalVolumeMl() }
    val sliceArea = remember(currentProfile, slicePreviewHeightCm) {
        currentProfile?.areaAt(slicePreviewHeightCm)
    }

    fun changeUnit(newUnit: String) {
        if (newUnit == lengthUnitId) return
        val baseTotal = currentProfile?.totalHeightCm ?: totalHeightCm ?: initial.totalHeightCm
        lengthUnitId = newUnit
        totalHeightText = "%.1f".format(fromBaseCm(baseTotal, newUnit))
        runCatching { CrossSectionProfile(points, baseTotal, newUnit) }.onSuccess(onProfileChanged)
    }

    fun commit(next: List<CrossSection>) {
        val sorted = next.sortedBy { it.heightCm }
        if (sorted.map { it.heightCm }.distinct().size != sorted.size) return
        points = sorted
        val h = totalHeightCm
        if (h != null) {
            runCatching { CrossSectionProfile(sorted, h, lengthUnitId) }.onSuccess(onProfileChanged)
        }
    }

    fun addPoint() {
        val sorted = points.sortedBy { it.heightCm }
        val highest = sorted.last()
        val newHeight = (highest.heightCm + (totalHeightCm ?: highest.heightCm)) / 2.0
        commit(points + CrossSection(newHeight, highest.shape))
        slicePreviewHeightCm = newHeight
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("容器截面定义", style = MaterialTheme.typography.titleMedium)
        Text(
            "输入容器总高与关键转折处的截面即可：只输一个截面按「面积×高度」计容积；相邻同形截面（圆/方/矩形同比例）按直壁圆台积分，异形或过渡截面线性插值，首尾两端平行延伸。侧视图宽度 ∝ √面积。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumericInputField(
                label = "容器总高 ($unit)",
                value = totalHeightText,
                onValueChange = { totalHeightText = it },
                spec = NumericInputSpec(
                    kind = NumericInputKind.Decimal,
                    range = RangeBand(min = 0.0, minInclusive = false, value = Unit),
                    decimalPlaces = 1,
                ),
                modifier = Modifier.weight(1f),
            )
            AppDropdownField(
                label = "单位",
                value = unit,
                options = PRACTICAL_LENGTH_UNITS.map { id ->
                    AppDropdownOption(id, lengthUnitSymbol(id))
                },
                onSelect = { changeUnit(it.id) },
                modifier = Modifier.width(104.dp),
            )
        }

        currentProfile?.let {
            ContainerSideView(
                profile = it,
                currentEditPercent = (slicePreviewHeightCm / it.totalHeightCm).toFloat().coerceIn(0f, 1f),
                onHeightChanged = { percent -> slicePreviewHeightCm = percent.toDouble() * it.totalHeightCm },
            )
        }
        if (sliceArea != null) {
            Text(
                "红线处截面面积: %.2f cm²".format(sliceArea),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        points.forEach { point ->
            CrossSectionPointEditor(
                point = point,
                lengthUnitId = lengthUnitId,
                totalHeightCm = totalHeightCm ?: point.heightCm,
                canDelete = points.size > 1,
                onUnitChange = { changeUnit(it) },
                onPointChanged = { updated ->
                    commit(points.map { if (it === point) updated else it })
                    slicePreviewHeightCm = updated.heightCm
                },
                onDelete = {
                    commit(points.filterNot { it === point })
                },
            )
        }

        AppIconTextButton("＋ 添加截面点", R.drawable.ic_add, ::addPoint, Modifier.fillMaxWidth())

        validation.forEach { item ->
            when (item) {
                is CrossSectionValidation.AreaDecreasing -> {
                    Text(
                        "⚠ 容器上窄下宽：%.1f %s 处 %.1f cm² → %.1f %s 处 %.1f cm²，请确认是否符合形状（如酒壶）。"
                            .format(
                                fromBaseCm(item.lowerHeight, lengthUnitId), unit, item.lowerArea,
                                fromBaseCm(item.upperHeight, lengthUnitId), unit, item.upperArea,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                is CrossSectionValidation.ZeroAreaAtMiddle -> {
                    Text(
                        "❌ %.1f %s 高度面积为 0，仅允许用于底部（尖底）或顶部（密封收口）。"
                            .format(fromBaseCm(item.heightCm, lengthUnitId), unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "总容积: ${volume?.let { "%.1f ml".format(it) } ?: "-"}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "总高: ${totalHeightCm?.let { "%.1f $unit".format(fromBaseCm(it, lengthUnitId)) } ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
