package com.woshiwangnima.healthdietpro.model.profile

import java.io.Serializable

data class BodyRecord(
    val date: String,
    val value: Float,
    val unit: String = "cm"
) : Serializable
