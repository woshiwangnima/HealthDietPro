package com.woshiwangnima.healthdietpro.common.ui.chart

import kotlin.math.abs

internal sealed interface ChartValueExpression {
    data class Source(val seriesId: String) : ChartValueExpression
    data class Constant(val value: Float) : ChartValueExpression
    data class Add(
        val left: ChartValueExpression,
        val right: ChartValueExpression,
    ) : ChartValueExpression

    data class Subtract(
        val left: ChartValueExpression,
        val right: ChartValueExpression,
    ) : ChartValueExpression

    data class Multiply(
        val left: ChartValueExpression,
        val right: ChartValueExpression,
    ) : ChartValueExpression

    data class Divide(
        val numerator: ChartValueExpression,
        val denominator: ChartValueExpression,
    ) : ChartValueExpression

    data class Percent(
        val part: ChartValueExpression,
        val total: ChartValueExpression,
    ) : ChartValueExpression
}

internal fun evaluateChartValueExpression(
    expression: ChartValueExpression,
    values: Map<String, Float>,
): Float? {
    val result = when (expression) {
        is ChartValueExpression.Source -> values[expression.seriesId]
        is ChartValueExpression.Constant -> expression.value
        is ChartValueExpression.Add -> {
            val left = evaluateChartValueExpression(expression.left, values)
            val right = evaluateChartValueExpression(expression.right, values)
            if (left == null || right == null) null else left + right
        }
        is ChartValueExpression.Subtract -> {
            val left = evaluateChartValueExpression(expression.left, values)
            val right = evaluateChartValueExpression(expression.right, values)
            if (left == null || right == null) null else left - right
        }
        is ChartValueExpression.Multiply -> {
            val left = evaluateChartValueExpression(expression.left, values)
            val right = evaluateChartValueExpression(expression.right, values)
            if (left == null || right == null) null else left * right
        }
        is ChartValueExpression.Divide -> {
            val numerator = evaluateChartValueExpression(expression.numerator, values)
            val denominator = evaluateChartValueExpression(expression.denominator, values)
            if (numerator == null || denominator == null || abs(denominator) < ZERO_EPSILON) {
                null
            } else {
                numerator / denominator
            }
        }
        is ChartValueExpression.Percent -> {
            val part = evaluateChartValueExpression(expression.part, values)
            val total = evaluateChartValueExpression(expression.total, values)
            if (part == null || total == null || abs(total) < ZERO_EPSILON) {
                null
            } else {
                part / total * 100f
            }
        }
    }
    return result?.takeIf { it.isFinite() }
}

private const val ZERO_EPSILON = 0.000001f
