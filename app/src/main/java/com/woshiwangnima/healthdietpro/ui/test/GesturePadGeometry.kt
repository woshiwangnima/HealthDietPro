package com.woshiwangnima.healthdietpro.ui.test

internal data class GestureCoordinate(val x: Float, val y: Float)

internal fun gesturePointAt(
    coordinate: GestureCoordinate,
    size: Float,
    hitRadiusRatio: Float = 0.42f,
): Int? {
    val spacing = size / 4f
    return (1..9).minByOrNull { point -> coordinate.distanceTo(gesturePointCenter(point, spacing)) }
        ?.takeIf { point -> coordinate.distanceTo(gesturePointCenter(point, spacing)) <= spacing * hitRadiusRatio }
}

internal fun gesturePointsCrossed(
    from: GestureCoordinate,
    to: GestureCoordinate,
    size: Float,
    excluded: Set<Int>,
    hitRadiusRatio: Float = 0.42f,
): List<Int> {
    val spacing = size / 4f
    return (1..9)
        .asSequence()
        .filterNot(excluded::contains)
        .map { point -> point to segmentProjection(from, to, gesturePointCenter(point, spacing)) }
        .filter { (_, projection) -> projection.distance <= spacing * hitRadiusRatio }
        .sortedBy { (_, projection) -> projection.position }
        .map { (point, _) -> point }
        .toList()
}

private fun gesturePointCenter(point: Int, spacing: Float): GestureCoordinate = GestureCoordinate(
    x = spacing * ((point - 1) % 3 + 1),
    y = spacing * ((point - 1) / 3 + 1),
)

private fun GestureCoordinate.distanceTo(other: GestureCoordinate): Float {
    val dx = x - other.x
    val dy = y - other.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun segmentProjection(
    start: GestureCoordinate,
    end: GestureCoordinate,
    point: GestureCoordinate,
): SegmentProjection {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0f) return SegmentProjection(0f, point.distanceTo(start))
    val position = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    val nearest = GestureCoordinate(start.x + dx * position, start.y + dy * position)
    return SegmentProjection(position, point.distanceTo(nearest))
}

private data class SegmentProjection(val position: Float, val distance: Float)
