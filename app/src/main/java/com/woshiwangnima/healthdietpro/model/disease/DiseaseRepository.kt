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

    /**
     * 按地区排序疾病。参数 [provinceCode] 为 GB/T 2260 二位代码（如 "11"）。
     * 与 diseases.json prevalence map key 口径一致。
     */
    fun getSorted(provinceCode: String?): List<Disease> {
        val all = loadAll()
        if (provinceCode.isNullOrEmpty()) {
            return all.sortedByDescending { avgPrevalence(it) }
        }
        return all.sortedByDescending { it.prevalence[provinceCode] ?: avgPrevalence(it) }
    }

    private fun avgPrevalence(disease: Disease): Float {
        if (disease.prevalence.isEmpty()) return 0f
        return disease.prevalence.values.sum() / disease.prevalence.size
    }
}
