package com.woshiwangnima.healthdietpro.ui.profile.list

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.ui.profile.BodyRecordAdapter
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.showConfirmDialog
import java.util.Calendar

class DataListFragment : Fragment() {

    var onRecordsChanged: (() -> Unit)? = null
    var records: MutableList<BodyRecord> = mutableListOf()
    private var unitId: String = UnitCategoryType.Length.defaultUnitId
    private var category: String = UnitCategoryType.Length.id
    private var isHeight: Boolean = true
    private lateinit var adapter: BodyRecordAdapter
    private var isDialogShowing = false
    private var rootView: View? = null

    companion object {
        private const val ARG_RECORDS = "records"
        private const val ARG_UNIT = "unit"
        private const val ARG_CATEGORY = "category"
        private const val ARG_IS_HEIGHT = "is_height"

        fun newInstance(records: ArrayList<BodyRecord>, unit: String, category: String, isHeight: Boolean): DataListFragment {
            val fragment = DataListFragment()
            val args = Bundle()
            args.putSerializable(ARG_RECORDS, records)
            args.putString(ARG_UNIT, unit)
            args.putString(ARG_CATEGORY, category)
            args.putBoolean(ARG_IS_HEIGHT, isHeight)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            records = (it.getSerializable(ARG_RECORDS, ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
            unitId = it.getString(ARG_UNIT, UnitCategoryType.Length.defaultUnitId) ?: UnitCategoryType.Length.defaultUnitId
            category = it.getString(ARG_CATEGORY, UnitCategoryType.Length.id) ?: UnitCategoryType.Length.id
            isHeight = it.getBoolean(ARG_IS_HEIGHT, true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_data_list, container, false)
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recordList)
        val addBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.addRecordBtn)

        adapter = BodyRecordAdapter(unitId)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        rootView = view
        refreshList()

        adapter.onDelete = { pos ->
            requireContext().showConfirmDialog("确认删除", "确定要删除这条记录吗？") {
                records.removeAt(pos)
                refreshList()
                onRecordsChanged?.invoke()
            }
        }

        adapter.onClick = { pos -> showRecordSheet(editPosition = pos) }

        addBtn.setOnClickListener { showRecordSheet(editPosition = -1) }
        return view
    }

    private fun refreshList() {
        records.sortByDescending { it.date }
        adapter.records = records
        rootView?.findViewById<TextView>(R.id.totalCount)?.text = "共 ${records.size} 条记录"
    }

    /**
     * "添加/编辑 记录" 弹层改用与「切换用户」一致的 BottomSheetDialog 风格：
     * 从底部弹出、同一套标题外观 (TextAppearance.Material3.TitleLarge) + 内容卡片，
     * 再加一个明确的「保存/添加」MaterialButton 收尾。editPosition=-1 表示新增。
     */
    private fun showRecordSheet(editPosition: Int) {
        if (isDialogShowing) return
        isDialogShowing = true
        val ctx = requireContext()
        val isEdit = editPosition >= 0
        val editing = if (isEdit) records[editPosition] else null

        val sheet = BottomSheetDialog(ctx)
        val density = ctx.resources.displayMetrics.density
        val padHorizontal = (24 * density).toInt()
        val padVertical = (16 * density).toInt()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padHorizontal, (20 * density).toInt(), padHorizontal, (20 * density).toInt())
        }

        root.addView(TextView(ctx).apply {
            text = if (isEdit)
                (if (isHeight) "编辑身高记录" else "编辑体重记录")
            else
                (if (isHeight) "添加身高记录" else "添加体重记录")
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        })

        root.addView(TextView(ctx).apply {
            text = if (isHeight)
                "请选择测量日期并填写身高数值，单位为「${unitId}」"
            else
                "请选择测量日期并填写体重数值，单位为「${unitId}」"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * density).toInt() }
        })

        // 日期行 (可点击)
        val dateRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (16 * density).toInt() }
        }
        val dateLabel = TextView(ctx).apply {
            text = "日期"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
        }
        dateRow.addView(dateLabel)
        val dateInput = TextView(ctx).apply {
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.edit_text_bg)
            setPadding((12 * density).toInt(), (12 * density).toInt(),
                (12 * density).toInt(), (12 * density).toInt())
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (6 * density).toInt() }
        }
        dateRow.addView(dateInput)
        root.addView(dateRow)

        // 数值输入框：直接复用 dialog_add_body_record.xml 中的 TextInputLayout
        // (已用 outlined-box 样式定义)，省得在这里手动重建 Material 样式。
        val valueRow = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_body_record, root, false)
        val valueInput = valueRow.findViewById<TextInputEditText>(R.id.dialogRecordValue)
        val unitText = valueRow.findViewById<TextView>(R.id.dialogRecordUnit)
        unitText.text = "单位：$unitId"
        // 原布局第一个子项是"日期"显示行 —— 上面已经重做日期行，移除它以免重复
        (valueRow as? ViewGroup)?.let { vg ->
            val first = vg.getChildAt(0)
            if (first?.id == R.id.dialogRecordDate) vg.removeView(first)
        }
        root.addView(valueRow)

        // 按钮行
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (20 * density).toInt() }
        }
        val cancelBtn = MaterialButton(ctx).apply {
            text = "取消"
            setOnClickListener { sheet.dismiss() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, (8 * density).toInt(), 0) }
        }
        val confirmBtn = MaterialButton(ctx).apply {
            text = if (isEdit) "保存" else "添加"
        }
        btnRow.addView(cancelBtn)
        btnRow.addView(confirmBtn)
        root.addView(btnRow)

        // 初始化值
        val cal = Calendar.getInstance()
        if (isEdit && editing != null) {
            dateInput.text = editing.date
            val displayValue = UnitConverter.fromBase(category, editing.value, unitId)
            valueInput.setText(String.format("%.1f", displayValue))
            try {
                val parts = editing.date.split("-")
                if (parts.size == 3) {
                    cal.set(Calendar.YEAR, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } catch (_: Exception) {}
            dateInput.setOnClickListener {
                DatePickerDialog(ctx, { _, year, month, day ->
                    dateInput.text = "%04d-%02d-%02d".format(year, month + 1, day)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        } else {
            val today = "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            dateInput.text = today
            dateInput.setOnClickListener {
                DatePickerDialog(ctx, { _, year, month, day ->
                    dateInput.text = "%04d-%02d-%02d".format(year, month + 1, day)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        confirmBtn.setOnClickListener {
            val dateStr = dateInput.text.toString()
            val valueStr = valueInput.text.toString()
            if (valueStr.isBlank()) {
                Toast.makeText(ctx, "请输入数值", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val value = valueStr.toFloatOrNull()
            if (value == null || value <= 0) {
                Toast.makeText(ctx, "请输入有效数值", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val baseValue = UnitConverter.toBase(category, value, unitId)
            if (isEdit && editing != null) {
                records[editPosition] = BodyRecord(date = dateStr, value = baseValue, unit = unitId)
            } else {
                records.add(BodyRecord(date = dateStr, value = baseValue, unit = unitId))
            }
            refreshList()
            onRecordsChanged?.invoke()
            sheet.dismiss()
        }

        sheet.setOnDismissListener { isDialogShowing = false }
        sheet.setContentView(root)
        sheet.show()
    }
}