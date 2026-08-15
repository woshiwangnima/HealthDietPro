package com.woshiwangnima.healthdietpro.model.sleep

import android.content.Context

internal class SleepRepository private constructor(context: Context) {
    private val archive = SleepArchiveStore.current(context)

    fun load(): SleepArchive = archive.load()

    fun upsert(record: SleepRecord) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == record.id } + record)
    }

    fun delete(id: String) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == id })
    }

    companion object {
        fun fromContext(context: Context) = SleepRepository(context.applicationContext)
    }
}