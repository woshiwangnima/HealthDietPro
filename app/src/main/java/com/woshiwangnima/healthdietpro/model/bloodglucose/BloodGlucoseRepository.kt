package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context

internal class BloodGlucoseRepository private constructor(private val context: Context) {
    private val archive = BloodGlucoseArchiveStore.current(context)

    fun load(): List<BloodGlucoseRecord> = archive.load().records

    fun loadArchive(): BloodGlucoseArchive = archive.load()

    fun save(records: List<BloodGlucoseRecord>) {
        archive.update { it.copy(records = records.sortedByDescending(BloodGlucoseRecord::timestamp)) }
    }

    fun saveSources(sources: List<BloodGlucoseSource>) {
        archive.update { it.copy(sources = sources) }
    }

    fun reorderSources(orderedIds: List<String>) {
        archive.update { current -> current.copy(sources = reorderBloodGlucoseSources(current.sources, orderedIds)) }
    }

    companion object {
        fun fromContext(context: Context) = BloodGlucoseRepository(context.applicationContext)
    }
}
