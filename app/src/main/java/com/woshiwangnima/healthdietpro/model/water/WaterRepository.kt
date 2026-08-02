package com.woshiwangnima.healthdietpro.model.water

import android.content.Context

internal class WaterRepository private constructor(context: Context) {
    private val archive = WaterArchiveStore.current(context)

    fun load(): WaterArchive = archive.load()

    fun add(record: WaterRecord) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == record.id } + record)
    }

    fun delete(id: String) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == id })
    }

    fun saveSettings(activityLevel: ActivityLevel, quickRecords: List<WaterQuickRecord>) = archive.update { current ->
        current.copy(activityLevel = activityLevel, quickRecords = quickRecords)
    }

    companion object {
        fun fromContext(context: Context) = WaterRepository(context.applicationContext)
    }
}
