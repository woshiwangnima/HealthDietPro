package com.woshiwangnima.healthdietpro.model.disease

import kotlinx.serialization.Serializable

@Serializable
enum class DietaryRecommendation {
    INCREASE,
    FREE,
    LIMIT,
    STRICT_LIMIT,
    FORBID
}
