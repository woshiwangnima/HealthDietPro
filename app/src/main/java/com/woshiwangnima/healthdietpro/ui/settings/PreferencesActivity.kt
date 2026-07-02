package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityPreferencesBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class PreferencesActivity : BaseBackActivity() {

    private lateinit var binding: ActivityPreferencesBinding

    override fun getTitleText(): String = "偏好设置"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        UnitConverter.init(this)
        com.woshiwangnima.healthdietpro.model.prefs.AppPrefs.ensureFontStyleTokenDefaults(this)
        buildFontPreviews()
        buildFontStylePreviews()
        buildUnitRows()
        refreshDisplay()
        setupClickListeners()
        setupFontScaleBar()
    }

    private data class FontPreviewItem(val cnName: String, val dimenRes: Int)

    private val fontPreviewItems = listOf(
        FontPreviewItem("脚注", R.dimen.text_size_caption),
        FontPreviewItem("标注", R.dimen.text_size_label),
        FontPreviewItem("正文", R.dimen.text_size_body),
        FontPreviewItem("副标题", R.dimen.text_size_subtitle),
        FontPreviewItem("标题", R.dimen.text_size_title),
        FontPreviewItem("大标题", R.dimen.text_size_headline)
    )

    private fun buildFontPreviews() {
        val container = binding.fontPreviewContainer
        container.removeAllViews()
        val scaledDensity = resources.displayMetrics.density * resources.configuration.fontScale
        val captionPx = resources.getDimension(R.dimen.text_size_caption)
        for (item in fontPreviewItems) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val px = resources.getDimension(item.dimenRes)
            val label = TextView(this).apply {
                text = item.cnName
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, captionPx)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
            }
            val sizeInfo = TextView(this).apply {
                text = "%.0fsp".format(px / scaledDensity)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, captionPx)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.15f)
            }
            val preview = TextView(this).apply {
                text = "预览ABcd123"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.65f)
            }
            row.addView(label)
            row.addView(sizeInfo)
            row.addView(preview)
            container.addView(row)
        }
    }

    /** "默认字体风格"静态展示行（仅开发演示，不持久化用户修改；令牌存 app 级）。 */
    private fun buildFontStylePreviews() {
        val container = binding.fontStylePreviewContainer
        container.removeAllViews()
        val sample = "示例文字 Aa"

        val alphaRows = listOf(
            "低透明（70%）" to AppPrefs.getFontStyleTokenAlphaLow(this),
            "中透明（50%）" to AppPrefs.getFontStyleTokenAlphaMid(this),
            "高透明（30%）" to AppPrefs.getFontStyleTokenAlphaHigh(this)
        )
        for ((label, alpha) in alphaRows) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val tv = TextView(this).apply {
                text = label
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_size_body))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
            }
            val preview = TextView(this).apply {
                text = sample
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_size_body))
                val a = (0xFF * alpha).toInt().coerceIn(0, 255)
                setTextColor((textColors.defaultColor and 0x00FFFFFF) or (a shl 24))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
            }
            row.addView(tv); row.addView(preview)
            container.addView(row)
        }

        if (AppPrefs.getFontStyleTokenUnderline(this)) {
            val span = android.text.SpannableString(sample)
            span.setSpan(android.text.style.UnderlineSpan(), 0, span.length, 0)
            container.addView(styleRow("链接", span))
        }
        if (AppPrefs.getFontStyleTokenStrike(this)) {
            val span = android.text.SpannableString(sample)
            span.setSpan(android.text.style.StrikethroughSpan(), 0, span.length, 0)
            container.addView(styleRow("删除线", span))
        }
        if (AppPrefs.getFontStyleTokenItalic(this)) {
            container.addView(styleRow("斜体", sample) {
                it.typeface = android.graphics.Typeface.create(it.typeface, android.graphics.Typeface.ITALIC)
            })
        }
    }

    private fun styleRow(
        label: String, content: CharSequence, applyStyle: (TextView) -> Unit = {}
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val tv = TextView(this).apply {
            text = label
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.text_size_body))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
        }
        val preview = TextView(this).apply {
            text = content
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.text_size_body))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
        }
        applyStyle(preview)
        row.addView(tv); row.addView(preview)
        return row
    }

    private fun buildUnitRows() {
        val repo = UnitConverter.getRepository() ?: return
        val container = binding.unitSettingsContainer
        container.removeAllViews()

        for (category in repo.getCategories()) {
            val visibleUnits = category.units.filter { !it.hidden }
            if (visibleUnits.isEmpty()) continue
            val row = LayoutInflater.from(this).inflate(R.layout.item_preference_row, null)
            val label = row.findViewById<TextView>(R.id.rowLabel)
            val value = row.findViewById<TextView>(R.id.rowValue)

            label.text = category.categoryCn
            val currentId = AppPrefs.getUnit(this, category.id, category.baseUnit)
            val currentUnit = visibleUnits.find { it.id == currentId }
            value.text = currentUnit?.let { "${it.symbolCn} ${it.symbolEn}" } ?: currentId

            row.setOnClickListener {
                val items = visibleUnits.map { "${it.symbolCn}  ${it.symbolEn}" }.toTypedArray()
                val checkedIndex = visibleUnits.indexOfFirst { u ->
                    u.id == AppPrefs.getUnit(this@PreferencesActivity, category.id, category.baseUnit)
                }.coerceAtLeast(0)
                showPicker(category.categoryCn, items, checkedIndex) { which ->
                    AppPrefs.setUnit(this@PreferencesActivity, category.id, visibleUnits[which].id)
                    buildUnitRows()
                }
            }
            container.addView(row)
        }
    }

    private fun refreshDisplay() {
        binding.firstDayValue.text =
            if (AppPrefs.getFirstDayOfWeek(this) == "MONDAY") "周一" else "周日"
        binding.darkModeValue.text = when (AppPrefs.getDarkMode(this)) {
            "FOLLOW_SYSTEM" -> "跟随系统"
            "YES" -> "深色模式"
            else -> "浅色模式"
        }
        refreshOverflowDisplay()
    }

    private fun refreshOverflowDisplay() {
        val mode = AppPrefs.getTextOverflowMode(this)
        binding.textOverflowValue.text = when (mode) {
            "marquee" -> "左右轮播"
            "ellipsis" -> "超出省略"
            else -> "自适应缩小"
        }
        binding.marqueeSpeedRow.visibility = if (mode == "marquee") android.view.View.VISIBLE else android.view.View.GONE
        binding.marqueeSpeedValue.text = "${AppPrefs.getMarqueeSpeed(this)}"
    }

    private fun setupClickListeners() {
        binding.firstDayRow.setOnClickListener {
            val items = arrayOf("周一", "周日")
            val checkedIndex = if (AppPrefs.getFirstDayOfWeek(this) == "MONDAY") 0 else 1
            showPicker("每周第一天", items, checkedIndex) { which ->
                AppPrefs.setFirstDayOfWeek(this, if (which == 0) "MONDAY" else "SUNDAY")
                refreshDisplay()
            }
        }

        binding.darkModeRow.setOnClickListener {
            val items = arrayOf("跟随系统", "深色模式", "浅色模式")
            val modes = arrayOf("FOLLOW_SYSTEM", "YES", "NO")
            val currentMode = AppPrefs.getDarkMode(this)
            val checkedIndex = modes.indexOf(currentMode).coerceAtLeast(0)
            showPicker("深色模式", items, checkedIndex) { which ->
                AppPrefs.setDarkMode(this, modes[which])
                refreshDisplay()
                AppCompatDelegate.setDefaultNightMode(when (modes[which]) {
                    "YES" -> AppCompatDelegate.MODE_NIGHT_YES
                    "NO" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                })
            }
        }

        binding.textOverflowRow.setOnClickListener {
            val items = arrayOf("自适应缩小", "左右轮播", "超出省略")
            val modes = arrayOf("shrink", "marquee", "ellipsis")
            val checkedIndex = modes.indexOf(AppPrefs.getTextOverflowMode(this)).coerceAtLeast(0)
            showPicker("文字溢出处理", items, checkedIndex) { which ->
                AppPrefs.setTextOverflowMode(this, modes[which])
                refreshOverflowDisplay()
            }
        }

        binding.marqueeSpeedRow.setOnClickListener {
            val input = EditText(this).apply {
                setText("${AppPrefs.getMarqueeSpeed(this@PreferencesActivity)}")
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            AlertDialog.Builder(this)
                .setTitle("轮播速度 (ms/字)")
                .setView(input)
                .setPositiveButton("确定") { _, _ ->
                    val speed = input.text.toString().toIntOrNull()?.coerceIn(50, 2000) ?: 200
                    AppPrefs.setMarqueeSpeed(this@PreferencesActivity, speed)
                    refreshOverflowDisplay()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun setupFontScaleBar() {
        val currentScale = AppPrefs.getFontScale(this)
        val progress = ((currentScale - 0.8f) / 0.7f * 70f).toInt().coerceIn(0, 70)
        binding.fontScaleSeekBar.progress = progress
        binding.fontScaleValue.text = "${(currentScale * 100).toInt()}%"

        binding.fontScaleSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                val scale = 0.8f + p / 70f * 0.7f
                binding.fontScaleValue.text = "${(scale * 100).toInt()}%"
                positionFontScaleValue(seekBar ?: return)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                val p = seekBar?.progress ?: 22
                val scale = 0.8f + p / 70f * 0.7f
                AppPrefs.setFontScale(this@PreferencesActivity, scale)
                recreate()
            }
        })
        binding.fontScaleSeekBar.post { positionFontScaleValue(binding.fontScaleSeekBar) }
    }

    /** 把 fontScaleValue 水平移动到 SeekBar 拇指正上方居中。 */
    private fun positionFontScaleValue(seekBar: android.widget.SeekBar) {
        val track = seekBar.width - seekBar.paddingLeft - seekBar.paddingRight
        if (track <= 0) return
        val thumbX = seekBar.paddingLeft + track.toFloat() * seekBar.progress / seekBar.max
        val value = binding.fontScaleValue
        if (value.width == 0) value.measure(
            android.view.View.MeasureSpec.UNSPECIFIED,
            android.view.View.MeasureSpec.UNSPECIFIED
        )
        val halfW = (value.width.takeIf { it > 0 } ?: value.measuredWidth) / 2f
        value.translationX = thumbX - halfW
    }

    private fun showPicker(title: String, items: Array<String>, checkedIndex: Int, onSelected: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(items, checkedIndex) { dialog, which ->
                onSelected(which)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
