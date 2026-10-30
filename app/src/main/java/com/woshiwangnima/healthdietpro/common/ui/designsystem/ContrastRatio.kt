package com.woshiwangnima.healthdietpro.common.ui.designsystem

private const val RED_WEIGHT = 0.2126
private const val GREEN_WEIGHT = 0.7152
private const val BLUE_WEIGHT = 0.0722

private fun linearize(channel8Bit: Int): Double {
    val channel = channel8Bit / 255.0
    return if (channel <= 0.03928) {
        channel / 12.92
    } else {
        val shifted = (channel + 0.055) / 1.055
        Math.pow(shifted, 2.4)
    }
}

fun relativeLuminance(argb: Int): Double {
    val red = linearize((argb shr 16) and 0xFF)
    val green = linearize((argb shr 8) and 0xFF)
    val blue = linearize(argb and 0xFF)
    return RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue
}

fun contrastRatio(foregroundArgb: Int, backgroundArgb: Int): Double {
    val foreground = relativeLuminance(foregroundArgb)
    val background = relativeLuminance(backgroundArgb)
    val lighter = maxOf(foreground, background)
    val darker = minOf(foreground, background)
    return (lighter + 0.05) / (darker + 0.05)
}

fun meetsContrast(foregroundArgb: Int, backgroundArgb: Int, minimum: Double): Boolean =
    contrastRatio(foregroundArgb, backgroundArgb) >= minimum
