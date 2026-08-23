package com.woshiwangnima.healthdietpro.model.diet

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class DietEditorDraft(
    val targetRecordId: String? = null,
    val record: DietRecord,
)

internal class DietEditorDraftRepository private constructor(context: Context) {
    private val prefs = UserPrefs.current(context.applicationContext)

    fun load(targetRecordId: String?): DietRecord? {
        val draft = loadDraft()
        return draft?.takeIf { it.targetRecordId == targetRecordId }?.record
    }

    fun loadDraft(): DietEditorDraft? = prefs.getString(KEY, "").takeIf(String::isNotBlank)
        ?.let { runCatching { json.decodeFromString<DietEditorDraft>(it) }.getOrNull() }

    fun save(targetRecordId: String?, record: DietRecord) {
        prefs.putString(KEY, json.encodeToString(DietEditorDraft(targetRecordId, record)))
    }

    fun clear() {
        prefs.remove(KEY)
    }

    companion object {
        private const val KEY = "diet_editor_draft_v1"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromContext(context: Context) = DietEditorDraftRepository(context)
    }
}
