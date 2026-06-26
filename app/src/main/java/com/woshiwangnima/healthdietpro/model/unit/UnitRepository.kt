package com.woshiwangnima.healthdietpro.model.unit

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UnitRepository(private val context: Context) {

    private var cache: List<UnitCategory>? = null

    fun getCategories(): List<UnitCategory> {
        if (cache != null) return cache!!
        val json = context.assets.open("units.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<UnitCategory>>() {}.type
        val categories: List<UnitCategory> = Gson().fromJson(json, type)
        cache = categories
        return categories
    }

    fun getCategory(categoryId: String): UnitCategory? =
        getCategories().find { it.id == categoryId }

    fun getUnit(categoryId: String, unitId: String): UnitDef? =
        getCategory(categoryId)?.units?.find { it.id == unitId }

    fun getUnitIds(categoryId: String): Array<String> =
        getCategory(categoryId)?.units?.map { it.id }?.toTypedArray() ?: emptyArray()
}
