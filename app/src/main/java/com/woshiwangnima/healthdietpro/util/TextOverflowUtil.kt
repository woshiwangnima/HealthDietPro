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

    private fun effectiveMode(view: TextView, mode: String): String {
        // marquee 要求单行且横向滚动；
        // 1) 多行输入框（EditText / TextInputEditText minLines>1）套 marquee 会冲突崩溃 → 降级 shrink
        // 2) 选中态的 Spinner 项视图位于固定宽度容器内，marquee 会让 Spinner 测量子项宽度时
        //    返回不稳定值，触发 ArrayAdapter.getView 递归/NPE → 降级 ellipsis
        if (mode != "marquee") return mode
        if (view is android.widget.EditText) {
            if (view.minLines > 1 && view.maxLines != 1) return "shrink"
        }
        return mode
    }

    private fun applyMode(view: TextView, mode: String) {
        val m = effectiveMode(view, mode)
        view.ellipsize = null
        view.setHorizontallyScrolling(false)
        view.marqueeRepeatLimit = 0
        view.isSelected = false
        view.setSingleLine(false)

        when (m) {
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

    /**
     * 创建支持文字溢出模式的 Spinner ArrayAdapter。
     *
     * 注意：Spinner 选中态（getView 返回的视图）位于固定宽度容器内，
     * marquee 会让 Spinner 测量阶段返回不稳定宽度 → 闪退，因此选中态
     * 一律不允许 marquee（降级为 ellipsis）；弹出下拉项 getDropDownView
     * 可正常应用 marquee。
     */
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
