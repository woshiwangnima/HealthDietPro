package com.woshiwangnima.healthdietpro.model.medication

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    private val json = Json { ignoreUnknownKeys = true }

    private fun prefs(context: Context) =
        context.getSharedPreferences("health_diet_prefs", Context.MODE_PRIVATE)

    private fun keyFor(context: Context): String =
        ProfilePrefs.makeChartStateKey(context, KEY_RECORDS)

    private fun catalogKeyFor(context: Context): String =
        ProfilePrefs.makeChartStateKey(context, KEY_CATALOG)

    fun getRecords(context: Context): List<MedicationRecord> {
        return migrated(context).records
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
        return migrated(context).catalog
    }

    private fun migrated(context: Context): MedicationMigration.Result {
        val records = readRecords(context)
        val catalog = readCatalog(context)
        val migration = MedicationMigration.migrate(catalog, records)
        if (migration.changed) {
            saveCatalog(context, migration.catalog)
            saveRecords(context, migration.records)
        }
        return migration
    }

    private fun readRecords(context: Context): List<MedicationRecord> {
        val raw = prefs(context).getString(keyFor(context), null) ?: return emptyList()
        return try {
            json.decodeFromString<List<MedicationRecord>>(raw)
        } catch (_: Exception) {
            // Existing installations wrote this key with Gson before kotlinx serialization.
            runCatching {
                Gson().fromJson<List<MedicationRecord>>(
                    raw,
                    object : TypeToken<List<MedicationRecord>>() {}.type,
                ) ?: emptyList()
            }.getOrDefault(emptyList())
        }
    }

    private fun readCatalog(context: Context): List<MedicationCatalogItem> {
        val stored = prefs(context).getString(catalogKeyFor(context), null)
        return stored?.let { runCatching { json.decodeFromString<List<MedicationCatalogItem>>(it) }.getOrDefault(emptyList()) }
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
