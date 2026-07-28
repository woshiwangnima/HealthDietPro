package com.woshiwangnima.healthdietpro.model.disease

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

class DiseaseRepository private constructor(
    private val readAsset: () -> String,
) {

    constructor(context: Context) : this({
        context.assets.open("diseases.json").bufferedReader().use { it.readText() }
    })

    private var cache: DiseaseCatalog? = null
    private var diseasesById: Map<String, Disease>? = null
    private var categoriesById: Map<String, DiseaseCategory>? = null
    private var departmentsById: Map<String, CareDepartment>? = null

    fun loadAll(): List<Disease> {
        return loadCatalog().diseases
    }

    fun loadCatalog(): DiseaseCatalog {
        if (cache != null) return cache!!

        return diseaseJson.decodeFromString<DiseaseCatalog>(readAsset()).also { catalog ->
            cache = catalog
            diseasesById = catalog.diseases.associateBy { it.id }
            categoriesById = catalog.categories.associateBy { it.id }
            departmentsById = catalog.departments.associateBy { it.id }
        }
    }

    fun findById(id: String): Disease? {
        loadCatalog()
        return diseasesById!![id]
    }

    fun findByIcd11Code(code: String): List<Disease> {
        val normalized = code.normalizeQuery()
        if (normalized.isEmpty()) return emptyList()
        return loadAll().filter { disease ->
            disease.icd11References.any { it.code.normalizeQuery() == normalized }
        }
    }

    fun search(query: String, locale: Locale = Locale.getDefault()): List<Disease> {
        val normalized = query.normalizeQuery()
        if (normalized.isEmpty()) return loadAll()
        return loadAll().filter { disease ->
            disease.id.normalizeQuery().contains(normalized) ||
                disease.icd11References.any { reference ->
                    reference.code.normalizeQuery().contains(normalized)
                } ||
                disease.localizedSearchTerms(locale).any { term ->
                    term.normalizeQuery().contains(normalized)
                }
        }
    }

    fun getByCategory(categoryId: String): List<Disease> =
        loadAll().filter { categoryId in it.categoryIds }

    fun getByCourse(course: DiseaseCourse): List<Disease> =
        loadAll().filter { it.course == course }

    fun getByDepartment(departmentId: String): List<Disease> =
        loadAll().filter { departmentId in it.careDepartmentIds }

    fun findCategoryById(id: String): DiseaseCategory? {
        loadCatalog()
        return categoriesById!![id]
    }

    fun findDepartmentById(id: String): CareDepartment? {
        loadCatalog()
        return departmentsById!![id]
    }

    private fun Disease.localizedSearchTerms(locale: Locale): List<String> {
        val language = locale.language
        val selected = i18n[language] ?: i18n["zh"] ?: i18n.values.firstOrNull()
        return selected?.let { listOf(it.label) + it.aliases }.orEmpty()
    }

    companion object {
        private val diseaseJson = Json { ignoreUnknownKeys = true }

        fun fromContext(context: Context): DiseaseRepository = DiseaseRepository(context)

        fun fromAsset(path: String): DiseaseRepository = DiseaseRepository {
            File(path).readText()
        }
    }
}

private fun String.normalizeQuery(): String =
    lowercase(Locale.ROOT).filterNot { it.isWhitespace() || it == '-' || it == '_' }
