package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context

internal class BloodGlucoseRepository private constructor(private val context: Context) {
    private val archive = BloodGlucoseArchiveStore.current(context)

    fun load(): List<BloodGlucoseRecord> = archive.load().records

    fun loadArchive(): BloodGlucoseArchive = archive.load()

    fun save(records: List<BloodGlucoseRecord>) {
        archive.update { it.copy(records = records.sortedByDescending(BloodGlucoseRecord::timestamp)) }
    }

    fun importAgp(records: List<BloodGlucoseRecord>, sourceId: String): Int {
        require(records.all { it.valueMmolPerL.isFinite() && it.valueMmolPerL > 0.0 }) { "Invalid AGP glucose value" }
        var inserted = 0
        archive.update { current ->
            require(current.sources.any { it.id == sourceId }) { "Selected blood glucose source no longer exists" }
            val existingKeys = current.records.map { it.timestamp to it.valueMmolPerL }.toMutableSet()
            val additions = records.filter { existingKeys.add(it.timestamp to it.valueMmolPerL) }
            inserted = additions.size
            current.copy(
                records = (current.records + additions).sortedByDescending(BloodGlucoseRecord::timestamp),
            )
        }
        return inserted
    }

    fun saveHbA1cRecords(records: List<BloodHbA1cRecord>) {
        archive.update { it.copy(hbA1cRecords = records.sortedByDescending(BloodHbA1cRecord::timestamp)) }
    }

    fun saveSources(sources: List<BloodGlucoseSource>) {
        archive.update { it.copy(sources = sources) }
    }

    fun deleteDataForSource(sourceId: String): BloodGlucoseSourceDataCounts {
        var counts = BloodGlucoseSourceDataCounts()
        archive.update { current ->
            val glucose = current.records.count { it.sourceId == sourceId }
            val hbA1c = current.hbA1cRecords.count { it.sourceId == sourceId }
            counts = BloodGlucoseSourceDataCounts(glucose, hbA1c)
            current.copy(
                records = current.records.filterNot { it.sourceId == sourceId },
                hbA1cRecords = current.hbA1cRecords.filterNot { it.sourceId == sourceId },
            )
        }
        return counts
    }

    fun reorderSources(orderedIds: List<String>) {
        archive.update { current -> current.copy(sources = reorderBloodGlucoseSources(current.sources, orderedIds)) }
    }

    companion object {
        fun fromContext(context: Context) = BloodGlucoseRepository(context.applicationContext)
    }
}

internal data class BloodGlucoseSourceDataCounts(
    val glucoseRecords: Int = 0,
    val hbA1cRecords: Int = 0,
)
