package com.woshiwangnima.healthdietpro.model.medication

import android.content.Context
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 用药记录的 per-user 存储。所有数据挂在 `medication_records_${userId}` 这个键下，
 * 由 [ProfilePrefs.makeChartStateKey] 拼接，删除用户时自动随 `_${uid}` 后缀清理。
 *
 * 衍生数据：
 *  - 药品名历史 = 所有记录中药品名的去重列表（按最近一次出现排序）
 *  - 用药方式历史 = 所有记录中方式的去重列表（按最近一次出现排序）
 *  - 特定药品名的默认填充 = 该名称最近一次记录的剂量/规格/方式
 */
object MedicationPrefs {

    fun getRecords(context: Context): List<MedicationRecord> {
        return archive(context).records
    }

    fun saveRecords(context: Context, records: List<MedicationRecord>) {
        MedicationArchiveStore.current(context).update {
            it.copy(records = records.sortedByDescending { record -> record.timestamp })
        }
    }

    fun addRecord(context: Context, record: MedicationRecord) {
        val list = getRecords(context).toMutableList()
        list.add(0, record) // 最近记录排在最前
        saveRecords(context, list)
    }

    fun getCatalog(context: Context): List<MedicationCatalogItem> {
        return archive(context).catalog
    }

    fun saveCatalog(context: Context, catalog: List<MedicationCatalogItem>) {
        MedicationArchiveStore.current(context).update { it.copy(catalog = catalog) }
    }

    fun upsertCatalogItem(context: Context, item: MedicationCatalogItem) {
        val catalog = getCatalog(context).toMutableList()
        val index = catalog.indexOfFirst { it.id == item.id }
        if (index >= 0) catalog[index] = item else catalog.add(item)
        saveCatalog(context, catalog)
    }

    fun deleteCatalogItem(context: Context, id: String) {
        saveCatalog(context, getCatalog(context).filterNot { it.id == id })
    }

    fun saveCatalogImage(context: Context, bitmap: android.graphics.Bitmap): String =
        saveImage(context, "catalog", bitmap)

    fun saveRecordPhoto(context: Context, bitmap: android.graphics.Bitmap): String =
        saveImage(context, "records", bitmap)

    fun loadAttachment(context: Context, relativePath: String): android.graphics.Bitmap? =
        File(context.filesDir, relativePath).takeIf(File::exists)
            ?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath) }

    fun deleteAttachment(context: Context, relativePath: String) {
        val file = File(context.filesDir, relativePath)
        if (file.canonicalPath.startsWith(attachmentDirectory(context).canonicalPath + File.separator)) file.delete()
    }

    private fun archive(context: Context): MedicationArchive =
        MedicationArchiveStore.current(context).load()

    private fun saveImage(context: Context, type: String, bitmap: android.graphics.Bitmap): String {
        val directory = File(attachmentDirectory(context), type).apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
        return file.relativeTo(context.filesDir).invariantSeparatorsPath
    }

    private fun attachmentDirectory(context: Context): File {
        val userId = ProfilePrefs.getCurrentUserId(context)
        val safeUserId = userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(context.filesDir, "user_archives/$safeUserId/attachments/medications")
    }


    /** 药品名历史：按最近使用排序的去重列表。 */
    fun getMedicationNameHistory(context: Context): List<String> {
        val seen = linkedSetOf<String>()
        getRecords(context).forEach { if (it.medicationName.isNotEmpty()) seen.add(it.medicationName) }
        return seen.toList()
    }

    /** 用药方式历史：内置默认 + 按最近使用排序的去重列表。 */
    fun getMethodHistory(context: Context): List<String> {
        val defaults = context.resources
            .getStringArray(R.array.medication_record_default_methods)
            .toList()
        val seen = linkedSetOf<String>()
        defaults.forEach { seen.add(it) }
        getRecords(context).forEach { if (it.method.isNotEmpty()) seen.add(it.method) }
        return seen.toList()
    }

    /**
     * 与药品名关联的默认填充：取该名称最近一条记录的剂量/规格/方式。
     * 没有历史记录时返回 null。
     */
    data class NameDefaults(
        val doseValue: Float, val doseUnit: String,
        val specValue: Float, val specUnitCategory: String, val specUnitId: String,
        val method: String
    )

    fun findNameDefaults(context: Context, name: String): NameDefaults? {
        val r = getRecords(context).firstOrNull {
            it.medicationName == name && it.medicationName.isNotEmpty()
        } ?: return null
        return NameDefaults(
            doseValue = r.doseValue, doseUnit = r.doseUnit,
            specValue = r.specValue, specUnitCategory = r.specUnitCategory, specUnitId = r.specUnitId,
            method = r.method
        )
    }
}
