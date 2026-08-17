package com.woshiwangnima.healthdietpro.model.diet

import android.content.Context

internal class DietRepository private constructor(context: Context) {
    private val archive = DietArchiveStore.current(context)

    fun load(): DietArchive = archive.load()

    fun upsert(record: DietRecord) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == record.id } + record)
    }

    fun delete(id: String) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == id })
    }

    companion object {
        fun fromContext(context: Context) = DietRepository(context.applicationContext)
    }
}