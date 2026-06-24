package com.woshiwangnima.healthdietpro.model.disease

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DiseaseRepository(private val context: Context) {

    private var cache: List<Disease>? = null

    fun loadAll(): List<Disease> {
        if (cache != null) return cache!!

        val json = context.assets.open("diseases.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Disease>>() {}.type
        val diseases: List<Disease> = Gson().fromJson(json, type)
        cache = diseases
        return diseases
    }

    fun getSorted(province: String?): List<Disease> {
        val all = loadAll()
        if (province.isNullOrEmpty()) {
            return all.sortedByDescending { avgPrevalence(it) }
        }
        return all.sortedByDescending { it.prevalence[province] ?: avgPrevalence(it) }
    }

    private fun avgPrevalence(disease: Disease): Float {
        if (disease.prevalence.isEmpty()) return 0f
        return disease.prevalence.values.sum() / disease.prevalence.size
    }
}
