package com.woshiwangnima.healthdietpro.model.medication

/** Pure one-time transformation of name-based legacy records into catalog-linked events. */
internal object MedicationMigration {
    data class Result(
        val catalog: List<MedicationCatalogItem>,
        val records: List<MedicationRecord>,
        val changed: Boolean,
    )

    fun migrate(
        existingCatalog: List<MedicationCatalogItem>,
        records: List<MedicationRecord>,
    ): Result {
        val catalogByNormalizedName = existingCatalog
            .filter { normalizeName(it.name).isNotEmpty() }
            .associateBy { normalizeName(it.name) }
            .toMutableMap()
        val generated = existingCatalog.toMutableList()
        var changed = false
        records.filter { it.medicationId == null && normalizeName(it.medicationName).isNotEmpty() }
            .groupBy { normalizeName(it.medicationName) }
            .toSortedMap()
            .forEach { (normalizedName, matchingRecords) ->
                if (catalogByNormalizedName[normalizedName] == null) {
                    val newest = matchingRecords.maxBy { it.timestamp }
                    val item = MedicationCatalogItem(
                        id = uniqueLegacyId(normalizedName, generated),
                        name = newest.medicationName.trim(),
                        specValue = newest.specValue,
                        specUnitCategory = newest.specUnitCategory,
                        specUnitId = newest.specUnitId,
                        manufacturer = newest.manufacturer,
                        defaultMethod = newest.method,
                        imagePath = newest.medicationImagePath,
                    )
                    catalogByNormalizedName[normalizedName] = item
                    generated += item
                    changed = true
                }
            }
        val linked = records.map { record ->
            val item = catalogByNormalizedName[normalizeName(record.medicationName)]
            if (record.medicationId == null && item != null) {
                changed = true
                record.copy(medicationId = item.id)
            } else {
                record
            }
        }
        return Result(generated, linked, changed)
    }

    fun normalizeName(name: String): String = name.trim().lowercase()

    private fun uniqueLegacyId(name: String, catalog: List<MedicationCatalogItem>): String {
        val base = "legacy_${name.hashCode().toUInt().toString(16)}"
        val used = catalog.mapTo(mutableSetOf()) { it.id }
        return generateSequence(1) { it + 1 }
            .map { suffix -> if (suffix == 1) base else "${base}_$suffix" }
            .first { it !in used }
    }
}
