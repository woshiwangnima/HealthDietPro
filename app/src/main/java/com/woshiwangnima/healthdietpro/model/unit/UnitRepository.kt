package com.woshiwangnima.healthdietpro.model.unit

import android.content.Context
import kotlinx.serialization.json.Json

class UnitRepository private constructor(
    private val jsonStrProvider: () -> String,
) {

    constructor(context: Context) : this({
        context.assets.open("units.json").bufferedReader().use { it.readText() }
    })

    private var cache: List<UnitCategory>? = null

    private val json = Json { ignoreUnknownKeys = true }

    fun getCategories(): List<UnitCategory> {
        if (cache != null) return cache!!
        val jsonStr = jsonStrProvider()
        val categories: List<UnitCategory> = json.decodeFromString(jsonStr)
        cache = categories
        return categories
    }

    fun getCategory(categoryId: String): UnitCategory? =
        getCategories().find { it.id == categoryId }

    fun getUnit(categoryId: String, unitId: String): UnitDef? =
        getCategory(categoryId)?.units?.find { it.id == unitId }

    fun getUnitIds(categoryId: String): Array<String> =
        getCategory(categoryId)?.units?.map { it.id }?.toTypedArray() ?: emptyArray()

    companion object {
        fun fromContext(context: Context): UnitRepository = UnitRepository(context)

        fun fromAsset(path: String): UnitRepository = UnitRepository({
            java.io.File(path).readText()
        })
    }
}
