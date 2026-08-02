package com.woshiwangnima.healthdietpro.model.bloodpressure

import android.content.Context

internal class BloodPressureRepository private constructor(context: Context) {
    private val archive = BloodPressureArchiveStore.current(context)

    fun load(): List<BloodPressureRecord> = archive.load().records

    fun save(records: List<BloodPressureRecord>) {
        archive.save(records)
    }

    companion object {
        fun fromContext(context: Context) = BloodPressureRepository(context.applicationContext)
    }
}
