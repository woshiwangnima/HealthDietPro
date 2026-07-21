package com.woshiwangnima.healthdietpro.model.bloodglucose

internal fun isValidBloodGlucoseValue(value: Double?): Boolean = value?.isFinite() == true && value > 0.0

internal fun normalizeBloodGlucoseTimestamp(timestamp: Long): Long = timestamp / MILLIS_PER_MINUTE * MILLIS_PER_MINUTE

private const val MILLIS_PER_MINUTE = 60_000L
