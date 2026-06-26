package com.woshiwangnima.healthdietpro.ui.profile.chart

enum class LineStyle {
    LINEAR,
    BEZIER,
    SPLINE,
    STEPPED_FRONT,
    STEPPED_BACK;

    companion object {
        fun fromSpinnerPosition(position: Int): LineStyle = when (position) {
            0 -> LINEAR
            1 -> BEZIER
            2 -> SPLINE
            3 -> STEPPED_FRONT
            4 -> STEPPED_BACK
            else -> LINEAR
        }

        fun toSpinnerPosition(style: LineStyle): Int = when (style) {
            LINEAR -> 0
            BEZIER -> 1
            SPLINE -> 2
            STEPPED_FRONT -> 3
            STEPPED_BACK -> 4
        }
    }
}
