package com.woshiwangnima.healthdietpro.ui.theme

import android.content.Context
import android.util.TypedValue
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

enum class FontStyle(val label: String, val cnName: String, val sizeMultiplier: Float) {
    DISPLAY("display", "巨标", 3.5f),
    HEADLINE("headline", "大标题", 2.25f),
    TITLE("title", "标题", 1.75f),
    SUBTITLE("subtitle", "副标题", 1.375f),
    BODY("body", "正文", 1.0f),
    LABEL("label", "标注", 0.875f),
    CAPTION("caption", "脚注", 0.75f);

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
