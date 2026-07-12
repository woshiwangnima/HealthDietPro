package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDestructiveTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppTextIconButton
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreen(
    state: ProfileUserInfoUiState,
    loadUsers: () -> List<UserProfile>,
    loadCurrentUserId: () -> String,
    onOpenAppSettings: () -> Unit,
    onOpenBmi: () -> Unit,
    onOpenUserSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onCreateUser: () -> Unit,
    onSwitchUser: (UserProfile) -> Unit,
    onDeleteUser: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUserSheet by remember { mutableStateOf(false) }
    var users by remember { mutableStateOf(emptyList<UserProfile>()) }
    var currentUserId by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<UserProfile?>(null) }

    fun reloadUsers() {
        users = loadUsers()
        currentUserId = loadCurrentUserId()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpenAppSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_app_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ProfileUserInfoCard(
            state = state,
            onEditProfile = onEditProfile,
            onSwitchUser = {
                reloadUsers()
                showUserSheet = true
            },
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column {
                SettingRow(
                    title = stringResource(R.string.bmi_title),
                    subtitle = stringResource(R.string.bmi_entry_desc),
                    leadingIconRes = R.drawable.ic_chart,
                    onClick = onOpenBmi,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.profile_user_settings),
                    subtitle = stringResource(R.string.profile_user_settings_desc),
                    leadingIconRes = R.drawable.ic_preferences,
                    onClick = onOpenUserSettings,
                )
            }
        }
    }

    if (showUserSheet) {
        ModalBottomSheet(onDismissRequest = { showUserSheet = false }) {
            UserSwitchSheetContent(
                users = users,
                currentUserId = currentUserId,
                onCreateUser = {
                    showUserSheet = false
                    onCreateUser()
                },
                onSelectUser = { user ->
                    onSwitchUser(user)
                    reloadUsers()
                    showUserSheet = false
                },
                onDeleteUser = { user -> pendingDelete = user },
            )
        }
    }

    pendingDelete?.let { user ->
        val fallbackName = stringResource(R.string.profile_name_unknown)
        val deleteUserName = user.name.ifBlank { fallbackName }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.profile_delete_user_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.profile_delete_user_message,
                        deleteUserName,
                    ),
                )
            },
            confirmButton = {
                AppDestructiveTextButton(
                    text = stringResource(R.string.profile_delete_user_confirm),
                    onClick = {
                        onDeleteUser(user)
                        pendingDelete = null
                        reloadUsers()
                        showUserSheet = false
                    },
                )
            },
            dismissButton = {
                AppTextIconButton(
                    text = stringResource(R.string.profile_delete_user_cancel),
                    iconRes = R.drawable.ic_cancel,
                    onClick = { pendingDelete = null },
                )
            },
        )
    }
}

@Composable
private fun UserSwitchSheetContent(
    users: List<UserProfile>,
    currentUserId: String,
    onCreateUser: () -> Unit,
    onSelectUser: (UserProfile) -> Unit,
    onDeleteUser: (UserProfile) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_switch_user),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .width(30.dp)
                    .height(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.profile_switch_user),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(16.dp))
        AppIconTextButton(
            text = stringResource(R.string.profile_create_user),
            iconRes = R.drawable.ic_add,
            onClick = onCreateUser,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        if (users.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_no_users),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                items(
                    items = users,
                    key = { it.id },
                ) { user ->
                    UserSwitchRow(
                        user = user,
                        isCurrent = user.id == currentUserId,
                        onSelect = { onSelectUser(user) },
                        onDelete = { onDeleteUser(user) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSwitchRow(
    user: UserProfile,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val fallbackName = stringResource(R.string.profile_name_unknown)
    val name = user.name.ifBlank { fallbackName }
    val context = LocalContext.current
    val avatarColor = remember(user.id) { avatarColorFor(user.id) }
    val avatarFile = remember(context, user.avatarFileName) {
        user.avatarFileName.takeIf { it.isNotBlank() }?.let { File(context.filesDir, "avatars/$it") }
    }
    val bitmap = remember(avatarFile) {
        avatarFile?.takeIf { it.exists() }?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath) }
    }
    val background = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                )
            } else {
                Text(
                    text = name.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val genderIcon = user.gender.displayIcon()
                if (genderIcon.isNotEmpty()) {
                    Text(
                        text = genderIcon,
                        style = MaterialTheme.typography.bodySmall,
                        color = user.gender.displayColor(),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = user.gender.displayText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isCurrent) {
            Text(
                text = stringResource(R.string.profile_current_user_marker),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        AppDestructiveTextButton(
            text = stringResource(R.string.profile_delete_user_confirm),
            onClick = onDelete,
        )
    }
}

@Composable
private fun Gender.displayText(): String = when (name) {
    "MALE" -> stringResource(R.string.profile_gender_male)
    "FEMALE" -> stringResource(R.string.profile_gender_female)
    else -> stringResource(R.string.profile_gender_unknown)
}

private fun Gender.displayIcon(): String = when (name) {
    "MALE" -> "\u2642"
    "FEMALE" -> "\u2640"
    else -> ""
}

@Composable
private fun Gender.displayColor(): Color = when (name) {
    "MALE" -> Color(0xFF2196F3)
    "FEMALE" -> Color(0xFFE91E63)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun avatarColorFor(id: String): Color {
    val colors = listOf(
        Color(0xFF1976D2),
        Color(0xFF388E3C),
        Color(0xFFF57C00),
        Color(0xFF7B1FA2),
        Color(0xFFC2185B),
        Color(0xFF0097A7),
        Color(0xFF689F38),
        Color(0xFF455A64),
    )
    return colors[abs(id.hashCode()) % colors.size]
}
