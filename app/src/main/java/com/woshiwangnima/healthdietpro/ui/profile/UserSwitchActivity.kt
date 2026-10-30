package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDestructiveTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppTextIconButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppChipSelectedAlpha
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserMetadata
import com.woshiwangnima.healthdietpro.model.archive.resolveAvatarFile
import kotlin.math.abs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UserSwitchActivity : androidx.activity.ComponentActivity() {
    private var users by mutableStateOf(emptyList<UserMetadata>())
    private var currentUserId by mutableStateOf("")
    private var pendingDelete by mutableStateOf<UserMetadata?>(null)

    private val createUserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { reloadUsers() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reloadUsers()
        setContent {
            // 外层保住字号倍率，内层提供设计令牌（§8.14 的固定组合）。
            HealthDietProTheme {
                AppTheme(darkTheme = rememberAppDarkTheme()) {
                    BaseScreen(title = stringResource(R.string.profile_switch_user), onBack = ::finish) { padding ->
                        UserSwitchScreen(users, currentUserId, padding, ::createUser, ::selectUser, { pendingDelete = it })
                    }
                    pendingDelete?.let { user -> DeleteUserDialog(user) }
                }
            }
        }
    }

    private fun reloadUsers() {
        users = ProfilePrefs.getAllUserMetadata(this)
        currentUserId = ProfilePrefs.getCurrentUserId(this)
    }

    private fun createUser() {
        createUserLauncher.launch(Intent(this, ProfileEditActivity::class.java).putExtra("create_new", true))
    }

    private fun selectUser(user: UserMetadata) {
        ProfilePrefs.setCurrentUserId(this, user.id)
        setResult(RESULT_OK)
        finish()
    }

    @Composable
    private fun DeleteUserDialog(user: UserMetadata) {
        val fallbackName = stringResource(R.string.profile_name_unknown)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.profile_delete_user_title)) },
            text = { Text(stringResource(R.string.profile_delete_user_message, user.name.ifBlank { fallbackName })) },
            confirmButton = {
                AppDestructiveTextButton(
                    text = stringResource(R.string.profile_delete_user_confirm),
                    onClick = {
                    ProfilePrefs.deleteUser(this@UserSwitchActivity, user.id)
                    pendingDelete = null
                    reloadUsers()
                    setResult(RESULT_OK)
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
private fun UserSwitchScreen(
    users: List<UserMetadata>,
    currentUserId: String,
    contentPadding: PaddingValues,
    onCreateUser: () -> Unit,
    onSelectUser: (UserMetadata) -> Unit,
    onDeleteUser: (UserMetadata) -> Unit,
) {
    var sortMode by remember { mutableStateOf(UserSortMode.LAST_ACTIVE) }
    val displayUsers = users.sortedWith(
        compareByDescending<UserMetadata> { it.id == currentUserId }
            .then(sortMode.comparator),
    )
    val tokens = LocalAppTokens.current
    Column(Modifier.fillMaxSize().background(tokens.colors.bg).padding(contentPadding)) {
        if (displayUsers.size > 1) {
            EqualWidthSegmentedTabs(
                tabs = UserSortMode.entries.map { EqualWidthTab.text(it.label()) },
                selectedIndex = UserSortMode.entries.indexOf(sortMode),
                onSelected = { sortMode = UserSortMode.entries[it] },
                modifier = Modifier.padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s2,
                ),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = tokens.spacing.page,
                end = tokens.spacing.page,
                top = tokens.spacing.s3,
                bottom = tokens.spacing.s3,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        ) {
            if (displayUsers.isEmpty()) {
                item {
                    AppEmptyState(
                        title = stringResource(R.string.profile_no_users),
                        description = stringResource(R.string.profile_create_user),
                        iconRes = R.drawable.ic_switch_user,
                    )
                }
            } else {
                items(displayUsers, key = { it.id }) { user ->
                    UserSwitchRow(
                        user,
                        isCurrent = user.id == currentUserId,
                        onSelect = { if (user.id != currentUserId) onSelectUser(user) },
                        onDelete = { onDeleteUser(user) },
                    )
                }
            }
        }
        AppIconTextButton(
            stringResource(R.string.profile_create_user),
            R.drawable.ic_add,
            onCreateUser,
            Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s3),
        )
    }
}

private enum class UserSortMode(val comparator: Comparator<UserMetadata>) {
    LAST_ACTIVE(compareByDescending<UserMetadata> { it.lastActiveAtMillis }.thenBy { it.name }),
    CREATED(compareByDescending<UserMetadata> { it.createdAtMillis }.thenBy { it.name }),
    NAME(compareBy<UserMetadata> { it.name.lowercase(Locale.getDefault()) }),
}

@Composable
private fun UserSortMode.label(): String = when (this) {
    UserSortMode.LAST_ACTIVE -> stringResource(R.string.profile_user_sort_last_active)
    UserSortMode.CREATED -> stringResource(R.string.profile_user_sort_created)
    UserSortMode.NAME -> stringResource(R.string.profile_user_sort_name)
}

@Composable
private fun UserSwitchRow(user: UserMetadata, isCurrent: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val fallbackName = stringResource(R.string.profile_name_unknown)
    val name = user.name.ifBlank { fallbackName }
    val context = androidx.compose.ui.platform.LocalContext.current
    val avatar = user.avatarFileName.takeIf { it.isNotBlank() }
        ?.let { resolveAvatarFile(context, user.id, it) }
        ?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath) }
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCurrent) tokens.colors.primary.copy(alpha = AppChipSelectedAlpha) else tokens.colors.panelRaised)
            .border(
                width = if (isCurrent) tokens.radii.stroke else tokens.radii.highlight,
                color = if (isCurrent) tokens.colors.primary else tokens.colors.border,
                shape = shape,
            )
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(avatarColorFor(user.id)).clickable(onClick = onSelect), contentAlignment = Alignment.Center) {
            if (avatar != null) Image(avatar.asImageBitmap(), null, Modifier.size(48.dp).clip(CircleShape))
            else Text(name.firstOrNull()?.toString() ?: "?", style = tokens.type.titleS, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(tokens.spacing.s3))
        Column(Modifier.weight(1f).clickable(onClick = onSelect)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = tokens.type.body,
                    color = tokens.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) {
                    Text(
                        text = stringResource(R.string.profile_current_user_marker),
                        style = tokens.type.caption,
                        color = tokens.colors.primary,
                        modifier = Modifier.padding(start = tokens.spacing.s1),
                    )
                }
            }
            Text(
                text = user.gender.displayText(),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
            )
            UserMetadataDateRows(user)
        }
        // 删除是行内唯一的破坏性动作，热区保持 48 dp（原来被 size(40.dp) 压到 40 dp，低于 §2.2）。
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.profile_delete_user_confirm),
                tint = tokens.colors.danger,
            )
        }
    }
}

@Composable
private fun UserMetadataDateRows(user: UserMetadata) {
    val created = formatMetadataDate(user.createdAtMillis)
    val lastActive = formatMetadataDate(user.lastActiveAtMillis)
    val updated = formatMetadataDate(user.updatedAtMillis)
    val tokens = LocalAppTokens.current
    Text(
        text = stringResource(R.string.profile_user_dates_compact, created, lastActive, updated),
        style = tokens.type.caption,
        color = tokens.colors.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun formatMetadataDate(value: Long): String {
    if (value <= 0L) return stringResource(R.string.profile_user_date_unknown)
    val locale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
    return runCatching {
        formatter.format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
    }.getOrDefault(stringResource(R.string.profile_user_date_unknown))
}

@Composable
private fun Gender.displayText(): String = when (name) {
    "MALE" -> stringResource(R.string.profile_gender_male)
    "FEMALE" -> stringResource(R.string.profile_gender_female)
    else -> stringResource(R.string.profile_gender_unknown)
}

private fun avatarColorFor(id: String): Color {
    val colors = listOf(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFFC2185B), Color(0xFF0097A7), Color(0xFF689F38), Color(0xFF455A64))
    return colors[abs(id.hashCode()) % colors.size]
}
