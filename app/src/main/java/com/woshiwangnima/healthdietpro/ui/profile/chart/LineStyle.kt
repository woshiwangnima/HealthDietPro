package com.woshiwangnima.healthdietpro.ui.profile.chart

enum class LineStyle {
    LINEAR,
    CUBIC_BEZIER,
    STEPPED;

    companion object {
        fun fromSpinnerPosition(pos: Int): LineStyle = when (pos) {
            1 -> CUBIC_BEZIER
            2 -> STEPPED
            else -> LINEAR
        }

        fun toSpinnerPosition(style: LineStyle): Int = when (style) {
            LINEAR -> 0
            CUBIC_BEZIER -> 1
            STEPPED -> 2
        }
    }
}