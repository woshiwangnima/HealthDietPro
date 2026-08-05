package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Read-only DRIs lookup used to calculate the displayed NRV percentage. */
internal class DriNrvRepository private constructor(
    private val populationAssets: () -> Map<String, String>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var references: Map<String, Map<String, Double>>? = null

    fun referenceFor(profile: UserProfile): NrvReference {
        val populationId = populationIdFor(profile.age, profile.gender)
        val populationValues = populationId?.let { valuesByPopulation()[it] }
        return if (populationValues.isNullOrEmpty()) {
            NrvReference(ADULT_REFERENCE_ID, adultNrv)
        } else {
            NrvReference(populationId, adultNrv + populationValues)
        }
    }

    fun referencesFor(profile: UserProfile): List<NrvReference> = listOf(
        referenceFor(profile),
        NrvReference(ADULT_REFERENCE_ID, adultNrv),
    ).distinctBy(NrvReference::id)

    private fun valuesByPopulation(): Map<String, Map<String, Double>> = references ?: populationAssets()
        .values
        .mapNotNull { raw -> runCatching { json.decodeFromString<DriPopulationDto>(raw) }.getOrNull() }
        .associate { dto -> dto.population.id to dto.data.mapNotNull(DriEntryDto::nrvValue).toMap() }
        .also { references = it }

    private fun populationIdFor(age: Int?, gender: Gender): String? {
        val range = when (age) {
            in 1..3 -> "children_1_3y"
            in 4..6 -> "children_4_6y"
            in 7..8 -> "children_7_8y"
            else -> return null
        }
        return "${range}_${gender.name.lowercase()}"
    }

    companion object {
        const val ADULT_REFERENCE_ID = "adult_standard"

        /** Chinese prepackaged-food adult NRV, normalized to food nutrient base units. */
        private val adultNrv = mapOf(
            "ENERGY" to 2_008.0, "PROTEIN" to 60.0, "FAT" to 60.0, "CHO" to 300.0,
            "FIBER" to 25.0, "VITA" to 800.0, "VITD" to 5.0, "VITE" to 14.0,
            "VITK" to 80.0, "VITB1" to 1.2, "VITB2" to 1.2, "NIACIN" to 14.0,
            "VITB6" to 1.4, "FOLATE" to 400.0, "VITB12" to 2.4, "VITC" to 100.0,
            "CA" to 800.0, "P" to 700.0, "K" to 2_000.0, "NA" to 2_000.0,
            "MG" to 300.0, "FE" to 15.0, "I" to 150.0, "ZN" to 12.0,
            "SE" to 60.0, "CU" to 1.5, "MN" to 3.0, "F" to 1.0,
        )

        fun fromContext(context: Context) = DriNrvRepository {
            context.assets.list("DRIs/populations").orEmpty().associateWith { filename ->
                context.assets.open("DRIs/populations/$filename").bufferedReader().use { it.readText() }
            }
        }

        internal fun fromPopulationAssets(assets: Map<String, String>) = DriNrvRepository { assets }
    }
}

internal data class NrvReference(val id: String, private val values: Map<String, Double>) {
    fun percent(code: String, amount: FoodAmount, multiplier: Double): Double? {
        val reference = values[code] ?: return null
        val normalizedAmount = when {
            code == "ENERGY" && amount.unitId.equals("kJ", true) -> amount.value / 4.184
            else -> amount.value
        }
        return (normalizedAmount * multiplier / reference * 100.0).takeIf { it.isFinite() }
    }
}

@Serializable
private data class DriPopulationDto(
    val population: DriPopulationInfoDto,
    val data: List<DriEntryDto> = emptyList(),
)

@Serializable
private data class DriPopulationInfoDto(val id: String)

@Serializable
private data class DriEntryDto(
    val code: String,
    val RNI: Double? = null,
    val AI: Double? = null,
    val unit: String = "",
) {
    fun nrvValue(): Pair<String, Double>? {
        val value = RNI ?: AI ?: return null
        if (unit.contains("%E") || unit.contains("MJ")) return null
        return code to value
    }
}
