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
    private var diseasesByReferenceId: Map<String, Disease>? = null
    private var categoriesById: Map<String, DiseaseCategory>? = null
    private var departmentsById: Map<String, CareDepartment>? = null
    private var searchTermsByDiseaseId: Map<String, Set<String>>? = null

    fun loadAll(): List<Disease> {
        return loadCatalog().diseases
    }

    fun loadCatalog(): DiseaseCatalog {
        if (cache != null) return cache!!

        return diseaseJson.decodeFromString<DiseaseCatalog>(readAsset()).also { catalog ->
            cache = catalog
            diseasesById = catalog.diseases.associateBy { it.id }
            diseasesByReferenceId = catalog.diseases
                .flatMap { disease -> disease.referenceIds().map { referenceId -> referenceId to disease } }
                .toMap()
            categoriesById = catalog.categories.associateBy { it.id }
            departmentsById = catalog.departments.associateBy { it.id }
            searchTermsByDiseaseId = catalog.diseases.associate { disease ->
                disease.id to buildSet {
                    add(disease.id.normalizeQuery())
                    disease.icd11References.forEach { add(it.code.normalizeQuery()) }
                    disease.i18n.values.forEach { localized ->
                        add(localized.label.normalizeQuery())
                        localized.aliases.forEach { add(it.normalizeQuery()) }
                    }
                }
            }
        }
    }

    fun findById(id: String): Disease? {
        loadCatalog()
        return diseasesById!![id]
    }

    fun findByReferenceId(referenceId: String): Disease? {
        loadCatalog()
        return diseasesByReferenceId!![referenceId] ?: referenceId
            .removePrefix("ICD-11-")
            .let(::findByIcd11Code)
            .firstOrNull()
    }

    fun findByIcd11Code(code: String): List<Disease> {
        val normalized = code.removePrefix("ICD-11-").normalizeQuery()
        if (normalized.isEmpty()) return emptyList()
        return loadAll().filter { disease ->
            disease.icd11References.any { it.code.normalizeQuery() == normalized }
        }
    }

    fun search(query: String, locale: Locale = Locale.getDefault()): List<Disease> {
        val normalized = query.normalizeQuery()
        if (normalized.isEmpty()) return loadAll()
        loadCatalog()
        return loadAll().filter { disease ->
            searchTermsByDiseaseId!![disease.id].orEmpty().any { it.contains(normalized) }
        }
    }

    fun getByCategory(categoryId: String): List<Disease> =
        loadAll().filter { categoryId in it.categoryIds }

    fun getByCourse(course: DiseaseCourse): List<Disease> =
        loadAll().filter { it.course == course }

    fun getByDepartment(departmentId: String): List<Disease> =
        loadAll().filter { departmentId in it.careDepartmentIds }

    fun relatedToMetric(metric: HealthMetricKind): List<Disease> =
        loadAll().filter { disease -> disease.metricReferences.any { it.metric == metric } }

    fun findCategoryById(id: String): DiseaseCategory? {
        loadCatalog()
        return categoriesById!![id]
    }

    fun findDepartmentById(id: String): CareDepartment? {
        loadCatalog()
        return departmentsById!![id]
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
