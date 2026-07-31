package com.woshiwangnima.healthdietpro.common.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.provider.Settings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseAlert
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseAlertKind
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseAlertMode

internal class BloodGlucoseAlertNotifier(private val context: Context) {
    fun notify(alert: BloodGlucoseAlert) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val channelId = channelId(alert.mode)
        ensureChannel(channelId, alert.mode)
        NotificationManagerCompat.from(context).notify(
            alert.kind.ordinal + 4100,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_blood_glucose)
                .setContentTitle(context.getString(R.string.blood_glucose_alert_notification_title))
                .setContentText(context.getString(alert.kind.notificationTextRes()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setTimeoutAfter(alert.durationSeconds.coerceAtLeast(1) * 1_000L)
                .build(),
        )
    }

    private fun ensureChannel(channelId: String, mode: BloodGlucoseAlertMode) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(channelId, context.getString(R.string.blood_glucose_alert_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(mode != BloodGlucoseAlertMode.Sound)
                if (mode == BloodGlucoseAlertMode.Vibration) {
                    setSound(null, null)
                } else {
                    setSound(
                        Settings.System.DEFAULT_NOTIFICATION_URI,
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                    )
                }
            },
        )
    }

    private fun channelId(mode: BloodGlucoseAlertMode): String = "blood_glucose_alert_${mode.name.lowercase()}"
}

private fun BloodGlucoseAlertKind.notificationTextRes(): Int = when (this) {
    BloodGlucoseAlertKind.High -> R.string.blood_glucose_alert_high_message
    BloodGlucoseAlertKind.Low -> R.string.blood_glucose_alert_low_message
    BloodGlucoseAlertKind.EmergencyLow -> R.string.blood_glucose_alert_emergency_low_message
    BloodGlucoseAlertKind.RisingFast -> R.string.blood_glucose_alert_rising_message
    BloodGlucoseAlertKind.FallingFast -> R.string.blood_glucose_alert_falling_message
}
