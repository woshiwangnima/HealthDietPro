package com.woshiwangnima.healthdietpro.model.archive

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private val compactJson = Json { explicitNulls = false }
private val prettyJson = Json { prettyPrint = true; explicitNulls = false }

internal fun stableJsonString(rawJson: String, prettyPrint: Boolean): String = runCatching {
    val element = compactJson.parseToJsonElement(rawJson)
    val formatter = if (prettyPrint) prettyJson else compactJson
    formatter.encodeToString(JsonElement.serializer(), stableJson(element))
}.getOrDefault(rawJson)

internal fun stableUserArchiveJsonString(rawJson: String): String = runCatching {
    val root = compactJson.parseToJsonElement(rawJson).jsonObject
    val orderedRoot = buildMap {
        USER_ARCHIVE_KEYS.forEach { key -> root[key]?.let { put(key, stableJson(it)) } }
        root.entries.filter { (key, _) -> key !in USER_ARCHIVE_KEYS }
            .sortedBy { it.key }
            .forEach { (key, value) -> put(key, stableJson(value)) }
    }
    compactJson.encodeToString(JsonElement.serializer(), JsonObject(orderedRoot))
}.getOrDefault(rawJson)

internal fun stableJson(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(element.entries.sortedBy { it.key }.associate { (key, value) -> key to stableJson(value) })
    is JsonArray -> JsonArray(element.map(::stableJson))
    else -> element
}

private val USER_ARCHIVE_KEYS = listOf(
    "formatVersion",
    "appVersion",
    "exportedAt",
    "sourceUserId",
    "metadata",
    "profile",
    "bodyMetrics",
    "medications",
    "bloodGlucose",
    "bloodPressure",
    "water",
    "diseaseRecords",
    "customFoods",
    "userPreferences",
    "attachments",
)
