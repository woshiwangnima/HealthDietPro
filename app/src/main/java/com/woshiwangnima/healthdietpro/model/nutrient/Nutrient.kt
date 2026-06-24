package com.woshiwangnima.healthdietpro.model.nutrient

enum class Nutrient(val id: String, val displayName: String, val unit: String) {
    SODIUM("sodium", "钠", "mg"),
    POTASSIUM("potassium", "钾", "mg"),
    ENERGY("energy", "能量", "kcal"),
    PROTEIN("protein", "蛋白质", "g"),
    FAT("fat", "脂肪", "g"),
    CARBOHYDRATE("carbohydrate", "碳水化合物", "g"),
    DIETARY_FIBER("dietary_fiber", "膳食纤维", "g"),
    CHOLESTEROL("cholesterol", "胆固醇", "mg"),
    SATURATED_FAT("saturated_fat", "饱和脂肪", "g"),
    SUGAR("sugar", "糖", "g"),
    CALCIUM("calcium", "钙", "mg"),
    IRON("iron", "铁", "mg");

    companion object {
        fun fromId(id: String): Nutrient? = entries.find { it.id == id }
    }
}
