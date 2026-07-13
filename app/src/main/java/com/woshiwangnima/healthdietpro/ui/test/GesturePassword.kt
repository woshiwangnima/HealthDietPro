package com.woshiwangnima.healthdietpro.ui.test

internal fun normalizeGesturePattern(points: List<Int>): List<Int> {
    val normalized = mutableListOf<Int>()
    points.forEach { point ->
        val previous = normalized.lastOrNull()
        if (point !in 1..9 || point == previous || point in normalized) return@forEach
        skippedPointBetween(previous, point)?.takeIf { it !in normalized }?.let(normalized::add)
        normalized += point
    }
    return normalized
}

internal fun isTestGesturePassword(points: List<Int>): Boolean =
    normalizeGesturePattern(points) == TEST_GESTURE_PASSWORD

private fun skippedPointBetween(from: Int?, to: Int): Int? = when (from to to) {
    1 to 3 -> 2
    1 to 7 -> 4
    1 to 9 -> 5
    2 to 8 -> 5
    3 to 1 -> 2
    3 to 7 -> 5
    3 to 9 -> 6
    4 to 6 -> 5
    6 to 4 -> 5
    7 to 1 -> 4
    7 to 3 -> 5
    7 to 9 -> 8
    8 to 2 -> 5
    9 to 1 -> 5
    9 to 3 -> 6
    9 to 7 -> 8
    else -> null
}

private val TEST_GESTURE_PASSWORD = listOf(1, 2, 3, 5, 7, 8, 9)
