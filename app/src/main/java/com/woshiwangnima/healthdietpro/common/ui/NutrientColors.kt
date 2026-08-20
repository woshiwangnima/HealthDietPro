package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.ui.graphics.Color

/** 营养素统一配色，与「食物详情-营养档案-营养素供能」保持一致。 */
internal val NutrientEnergyColor = Color(0xFFF57C00)
internal val NutrientCarbsColor = Color(0xFFF9A825)
internal val NutrientProteinColor = Color(0xFF43A047)
internal val NutrientFatColor = Color(0xFFE53935)
internal val NutrientFiberColor = Color(0xFF00897B)
internal val NutrientAlcoholColor = Color(0xFF6D4C41)
internal val NutrientOrganicAcidColor = Color(0xFF8E24AA)
internal val NutrientOtherColor = Color(0xFF757575)

internal fun nutrientColor(nutrientId: String): Color = when (nutrientId.uppercase()) {
    "ENERGY" -> NutrientEnergyColor
    "CHO" -> NutrientCarbsColor
    "PROTEIN" -> NutrientProteinColor
    "FAT" -> NutrientFatColor
    "FIBER" -> NutrientFiberColor
    "ALCOHOL" -> NutrientAlcoholColor
    "ORGANIC_ACID" -> NutrientOrganicAcidColor
    else -> NutrientOtherColor
}