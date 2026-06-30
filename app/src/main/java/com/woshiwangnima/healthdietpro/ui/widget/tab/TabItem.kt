package com.woshiwangnima.healthdietpro.ui.widget.tab

import androidx.annotation.DrawableRes

data class TabItem(
    @DrawableRes val icon: Int? = null,
    val label: String,
    val tag: String? = null
)