package com.woshiwangnima.healthdietpro.ui.profile.chart

enum class LineStyle {
    LINEAR,
    BEZIER,
    SPLINE,
    CATMULL_ROM,
    MONOTONE,
    STEPPED_FRONT,
    STEPPED_BACK;

    companion object {
        fun fromSpinnerPosition(position: Int): LineStyle = when (position) {
            0 -> LINEAR
            1 -> BEZIER
            2 -> SPLINE
            3 -> CATMULL_ROM
            4 -> MONOTONE
            5 -> STEPPED_FRONT
            6 -> STEPPED_BACK
            else -> LINEAR
        }

        fun toSpinnerPosition(style: LineStyle): Int = when (style) {
            LINEAR -> 0
            BEZIER -> 1
            SPLINE -> 2
            CATMULL_ROM -> 3
            MONOTONE -> 4
            STEPPED_FRONT -> 5
            STEPPED_BACK -> 6
        }
    }
}
