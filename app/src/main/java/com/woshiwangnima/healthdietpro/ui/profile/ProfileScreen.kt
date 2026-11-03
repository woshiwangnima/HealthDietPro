package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPillIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.ui.profile.archive.ArchiveTransferController
import com.woshiwangnima.healthdietpro.ui.profile.archive.ArchiveTransferScreen

/**
 * 「我的」页。
 *
 * 本轮只做与规范对齐的细调（§2.5 圆角、§2.7-1 底材、§2.2 热区）：
 * - 顶部设置入口单独一行，热区 48 dp，不再和卡片挤在一张 `MaterialTheme.surface` 上；
 * - 卡片统一 `radii.container` + 1 dp 描边、去掉阴影与 tonal 叠加（旧版 8/16 dp 圆角 + tonal + shadow 三套并存）；
 * - 设置项行高 ≥ 56 dp，图标 20 dp，分隔线用 `border`；标题/副标题字号收敛到 `body` / `caption` 两级。
 *
 * 「导入/导出用户数据」不再就地弹 `AlertDialog`（§8.41）：那一版把格式、加密、预览、导出、导入
 * 五件事塞进一个只能放两行文字的浮层里，格式与压缩还是两个只有一个选项的假下拉。
 * 现在整块换成独立页面 [ArchiveTransferScreen]，由 [ArchiveTransferController] 的 `visible` 驱动，
 * 因此从别的标签页切走再切回来不会残留半开的浮层。
 */
@Composable
internal fun ProfileScreen(
    state: ProfileUserInfoUiState,
    onOpenAppSettings: () -> Unit,
    onOpenBmi: () -> Unit,
    onOpenUserSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenUserSwitch: () -> Unit,
    archiveController: ArchiveTransferController,
    modifier: Modifier = Modifier,
) {
    AppTheme(darkTheme = rememberAppDarkTheme()) {
        if (archiveController.uiState.visible) {
            ArchiveTransferScreen(archiveController)
        } else {
            ProfileContent(
                state = state,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBmi = onOpenBmi,
                onOpenUserSettings = onOpenUserSettings,
                onEditProfile = onEditProfile,
                onOpenUserSwitch = onOpenUserSwitch,
                onOpenArchive = archiveController::open,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUserInfoUiState,
    onOpenAppSettings: () -> Unit,
    onOpenBmi: () -> Unit,
    onOpenUserSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenUserSwitch: () -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.bg),
    ) {
        // 顶栏：只有设置入口。固定不随之滚动，滚到底也还能进设置。
        // 图标必须是「图标 + 文字」的描边胶囊：右上角一个纯齿轮图标，用户得先猜它是什么才能点（§5.1 次要按钮）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = tokens.spacing.page, end = tokens.spacing.s3, top = tokens.spacing.s1),
            horizontalArrangement = Arrangement.End,
        ) {
            AppPillIconTextButton(
                text = stringResource(R.string.profile_settings_button),
                iconRes = R.drawable.ic_settings,
                onClick = onOpenAppSettings,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = tokens.spacing.page, end = tokens.spacing.page, bottom = tokens.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            ProfileUserInfoCard(
                state = state,
                onEditProfile = onEditProfile,
                onSwitchUser = onOpenUserSwitch,
            )

            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.bmi_title),
                    iconRes = R.drawable.ic_bmi,
                    tooltip = stringResource(R.string.bmi_entry_desc),
                    showChevron = true,
                    onClick = onOpenBmi,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.profile_user_settings),
                    iconRes = R.drawable.ic_preferences,
                    tooltip = stringResource(R.string.profile_user_settings_desc),
                    showChevron = true,
                    onClick = onOpenUserSettings,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.profile_archive_data),
                    iconRes = R.drawable.ic_import_export,
                    tooltip = stringResource(R.string.profile_archive_data_desc),
                    showChevron = true,
                    onClick = onOpenArchive,
                )
            }
        }
    }
}
