package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText

@Composable
internal fun ProfileUserInfoCard(
    state: ProfileUserInfoUiState,
    onEditProfile: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ProfileAvatar(state = state)
                IconButton(
                    onClick = onSwitchUser,
                    modifier = Modifier
                        .width(44.dp)
                        .height(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_switch_user),
                        contentDescription = stringResource(R.string.profile_switch_user),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(30.dp)
                            .height(24.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infoSegments = state.infoLine
                        .split("|")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (state.genderIcon.isNotBlank()) {
                        Text(
                            text = state.genderIcon,
                            style = MaterialTheme.typography.bodyMedium,
                            color = state.genderTone.color(),
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    infoSegments.take(2).forEachIndexed { index, segment ->
                        if (index > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_separator),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        TextOverflowText(
                            text = segment,
                            modifier = Modifier.weight(
                                if (index == 0) 1f else 1.15f,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                ProfileInfoLabelRow(R.drawable.ic_birthday, state.birthdayText)
                ProfileInfoLabelRow(R.drawable.ic_region, state.regionText)
                if (state.hasDiseaseText) ProfileInfoLabelRow(R.drawable.ic_medical_history, state.diseaseText)
            }

            Spacer(Modifier.width(10.dp))

            IconButton(
                onClick = onEditProfile,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.profile_user_info),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(state: ProfileUserInfoUiState) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(state.avatarColor),
        contentAlignment = Alignment.Center,
    ) {
        if (state.avatarBitmap != null) {
            Image(
                bitmap = state.avatarBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )
        } else {
            Text(
                text = state.avatarInitial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ProfileInfoLabelRow(iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        TextOverflowText(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ProfileGenderTone.color(): Color = when (this) {
    ProfileGenderTone.Male -> Color(0xFF2196F3)
    ProfileGenderTone.Female -> Color(0xFFE91E63)
    ProfileGenderTone.Unknown -> Color.Unspecified
}
