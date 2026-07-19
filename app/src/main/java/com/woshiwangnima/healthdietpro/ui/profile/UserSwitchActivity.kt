package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDestructiveTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppTextIconButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import java.io.File
import kotlin.math.abs

class UserSwitchActivity : androidx.activity.ComponentActivity() {
    private var users by mutableStateOf(emptyList<UserProfile>())
    private var currentUserId by mutableStateOf("")
    private var pendingDelete by mutableStateOf<UserProfile?>(null)

    private val createUserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { reloadUsers() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reloadUsers()
        setContent {
            HealthDietProTheme {
                BaseScreen(title = stringResource(R.string.profile_switch_user), onBack = ::finish) { padding ->
                    UserSwitchScreen(users, currentUserId, padding, ::createUser, ::selectUser, { pendingDelete = it })
                }
                pendingDelete?.let { user -> DeleteUserDialog(user) }
            }
        }
    }

    private fun reloadUsers() {
        users = ProfilePrefs.getAllUsers(this)
        currentUserId = ProfilePrefs.getCurrentUserId(this)
    }

    private fun createUser() {
        createUserLauncher.launch(Intent(this, ProfileEditActivity::class.java).putExtra("create_new", true))
    }

    private fun selectUser(user: UserProfile) {
        ProfilePrefs.setCurrentUserId(this, user.id)
        setResult(RESULT_OK)
        finish()
    }

    @Composable
    private fun DeleteUserDialog(user: UserProfile) {
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
    users: List<UserProfile>,
    currentUserId: String,
    contentPadding: PaddingValues,
    onCreateUser: () -> Unit,
    onSelectUser: (UserProfile) -> Unit,
    onDeleteUser: (UserProfile) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (users.isEmpty()) {
                item { Text(stringResource(R.string.profile_no_users), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(users, key = { it.id }) { user ->
                    UserSwitchRow(user, user.id == currentUserId, onSelect = { onSelectUser(user) }, onDelete = { onDeleteUser(user) })
                }
            }
        }
        AppIconTextButton(stringResource(R.string.profile_create_user), R.drawable.ic_add, onCreateUser, Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
private fun UserSwitchRow(user: UserProfile, isCurrent: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val fallbackName = stringResource(R.string.profile_name_unknown)
    val name = user.name.ifBlank { fallbackName }
    val avatar = user.avatarFileName.takeIf { it.isNotBlank() }?.let { File(androidx.compose.ui.platform.LocalContext.current.filesDir, "avatars/$it") }?.takeIf { it.exists() }?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath) }
    val background = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(background).clickable(onClick = onSelect).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(avatarColorFor(user.id)), contentAlignment = Alignment.Center) {
            if (avatar != null) Image(avatar.asImageBitmap(), null, Modifier.size(48.dp).clip(CircleShape))
            else Text(name.firstOrNull()?.toString() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(user.gender.displayText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isCurrent) Text(stringResource(R.string.profile_current_user_marker), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
        AppDestructiveTextButton(stringResource(R.string.profile_delete_user_confirm), onDelete)
    }
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
