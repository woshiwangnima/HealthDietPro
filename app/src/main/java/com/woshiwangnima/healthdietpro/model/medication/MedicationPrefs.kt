package com.woshiwangnima.healthdietpro.model.medication

import android.content.Context
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

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

    private const val KEY_RECORDS = "medication_records"
    private const val KEY_CATALOG = "medication_catalog"
    private const val KEY_CATALOG_BACKUP = "medication_catalog_backup_v1"
    private const val KEY_RECORDS_BACKUP = "medication_records_backup_v1"
    private val json = Json { ignoreUnknownKeys = true }

    private fun prefs(context: Context) =
        context.getSharedPreferences("health_diet_prefs", Context.MODE_PRIVATE)

    private fun keyFor(context: Context): String =
        ProfilePrefs.makeChartStateKey(context, KEY_RECORDS)

    private fun catalogKeyFor(context: Context): String =
        ProfilePrefs.makeChartStateKey(context, KEY_CATALOG)

    fun getRecords(context: Context): List<MedicationRecord> {
        return readRecords(context)
    }

    fun saveRecords(context: Context, records: List<MedicationRecord>) {
        prefs(context).edit()
            .putString(keyFor(context), json.encodeToString(records))
            .apply()
    }

    fun addRecord(context: Context, record: MedicationRecord) {
        val list = getRecords(context).toMutableList()
        list.add(0, record) // 最近记录排在最前
        saveRecords(context, list)
    }

    fun getCatalog(context: Context): List<MedicationCatalogItem> {
        return readCatalog(context)
    }

    private fun readRecords(context: Context): List<MedicationRecord> {
        return readRecords(context, keyFor(context))
    }

    private fun readRecords(context: Context, key: String = keyFor(context)): List<MedicationRecord> {
        val raw = prefs(context).getString(key, null) ?: return emptyList()
        return runCatching {
            val migrated = migrateRecords(raw)
            val records = json.decodeFromString<List<MedicationRecord>>(migrated)
            val canonical = json.encodeToString(records)
            if (canonical != raw) persistMigration(context, key, raw, canonical, KEY_RECORDS_BACKUP)
            records
        }.getOrDefault(emptyList())
    }

    private fun readCatalog(context: Context): List<MedicationCatalogItem> {
        return readCatalog(context, catalogKeyFor(context))
    }

    private fun readCatalog(context: Context, key: String = catalogKeyFor(context)): List<MedicationCatalogItem> {
        val stored = prefs(context).getString(key, null)
        return stored?.let { raw -> runCatching {
            val migrated = migrateCatalog(raw)
            val catalog = json.decodeFromString<List<MedicationCatalogItem>>(migrated)
            val canonical = json.encodeToString(catalog)
            if (canonical != raw) persistMigration(context, key, raw, canonical, KEY_CATALOG_BACKUP)
            catalog
        }.getOrDefault(emptyList()) }
            ?: emptyList()
    }

    fun saveCatalog(context: Context, catalog: List<MedicationCatalogItem>) {
        prefs(context).edit()
            .putString(catalogKeyFor(context), json.encodeToString(catalog))
            .apply()
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

    fun restoreLatestCatalogBackup(context: Context): Boolean = restoreBackup(context, KEY_CATALOG_BACKUP, catalogKeyFor(context))

    fun restoreLatestRecordsBackup(context: Context): Boolean = restoreBackup(context, KEY_RECORDS_BACKUP, keyFor(context))

    private fun restoreBackup(context: Context, backupBaseKey: String, targetKey: String): Boolean {
        val p = prefs(context)
        val backupKey = ProfilePrefs.makeChartStateKey(context, backupBaseKey)
        val raw = p.getString(backupKey, null) ?: return false
        p.edit().putString(targetKey, raw).remove(backupKey).apply()
        return true
    }

    private fun persistMigration(context: Context, key: String, original: String, migrated: String, backupBaseKey: String) {
        val p = prefs(context)
        val backupKey = ProfilePrefs.makeChartStateKey(context, backupBaseKey)
        val editor = p.edit().putString(key, migrated)
        if (!p.contains(backupKey)) editor.putString(backupKey, original)
        editor.commit()
    }

    private fun migrateCatalog(raw: String): String {
        val root = json.parseToJsonElement(raw)
        if (root !is JsonArray) return raw
        var changed = false
        val result = root.map { element ->
            val obj = element.jsonObject
            if ("indicationTags" !in obj || "indications" in obj) return@map element
            changed = true
            buildJsonObject {
                obj.forEach { (key, value) -> if (key != "indicationTags") put(key, value) }
                put("indications", JsonArray(emptyList()))
                put("legacyIndicationTags", obj["indicationTags"] ?: JsonArray(emptyList()))
            }
        }
        return if (changed) JsonArray(result).toString() else raw
    }

    private fun migrateRecords(raw: String): String {
        val root = json.parseToJsonElement(raw)
        if (root !is JsonArray) return raw
        var changed = false
        val result = root.map { element ->
            val obj = element.jsonObject
            if ("purposes" !in obj || "indicationReferences" in obj) return@map element
            changed = true
            buildJsonObject {
                obj.forEach { (key, value) -> if (key != "purposes") put(key, value) }
                put("indicationReferences", JsonArray(emptyList()))
                put("legacyPurposeTags", obj["purposes"] ?: JsonArray(emptyList()))
            }
        }
        return if (changed) JsonArray(result).toString() else raw
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
