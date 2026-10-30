package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.archive.appVersion

/**
 * 关于：应用标识 + 一个「检查更新」主操作 + 一张资料与声明卡。
 *
 * 旧版把版本信息混在 `bodyLarge` 段落里、三行说明平铺且标题 24 sp，和上面的 Logo 抢层级。
 *
 * 「来源与参考」不再是另一个页面：展开箭头一转，DRIs 与 WHO 两条来源直接缩进列在这一行下面
 * （文件夹式的手风琴），用户不用为看两个链接进出两层页面；两个来源各自带外链图标，点了直接开浏览器。
 */
@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokens = LocalAppTokens.current
    var showDisclaimer by remember { mutableStateOf(false) }
    var sourcesExpanded by remember { mutableStateOf(false) }
    val drisUrl = stringResource(R.string.settings_dris_url)
    val icd11Url = stringResource(R.string.settings_icd11_url)

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.settings_disclaimer)) },
            text = { Text(stringResource(R.string.settings_disclaimer_content)) },
            confirmButton = {
                Button(onClick = { showDisclaimer = false }) {
                    Text(stringResource(R.string.settings_confirm))
                }
            },
        )
    }

    BaseScreen(title = stringResource(R.string.settings_about), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s3),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_badge),
                    contentDescription = stringResource(R.string.settings_about_app_icon),
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(tokens.spacing.s3))
                Text(
                    text = stringResource(R.string.settings_about_name),
                    style = tokens.type.titleS,
                    color = tokens.colors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(tokens.spacing.s1))
                Text(
                    text = stringResource(R.string.settings_about_version, appVersion(context)),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
            }

            AppOutlinedIconTextButton(
                text = stringResource(R.string.settings_check_update),
                iconRes = R.drawable.ic_refresh,
                onClick = { openUrl(context, context.getString(R.string.settings_update_url)) },
                modifier = Modifier.fillMaxWidth(),
            )

            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.settings_source_code),
                    iconRes = R.drawable.ic_description,
                    trailingIconRes = R.drawable.ic_open_in_new,
                    tooltip = stringResource(R.string.settings_source_url),
                    onClick = { openUrl(context, context.getString(R.string.settings_source_url)) },
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.settings_disclaimer),
                    iconRes = R.drawable.ic_help,
                    tooltip = stringResource(R.string.settings_disclaimer_desc),
                    onClick = { showDisclaimer = true },
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.settings_sources_and_references),
                    iconRes = R.drawable.ic_info,
                    expanded = sourcesExpanded,
                    tooltip = stringResource(R.string.settings_sources_and_references_desc),
                    onClick = { sourcesExpanded = !sourcesExpanded },
                )
                // 展开后紧贴在父行下方，缩进到与父行文字同一条起始线，读起来像「文件夹里的两项」。
                AnimatedVisibility(
                    visible = sourcesExpanded,
                    enter = expandVertically(animationSpec = tween(tokens.motion.baseMs)) + fadeIn(
                        animationSpec = tween(tokens.motion.baseMs),
                    ),
                    exit = shrinkVertically(animationSpec = tween(tokens.motion.baseMs)) + fadeOut(
                        animationSpec = tween(tokens.motion.baseMs),
                    ),
                ) {
                    Column {
                        AppListRow(
                            title = stringResource(R.string.settings_dris),
                            modifier = Modifier.padding(start = tokens.spacing.s3 + 20.dp),
                            trailingIconRes = R.drawable.ic_open_in_new,
                            tooltip = drisUrl,
                            onClick = { openUrl(context, drisUrl) },
                        )
                        AppListRowDivider()
                        AppListRow(
                            title = stringResource(R.string.settings_icd11),
                            modifier = Modifier.padding(start = tokens.spacing.s3 + 20.dp),
                            trailingIconRes = R.drawable.ic_open_in_new,
                            tooltip = icd11Url,
                            onClick = { openUrl(context, icd11Url) },
                        )
                    }
                }
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
