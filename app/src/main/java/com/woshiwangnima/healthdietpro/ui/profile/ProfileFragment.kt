package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.woshiwangnima.healthdietpro.databinding.FragmentProfileBinding
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.settings.AppSettingsActivity
import com.woshiwangnima.healthdietpro.util.UnitConverter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var diseaseRepo: DiseaseRepository
    private lateinit var editLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(requireContext())
        editLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            refreshProfile()
        }
        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            refreshProfile()
        }
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
            val intent = Intent(requireContext(), AppSettingsActivity::class.java)
            settingsLauncher.launch(intent)
        }

        binding.editProfileBtn.setOnClickListener {
            val intent = Intent(requireContext(), ProfileEditActivity::class.java)
            editLauncher.launch(intent)
        }

        refreshProfile()
    }

    private fun refreshProfile() {
        val profile = ProfilePrefs.load(requireContext())
        binding.profileName.text = profile.name.ifEmpty { "未设置" }
        val genderStr = if (profile.gender.name == "MALE") "男" else "女"
        val birthdayStr = profile.birthday?.date ?: "未设置"
        binding.profileGender.text = "$genderStr | $birthdayStr"

        binding.profileProvince.text = profile.province.ifEmpty { "未设置" }

        if (profile.diseaseIds.isEmpty()) {
            binding.profileDiseases.text = "无"
        } else {
            val diseases = diseaseRepo.loadAll()
            val names = profile.diseaseIds.map { id ->
                diseases.find { it.id == id }?.name ?: id
            }
            binding.profileDiseases.text = names.joinToString("、")
        }

        val heightUnit = AppPrefs.getHeightUnit(requireContext())
        val weightUnit = AppPrefs.getWeightUnit(requireContext())
        val latestHeight = profile.latestHeight
        binding.profileHeight.text = if (latestHeight != null)
            UnitConverter.formatWithUnit("height", latestHeight.value, heightUnit) else "无记录"

        val latestWeight = profile.latestWeight
        binding.profileWeight.text = if (latestWeight != null)
            UnitConverter.formatWithUnit("weight", latestWeight.value, weightUnit) else "无记录"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
