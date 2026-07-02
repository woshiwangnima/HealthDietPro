package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityAppSettingsBinding
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import java.io.File

class AppSettingsActivity : BaseBackActivity() {

    private lateinit var binding: ActivityAppSettingsBinding

    override fun getTitleText(): String = "软件设置"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        UnitConverter.init(this)
        setupListeners()
        refreshCacheSize()
    }

    private fun setupListeners() {
        // 消息通知：补 APP_UID 确保新 Android 上能正确锁定本应用
        binding.messageSettingsRow.setOnClickListener {
            val uid = try {
                packageManager.getApplicationInfo(packageName, 0).uid
            } catch (_: Exception) { -1 }
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra("android.provider.extra.APP_UID", uid)
            })
        }

        // 系统权限管理：跳转本应用详情页（含权限分组、通知、存储等全部）
        binding.permissionSettingsRow.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        // 清除缓存：清 cacheDir + codeCacheDir + externalCacheDir 并汇报释放大小
        binding.clearCacheRow.setOnClickListener {
            val before = cacheTotalSize()
            deleteRecursively(cacheDir)
            deleteRecursively(codeCacheDir)
            externalCacheDir?.let { deleteRecursively(it) }
            val freed = before - cacheTotalSize()
            val kb = freed / 1024
            if (kb >= 1024) {
                Toast.makeText(this, "已清理 %.1f MB".format(kb / 1024f), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已清理 $kb KB", Toast.LENGTH_SHORT).show()
            }
            refreshCacheSize()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCacheSize()
    }

    private fun refreshCacheSize() {
        val totalBytes = cacheTotalSize()
        binding.cacheSizeText.text = formatStorageSize(totalBytes)
    }

    private fun formatStorageSize(bytes: Long): String {
        val repo = UnitConverter.getRepository() ?: return formatLegacy(bytes)
        val storage = repo.getCategory(UnitCategoryType.Storage.id) ?: return formatLegacy(bytes)
        val best = storage.units.lastOrNull { bytes >= it.toBase } ?: storage.units.firstOrNull() ?: return formatLegacy(bytes)
        val converter = UnitConverter
        val converted = converter.fromBase(UnitCategoryType.Storage.id, bytes.toFloat(), best.id)
        val symbol = best.symbolCn
        return if (best.id == "b" || best.id == "kb") {
            "%.0f %s".format(converted, symbol)
        } else {
            "%.1f %s".format(converted, symbol)
        }
    }

    private fun formatLegacy(bytes: Long): String {
        val kb = bytes / 1024
        return if (kb >= 1024) "%.1f MB".format(kb / 1024f) else "$kb KB"
    }

    private fun cacheTotalSize(): Long {
        var total = 0L
        total += sizeOf(cacheDir)
        total += sizeOf(codeCacheDir)
        externalCacheDir?.let { total += sizeOf(it) }
        return total
    }

    private fun sizeOf(file: File): Long {
        if (file.isFile) return file.length()
        if (file.isDirectory) {
            var s = 0L
            file.listFiles()?.forEach { s += sizeOf(it) }
            return s
        }
        return 0
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) file.listFiles()?.forEach { deleteRecursively(it) }
        // 注意：不删除目录本身（cacheDir/codeCacheDir 是系统目录），只清其内容
        if (file != cacheDir && file != codeCacheDir && file != externalCacheDir) {
            file.delete()
        }
    }
}
