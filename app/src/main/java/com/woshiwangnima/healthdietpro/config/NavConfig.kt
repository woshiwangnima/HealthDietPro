package com.woshiwangnima.healthdietpro.config

object NavConfig {
    const val BOTTOM_BAR_HEIGHT_FRACTION = 0.08f
    const val CENTER_BUTTON_SCALE = 1.2f
    const val DEFAULT_TEXT_SIZE_SP = 11f
    const val SELECTED_TEXT_SIZE_SP = 11f

    fun calculateBarHeightPx(screenHeightPx: Int, density: Float): Int =
        (screenHeightPx * BOTTOM_BAR_HEIGHT_FRACTION / density * density).toInt()
    
    fun calculateBarHeightDp(screenHeightPx: Int, density: Float): Int =
        (screenHeightPx * BOTTOM_BAR_HEIGHT_FRACTION / density).toInt()
}
