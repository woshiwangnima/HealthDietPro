package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.databinding.FragmentProfileBinding
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import com.woshiwangnima.healthdietpro.ui.settings.AppSettingsComposeActivity
import com.woshiwangnima.healthdietpro.util.UnitConverter
import kotlin.math.abs

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userInfoViewModel: ProfileUserInfoViewModel by viewModels()
    private lateinit var editLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    private val avatarColors = intArrayOf(
        0xFF1976D2.toInt(), 0xFF388E3C.toInt(), 0xFFF57C00.toInt(),
        0xFF7B1FA2.toInt(), 0xFFC2185B.toInt(), 0xFF0097A7.toInt(),
        0xFF689F38.toInt(), 0xFF455A64.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(requireContext())
        editLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { refreshProfile() }
        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { refreshProfile() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingsBtn.setOnClickListener {
            val intent = Intent(requireContext(), AppSettingsComposeActivity::class.java)
            settingsLauncher.launch(intent)
        }

        binding.userSettingsBtn.setOnClickListener {
            startActivity(Intent(requireContext(),
                com.woshiwangnima.healthdietpro.ui.settings.UserSettingsActivity::class.java))
        }

        binding.profileUserInfoCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.profileUserInfoCompose.setContent {
            HealthDietProTheme {
                val state by userInfoViewModel.uiState.collectAsState()
                ProfileUserInfoCard(
                    state = state,
                    onEditProfile = {
                        val intent = Intent(requireContext(), ProfileEditActivity::class.java)
                        editLauncher.launch(intent)
                    },
                    onSwitchUser = { showUserSwitchSheet() },
                )
            }
        }

        refreshProfile()
    }

    private fun refreshProfile() {
        userInfoViewModel.refresh()
    }

    private fun showUserSwitchSheet() {
        val ctx = requireContext()
        val users = ProfilePrefs.getAllUsers(ctx)
        val currentId = ProfilePrefs.getCurrentUserId(ctx)

        val sheet = BottomSheetDialog(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 16)
        }

        root.addView(TextView(ctx).apply {
            text = "\u5207\u6362\u7528\u6237"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            setPadding(24, 0, 24, 16)
        })

        root.addView(MaterialButton(ctx).apply {
            text = "+ \u65B0\u589E\u7528\u6237"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(24, 0, 24, 12) }
            setOnClickListener {
                val intent = Intent(ctx, ProfileEditActivity::class.java)
                intent.putExtra("create_new", true)
                editLauncher.launch(intent)
                sheet.dismiss()
            }
        })

        val scroll = ScrollView(ctx)
        val listContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (user in users) {
            val isCurrent = user.id == currentId
            listContainer.addView(buildUserRow(ctx, user, isCurrent,
                onSelect = { selected ->
                    ProfilePrefs.setCurrentUserId(ctx, selected.id)
                    refreshProfile()
                    sheet.dismiss()
                },
                onDelete = { deleted ->
                    AlertDialog.Builder(ctx)
                        .setTitle("\u5220\u9664\u7528\u6237")
                        .setMessage("\u786E\u5B9A\u5220\u9664\u201C${deleted.name.ifEmpty { "\u672A\u8BBE\u7F6E" }}\u201D\u5417\uFF1F\u8BE5\u7528\u6237\u7684\u6240\u6709\u6570\u636E\u5C06\u88AB\u6E05\u9664\u3002")
                        .setPositiveButton("\u5220\u9664") { _, _ ->
                            ProfilePrefs.deleteUser(ctx, deleted.id)
                            sheet.dismiss()
                            refreshProfile()
                        }
                        .setNegativeButton("\u53D6\u6D88", null)
                        .show()
                }
            ))
        }

        if (users.isEmpty()) {
            listContainer.addView(TextView(ctx).apply {
                text = "\u6682\u65E0\u7528\u6237"
                setPadding(24, 16, 24, 16)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                setTextColor(0xFF888888.toInt())
            })
        }

        scroll.addView(listContainer)
        root.addView(scroll)
        sheet.setContentView(root)
        sheet.show()
    }

    private fun buildUserRow(
        ctx: Context,
        user: UserProfile,
        isCurrent: Boolean,
        onSelect: (UserProfile) -> Unit,
        onDelete: (UserProfile) -> Unit
    ): View {
        val bd = user.birthday?.date
        val genderText = if (user.gender.name == "MALE") "\u2642" else "\u2640"
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 12, 24, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { onSelect(user) }
            if (isCurrent) {
                setBackgroundColor(0x26000000.toInt())
            }

            // Avatar
            addView(TextView(ctx).apply {
                val dn = user.name.ifEmpty { "?" }
                text = dn.first().toString()
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(56, 56)
                val ci = abs(user.id.hashCode()) % avatarColors.size
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(avatarColors[ci])
                }
                background = bg
            })

            // Name + gender
            addView(TextView(ctx).apply {
                val dn = user.name.ifEmpty { "\u672A\u8BBE\u7F6E" }
                text = dn + "  " + genderText
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(16, 0, 8, 0)
                }
            })

            // Checkmark
            if (isCurrent) {
                addView(TextView(ctx).apply {
                    text = "\u2713"
                    textSize = 18f
                    setTextColor(0xFF4CAF50.toInt())
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 16, 0) }
                })
            }

            // Delete
            addView(ImageButton(ctx).apply {
                setImageResource(android.R.drawable.ic_delete)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                background = null
                setColorFilter(0xFF888888.toInt())
                setOnClickListener { onDelete(user) }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
