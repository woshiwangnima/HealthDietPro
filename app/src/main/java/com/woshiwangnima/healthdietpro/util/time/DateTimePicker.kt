package com.woshiwangnima.healthdietpro.util.time

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 公用日期+时间选择器：先弹出 [DatePickerDialog] 选择年月日，
 * 用户确认后再串联弹出 [TimePickerDialog] 选择时、分（秒以 0 固定），
 * 最终通过 [onPicked] 回调给调用方一个 epoch millis 值。
 *
 * 用法：
 * ```
 * DateTimePicker.show(context, initialMillis) { ts ->
 *     binding.timeRow.text = DateTimePicker.format(ts)
 * }
 * ```
 */
object DateTimePicker {

    /** 把 epoch millis 格式化为 "yyyy-MM-dd HH:mm"。 */
    fun format(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    /** 把 epoch millis 格式化为 "yyyy-MM-dd HH:mm:ss"。 */
    fun formatWithSeconds(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))

    /** 仅日期 "yyyy-MM-dd"。 */
    fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))

    /**
     * 弹出选择器。 [initial] 缺省取当前时间。 [is24Hour] 默认 true。
     */
    fun show(
        context: Context,
        initial: Long = System.currentTimeMillis(),
        is24Hour: Boolean = true,
        onPicked: (Long) -> Unit
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = initial }
        show(context, cal, is24Hour, onPicked)
    }

    private fun show(
        context: Context,
        cal: Calendar,
        is24Hour: Boolean,
        onPicked: (Long) -> Unit
    ) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    is24Hour
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}