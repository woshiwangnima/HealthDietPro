package com.woshiwangnima.healthdietpro.ui.theme

import android.content.Context
import android.util.TypedValue
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

enum class FontStyle(val label: String, val sizeMultiplier: Float) {
    DISPLAY("display", 3.5f),
    HEADLINE("headline", 2.25f),
    TITLE("title", 1.75f),
    SUBTITLE("subtitle", 1.375f),
    BODY("body", 1.0f),
    LABEL("label", 0.875f),
    CAPTION("caption", 0.75f);

    companion object {
        private const val BASE_SP = 16f

        fun sp(context: Context, style: FontStyle): Float {
            val scale = AppPrefs.getFontScale(context)
            return BASE_SP * style.sizeMultiplier * scale
        }

        fun px(context: Context, style: FontStyle): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp(context, style),
                context.resources.displayMetrics
            )
        }
    }
}
