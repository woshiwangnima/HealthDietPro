package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.woshiwangnima.healthdietpro.databinding.FragmentProfileBinding
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import com.woshiwangnima.healthdietpro.ui.settings.AppSettingsComposeActivity
import com.woshiwangnima.healthdietpro.util.TextOverflowUtil
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.io.File
import kotlin.math.abs

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var diseaseRepo: DiseaseRepository
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
        diseaseRepo = DiseaseRepository(requireContext())

        binding.settingsBtn.setOnClickListener {
            val intent = Intent(requireContext(), AppSettingsComposeActivity::class.java)
            settingsLauncher.launch(intent)
        }

        binding.profileDetailBtn.setOnClickListener {
            val intent = Intent(requireContext(), ProfileEditActivity::class.java)
            editLauncher.launch(intent)
        }

        binding.userSettingsBtn.setOnClickListener {
            startActivity(Intent(requireContext(),
                com.woshiwangnima.healthdietpro.ui.settings.UserSettingsActivity::class.java))
        }

        binding.switchUserBtn.setOnClickListener {
            showUserSwitchSheet()
        }

        refreshProfile()
    }

    private fun refreshProfile() {
        val ctx = requireContext()
        val profile = ProfilePrefs.load(ctx)
        val displayName = profile.name.ifEmpty { "未设置" }
        binding.profileName.text = displayName

        val initial = displayName.first().toString()
        val colorIdx = abs(profile.id.hashCode()) % avatarColors.size

        val avatarLoaded = if (profile.avatarFileName.isNotEmpty()) {
            val file = File(ctx.filesDir, "avatars/${profile.avatarFileName}")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.avatarText.text = ""
                    val d = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(resources, bitmap)
                    d.isCircular = true
                    binding.avatarText.background = d
                    true
                } else false
            } else false
        } else false

        if (!avatarLoaded) {
            binding.avatarText.text = initial
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(avatarColors[colorIdx])
            }
            binding.avatarText.background = bg
        }

        val genderIcon: String
        val genderText: String
        val genderColor: Int
        if (profile.gender.name == "MALE") {
            genderIcon = "\u2642"
            genderText = "男"
            genderColor = Color.parseColor("#2196F3")
        } else {
            genderIcon = "\u2640"
            genderText = "女"
            genderColor = Color.parseColor("#E91E63")
        }
        binding.profileGenderIcon.text = genderIcon
        binding.profileGenderIcon.setTextColor(genderColor)
        binding.profileGenderText.text = genderText
        binding.profileGenderText.setTextColor(genderColor)

        val age = profile.age
        if (age != null) {
            binding.profileAge.text = age.toString() + "岁"
        } else {
            binding.profileAge.text = ""
        }

        binding.profileBirthday.text = profile.birthday?.date ?: "未设置"

        // 第三行：地区
        var regionDisplay = profile.region.display()
        // 兜底：若新 region 仅省代码无省名（迁移后的数据），补一下显示
        if (profile.region.provinceCode.isNotEmpty() && profile.region.provinceName.isEmpty()) {
            runCatching {
                com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
                    .fromContext(ctx).findByCode(profile.region.provinceCode)?.name
            }.getOrNull()?.let { name ->
                regionDisplay = profile.region.copy(provinceName = name).display()
            }
        }
        binding.profileRegionLine.text = regionDisplay

        // 第三行：病史 —— 为"无"时隐藏
        val diseaseText = if (profile.diseaseIds.isEmpty()) {
            ""
        } else {
            val diseases = diseaseRepo.loadAll()
            val names = profile.diseaseIds.map { id ->
                diseases.find { it.id == id }?.name ?: id
            }
            names.joinToString("、")
        }
        if (diseaseText.isEmpty()) {
            binding.profileDiseaseLine.visibility = View.GONE
            binding.profileDiseaseSeparator.visibility = View.GONE
        } else {
            binding.profileDiseaseLine.text = diseaseText
            binding.profileDiseaseLine.visibility = View.VISIBLE
            binding.profileDiseaseSeparator.visibility = View.VISIBLE
            TextOverflowUtil.apply(binding.profileDiseaseLine, ctx)
        }
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
