package com.woshiwangnima.healthdietpro.model.food

/** Lightweight, non-serialized data required to render one browser card. */
internal data class FoodCardMetadata(
    val id: String,
    val kind: FoodKind,
    val primaryName: String,
    val aliases: List<String>,
    val categoryLabels: List<String>,
    val cookingMethodLabel: String?,
    val componentCount: Int?,
    val imageKey: String?,
    val systemTags: List<String>,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    val isRecent: Boolean,
    val energyPer100g: Double,
    val glycemicIndex: Double?,
    val glycemicLoadPer100g: Double?,
)
