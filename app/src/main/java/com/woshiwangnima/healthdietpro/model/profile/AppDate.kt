package com.woshiwangnima.healthdietpro.model.profile

data class AppDate(
    val date: String,
    val time: String? = null
) {
    val displayText: String get() = if (time != null) "$date $time" else date
    val isDateTime: Boolean get() = time != null
}
