package com.woshiwangnima.healthdietpro.common.format

import java.text.NumberFormat
import java.util.Locale

/**
 * 整数显示统一带千分位（通用 UI 规范 §4.3-1：整数使用千分位分隔，不显示小数）。
 * 只用于界面展示，不用于存档或计算。
 */
fun formatGroupedInteger(value: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(value)

fun formatGroupedInteger(value: Int, locale: Locale = Locale.getDefault()): String =
    formatGroupedInteger(value.toDouble(), locale)
