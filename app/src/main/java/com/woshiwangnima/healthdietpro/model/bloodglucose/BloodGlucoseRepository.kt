package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context

internal class BloodGlucoseRepository private constructor(private val context: Context) {
    private val archive = BloodGlucoseArchiveStore.current(context)

    fun load(): List<BloodGlucoseRecord> = archive.load().records

    fun save(records: List<BloodGlucoseRecord>) {
        archive.update { it.copy(records = records.sortedByDescending(BloodGlucoseRecord::timestamp)) }
    }

    companion object {
        fun fromContext(context: Context) = BloodGlucoseRepository(context.applicationContext)
    }
}
