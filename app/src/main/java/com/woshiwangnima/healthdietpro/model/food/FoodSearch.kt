package com.woshiwangnima.healthdietpro.model.food

/** Returns the best name-match rank, or null when the food does not match. */
internal fun FoodItem.searchMatchRank(query: String): Int? {
    val token = query.normalizeFoodSearchToken()
    if (token.isBlank()) return 0

    val names = searchableNames().map(String::normalizeFoodSearchToken).filter(String::isNotBlank)
    if (names.any { it == token || it.startsWith(token) }) return 0
    if (names.any { it.contains(token) }) return 1

    val queryCharacters = token.toSet()
    return if (names.any { name -> name.any(queryCharacters::contains) }) 2 else null
}

private fun String.normalizeFoodSearchToken(): String = lowercase().filterNot(Char::isWhitespace)
