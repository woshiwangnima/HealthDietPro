package com.woshiwangnima.healthdietpro.model.price

import android.content.Context

internal class PriceRepository private constructor(context: Context) {
    private val archive = PriceArchiveStore.current(context)

    fun load(): PriceArchive = archive.load()

    fun upsert(record: PriceRecord) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == record.id } + record)
    }

    fun delete(id: String) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id == id })
    }

    /** 删除合并卡片里的全部记录：只写一次盘，避免逐条读改写。 */
    fun deleteAll(ids: Set<String>) = archive.update { current ->
        current.copy(records = current.records.filterNot { it.id in ids })
    }

    companion object {
        fun fromContext(context: Context) = PriceRepository(context.applicationContext)
    }
}
