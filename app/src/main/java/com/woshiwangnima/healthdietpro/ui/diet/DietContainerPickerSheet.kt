package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSheetHeight
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.ui.container.ContainerCardRow

/**
 * 挑一个已记录的容器做去皮。
 *
 * 选择器走底部抽屉而不是居中对话框（通用规范 §5.2）：选项是带插图的卡片，高度不可预知，
 * 抽屉能给到「半屏起步、可拉到满屏」的余地，也不会有对话框在小屏上被顶出屏幕的问题。
 *
 * **点选即生效并关闭，不放底部确定按钮**（通用规范 §5.5 单选类操作：「选中即生效，不需要确定」）。
 * 之前抽屉内容底部压着一条「确定」操作条：抽屉处在半屏档时，操作条落在可见区域之外，
 * 既占掉列表高度又把最后一行挤到导航条附近，是两个问题叠在一起。
 */
@Composable
internal fun DietContainerPickerSheet(
    containers: List<ContainerRecord>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    AppBottomSheet(
        title = stringResource(R.string.diet_select_container),
        onDismiss = onDismiss,
        initialSnap = AppSheetHeight.Half,
        contentScrollable = false,
    ) {
        if (containers.isEmpty()) {
            AppEmptyState(
                title = stringResource(R.string.record_no_data),
                description = stringResource(R.string.diet_containers_empty),
                iconRes = R.drawable.ic_container,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(vertical = tokens.spacing.s1),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            ) {
                items(containers, key = ContainerRecord::id) { container ->
                    val selected = container.id == selectedId
                    Surface(
                        onClick = {
                            onSelect(container.id)
                            onDismiss()
                        },
                        color = if (selected) tokens.colors.primaryContainer else tokens.colors.panel,
                        border = BorderStroke(
                            if (selected) tokens.radii.stroke else tokens.radii.highlight,
                            if (selected) tokens.colors.primary else tokens.colors.border,
                        ),
                        shape = RoundedCornerShape(tokens.radii.label),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ContainerCardRow(container, showDelete = false)
                    }
                }
            }
        }
    }
}
