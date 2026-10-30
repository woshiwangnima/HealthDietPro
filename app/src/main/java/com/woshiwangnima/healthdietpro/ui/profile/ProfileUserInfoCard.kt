package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens

/**
 * 个人卡片（§2.7-1 底材：`panelRaised` + 1 dp 描边，不叠阴影）。
 *
 * 旧版是 16 dp 圆角 + 2 dp 阴影 + 2 dp tonal 的 Material 默认卡，与本项目其它已重刷的卡片不是一套语言；
 * 头像下方那颗「切换用户」还是填充主色按钮，比卡片主信息更抢眼，改成文本按钮后主次才回到姓名上。
 */
@Composable
internal fun ProfileUserInfoCard(
    state: ProfileUserInfoUiState,
    onEditProfile: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val shape = RoundedCornerShape(tokens.radii.container)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panelRaised)
            .border(tokens.radii.highlight, colors.border, shape)
            .padding(tokens.spacing.s3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(state = state)
                Spacer(Modifier.height(tokens.spacing.s1))
                // 「图标 + 切换用户」包一层描边胶囊：原来是裸文本按钮，看着像一段可点文字而不是按钮。
                // 图标与文字同为 primary，并各自 `CenterVertically`；图标本身的图形重心已在
                // ic_switch_user.xml 里对到画布中线，不再靠视觉微调硬凑。
                Row(
                    modifier = Modifier
                        .heightIn(min = tokens.spacing.s6)
                        .clip(RoundedCornerShape(tokens.radii.pill))
                        .border(
                            1.dp,
                            colors.primary.copy(alpha = SwitchUserBorderAlpha),
                            RoundedCornerShape(tokens.radii.pill),
                        )
                        .clickable(onClick = onSwitchUser)
                        .padding(horizontal = tokens.spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_switch_user),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.primary,
                    )
                    Spacer(Modifier.width(tokens.spacing.s1))
                    Text(
                        text = stringResource(R.string.profile_switch_user),
                        style = tokens.type.caption,
                        color = colors.primary,
                    )
                }
            }

            Spacer(Modifier.width(tokens.spacing.s3))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.displayName,
                    style = tokens.type.titleS,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(tokens.spacing.s1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.genderIcon.isNotBlank()) {
                        Text(
                            text = state.genderIcon,
                            style = tokens.type.bodyS,
                            color = state.genderTone.color(),
                        )
                        Spacer(Modifier.width(tokens.spacing.s1))
                    }
                    TextOverflowText(
                        text = state.infoLine,
                        modifier = Modifier.weight(1f),
                        style = tokens.type.bodyS,
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.height(tokens.spacing.s2))
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
                    ProfileInfoLabelRow(R.drawable.ic_cake, state.birthdayText)
                    ProfileInfoLabelRow(R.drawable.ic_region, state.regionText)
                    if (state.diseases.isNotEmpty()) {
                        ProfileDiseaseLabelRow(state.diseases)
                    }
                }
            }

            IconButton(
                onClick = onEditProfile,
                modifier = Modifier.size(tokens.spacing.hitMin),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.profile_user_info),
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(state: ProfileUserInfoUiState) {
    val tokens = LocalAppTokens.current
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(state.avatarColor),
        contentAlignment = Alignment.Center,
    ) {
        if (state.avatarBitmap != null) {
            Image(
                bitmap = state.avatarBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape),
            )
        } else {
            Text(
                text = state.avatarInitial,
                style = tokens.type.titleS,
                fontWeight = FontWeight.SemiBold,
                color = tokens.colors.onPrimary,
            )
        }
    }
}

@Composable
private fun ProfileInfoLabelRow(iconRes: Int, text: String) {
    val tokens = LocalAppTokens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tokens.colors.textMuted,
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        TextOverflowText(
            text = text,
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun ProfileDiseaseLabelRow(diseases: List<ProfileDiseaseDisplay>) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_medical_history),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tokens.colors.textMuted,
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        val text = buildAnnotatedString {
            diseases.forEachIndexed { index, disease ->
                if (index > 0) append("、")
                withStyle(androidx.compose.ui.text.SpanStyle(color = disease.status.profileColor())) {
                    append(disease.name)
                }
            }
        }
        TextOverflowText(
            text = text,
            modifier = Modifier.weight(1f),
            style = tokens.type.caption,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProfileGenderTone.color(): Color {
    val colors = LocalAppTokens.current.colors
    return when (this) {
        ProfileGenderTone.Male -> colors.primary
        ProfileGenderTone.Female -> colors.info
        ProfileGenderTone.Unknown -> colors.textMuted
    }
}

/** 头像直径：比信息行字号大一档，保证姓名仍是卡片里最重的元素。 */
private val AvatarSize = 56.dp

/** 描边胶囊的边框透明度：主色描边压到 45%，只做「这是个按钮」的提示，不与姓名抢注意力。 */
private const val SwitchUserBorderAlpha = 0.45f
