package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.settings.AppSettingsComposeActivity
import com.woshiwangnima.healthdietpro.ui.settings.UserSettingsActivity
import com.woshiwangnima.healthdietpro.util.UnitConverter

class ProfileFragment : Fragment() {

    private val userInfoViewModel: ProfileUserInfoViewModel by viewModels()
    private lateinit var editLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(requireContext())
        editLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { refreshProfile() }
        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { refreshProfile() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                HealthDietProTheme {
                    val state by userInfoViewModel.uiState.collectAsState()
                    ProfileScreen(
                        state = state,
                        loadUsers = { ProfilePrefs.getAllUsers(requireContext()) },
                        loadCurrentUserId = { ProfilePrefs.getCurrentUserId(requireContext()) },
                        onOpenAppSettings = {
                            settingsLauncher.launch(
                                Intent(requireContext(), AppSettingsComposeActivity::class.java),
                            )
                        },
                        onOpenBmi = {
                            startActivity(Intent(requireContext(), BmiDetailActivity::class.java))
                        },
                        onOpenUserSettings = {
                            startActivity(Intent(requireContext(), UserSettingsActivity::class.java))
                        },
                        onEditProfile = {
                            editLauncher.launch(Intent(requireContext(), ProfileEditActivity::class.java))
                        },
                        onCreateUser = {
                            editLauncher.launch(
                                Intent(requireContext(), ProfileEditActivity::class.java)
                                    .putExtra("create_new", true),
                            )
                        },
                        onSwitchUser = { user ->
                            ProfilePrefs.setCurrentUserId(requireContext(), user.id)
                            refreshProfile()
                        },
                        onDeleteUser = { user ->
                            ProfilePrefs.deleteUser(requireContext(), user.id)
                            refreshProfile()
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfile()
    }

    private fun refreshProfile() {
        userInfoViewModel.refresh()
    }
}
