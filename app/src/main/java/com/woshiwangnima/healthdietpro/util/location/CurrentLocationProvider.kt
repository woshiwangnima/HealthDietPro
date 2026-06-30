package com.woshiwangnima.healthdietpro.util.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * 原生 LocationManager 异步封装：不依赖 Google Play Services，适合国内无 GMS 环境。
 * 调用方必须先确认已获得 [Manifest.permission.ACCESS_FINE_LOCATION]，
 * 否则 [getCurrentLocation] 会通过 [Result.Err] 路径直接返回。
 */
class CurrentLocationProvider(private val context: Context) {

    sealed class Result {
        data class Ok(val lng: Double, val lat: Double) : Result()
        data class Err(val reason: String) : Result()
    }

    private var consumed = false   // 防止 timeout 与 onLocationChanged 都触发 callback

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Result) -> Unit) {
        if (!hasPermission()) {
            callback(Result.Err("no_permission"))
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { lm.isProviderEnabled(it) }
        if (provider == null) {
            // 没有可用定位提供器；尝试被动 provider 的最近已知坐标兜底
            try {
                val last = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                deliver(last, callback)
            } catch (_: SecurityException) {
                callback(Result.Err("no_provider"))
            }
            return
        }

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                if (consumed) return
                consumed = true
                lm.removeUpdates(this)
                callback(Result.Ok(location.longitude, location.latitude))
            }
        }

        try {
            // 单次请求；通过 Looper.getMainLooper() 保证回调在主线程
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            callback(Result.Err("no_permission_runtime"))
            return
        }

        // 4 秒兜底：超时则按失败处理
        Handler(Looper.getMainLooper()).postDelayed({
            if (consumed) return@postDelayed
            consumed = true
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
            callback(Result.Err("timeout"))
        }, 4_000L)
    }

    private fun deliver(loc: Location?, cb: (Result) -> Unit) {
        if (loc == null) cb(Result.Err("no_last_location"))
        else cb(Result.Ok(loc.longitude, loc.latitude))
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}