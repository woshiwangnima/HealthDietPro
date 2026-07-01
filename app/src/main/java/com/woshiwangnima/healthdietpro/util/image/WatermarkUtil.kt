package com.woshiwangnima.healthdietpro.util.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 拍照水印叠加：在图片底部画一道半透明黑→透明渐变带，并在其上写出
 * 「时间 + 地点」两行白字。原地修改并返回 [bitmap] 的副本；不改变原 bitmap。
 *
 * 调用方典型场景：刚拍完照 → 解码出 Bitmap → 追加水印 → 保存到 filesDir。
 */
object WatermarkUtil {

    /**
     * @param bitmap        原始照片位图
     * @param timestamp     照片时刻 (epoch millis)
     * @param locationText  已格式化的位置字符串（如「安徽省 - 合肥市 - 蜀山区」），空则不画
     * @param maxWidth      输出最大宽度，超过则等比缩小（保持宽高比）。0 表示不缩
     */
    fun apply(
        bitmap: Bitmap,
        timestamp: Long,
        locationText: String = "",
        maxWidth: Int = 1280
    ): Bitmap {
        val src = if (maxWidth > 0 && bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            Bitmap.createScaledBitmap(
                bitmap, maxWidth, (bitmap.height * ratio).toInt(), true
            )
        } else bitmap

        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val w = canvas.width
        val h = canvas.height

        // 底部水印区高度约占图高 18%
        val barHeight = (h * 0.18f).toInt().coerceAtLeast(96)

        // 渐变背景（从底向上 加深）
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, h - barHeight.toFloat(), 0f, h.toFloat(),
            intArrayOf(Color.TRANSPARENT, 0xA6000000.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, h - barHeight.toFloat(), w.toFloat(), h.toFloat(), paint)

        // 文字画笔
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (h * 0.030f).coerceAtLeast(28f)
            typeface = Typeface.DEFAULT_BOLD
        }

        val pad = (w * 0.03f).coerceAtLeast(24f)
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))

        canvas.drawText(timeStr, pad, h - barHeight * 0.45f, textPaint)
        if (locationText.isNotEmpty()) {
            textPaint.textSize = (h * 0.024f).coerceAtLeast(22f)
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(locationText, pad, h - barHeight * 0.18f, textPaint)
        }
        return out
    }
}