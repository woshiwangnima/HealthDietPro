package com.woshiwangnima.healthdietpro.util

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

/**
 * 根据 APP 偏好设置中的"文字溢出处理"模式，统一控制 TextView / EditText / Spinner 的文字展示行为。
 *
 * 支持三种模式：
 *  - "shrink"   ：自适应缩小（AppCompatAutoSizeText）
 *  - "marquee"  ：左右轮播
 *  - "ellipsis" ：超出部分显示省略号
 */
object TextOverflowUtil {

    fun apply(view: TextView, context: Context) {
        val mode = AppPrefs.getTextOverflowMode(context)
        applyMode(view, mode)
    }

    fun apply(view: TextView, mode: String) {
        applyMode(view, mode)
    }

    private fun applyMode(view: TextView, mode: String) {
        view.ellipsize = null
        view.setHorizontallyScrolling(false)
        view.marqueeRepeatLimit = 0
        view.isSelected = false
        view.setSingleLine(false)

        when (mode) {
            "marquee" -> {
                view.ellipsize = TextUtils.TruncateAt.MARQUEE
                view.setHorizontallyScrolling(true)
                view.marqueeRepeatLimit = -1
                view.isSelected = true
                view.setSingleLine(true)
            }
            "ellipsis" -> {
                view.ellipsize = TextUtils.TruncateAt.END
                view.setSingleLine(true)
            }
            "shrink" -> {
                // 自适应缩小：默认行为，不做特殊处理
            }
        }
    }

    /** 创建支持文字溢出模式的 Spinner ArrayAdapter。 */
    fun <T> createSpinnerAdapter(
        context: Context,
        items: List<T>,
        mode: String? = null
    ): ArrayAdapter<T> {
        val overflowMode = mode ?: AppPrefs.getTextOverflowMode(context)
        return object : ArrayAdapter<T>(context, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (view is TextView) {
                    applyMode(view, overflowMode)
                    view.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                if (view is TextView) {
                    applyMode(view, overflowMode)
                }
                return view
            }
        }
    }
}
