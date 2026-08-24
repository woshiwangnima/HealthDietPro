package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RelativeTimeUnit
import com.woshiwangnima.healthdietpro.common.time.relativeTimeSince

@Composable
internal fun recordLatestSummary(timestamp: Long, value: String): String =
    stringResource(R.string.record_latest_summary, recordRelativeTime(timestamp), value)

@Composable
internal fun recordLatestUpdatePrefix(timestamp: Long): String = recordLatestSummary(timestamp, "").trimEnd()

@Composable
private fun recordRelativeTime(timestamp: Long): String {
    val relativeTime = relativeTimeSince(timestamp, System.currentTimeMillis())
    val textRes = when (relativeTime.unit) {
        RelativeTimeUnit.SECOND -> R.string.record_seconds_ago
        RelativeTimeUnit.MINUTE -> R.string.record_minutes_ago
        RelativeTimeUnit.HOUR -> R.string.record_hours_ago
        RelativeTimeUnit.DAY -> R.string.record_days_ago
        RelativeTimeUnit.MONTH -> R.string.record_months_ago
        RelativeTimeUnit.YEAR -> R.string.record_years_ago
    }
    return stringResource(textRes, relativeTime.amount)
}
