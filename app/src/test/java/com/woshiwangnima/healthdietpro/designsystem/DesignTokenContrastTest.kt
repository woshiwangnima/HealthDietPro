package com.woshiwangnima.healthdietpro.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFoodKindAccessible
import com.woshiwangnima.healthdietpro.common.ui.designsystem.DarkAppChartColors
import com.woshiwangnima.healthdietpro.common.ui.designsystem.DarkAppColors
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LightAppChartColors
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LightAppColors
import com.woshiwangnima.healthdietpro.common.ui.designsystem.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * 依据 docs/UI_SPEC_PROJECT.md 与《通用UI设计规范》§11.1：正文对比度 ≥ 4.5:1，
 * 大字号 ≥ 3:1，图标/描边 ≥ 3:1。
 */
class DesignTokenContrastTest {

    private fun ratio(foreground: Color, background: Color): Double =
        contrastRatio(foreground.toArgb(), background.toArgb())

    private fun assertAtLeast(
        label: String,
        foreground: Color,
        background: Color,
        minimum: Double,
    ) {
        val actual = ratio(foreground, background)
        assertTrue(
            "$label 对比度 ${"%.2f".format(actual)}:1 低于要求 ${"%.1f".format(minimum)}:1",
            actual >= minimum,
        )
    }

    @Test
    fun darkTextPairsMeetWcag() {
        val colors = DarkAppColors
        assertAtLeast("text/bg", colors.text, colors.bg, 7.0)
        assertAtLeast("text/panel", colors.text, colors.panel, 7.0)
        assertAtLeast("text/panelRaised", colors.text, colors.panelRaised, 7.0)
        assertAtLeast("textMuted/bg", colors.textMuted, colors.bg, 4.5)
        assertAtLeast("textMuted/panel", colors.textMuted, colors.panel, 4.5)
    }

    @Test
    fun darkSemanticPairsMeetWcag() {
        val colors = DarkAppColors
        listOf(
            "primary" to colors.primary,
            "success" to colors.success,
            "warning" to colors.warning,
            "danger" to colors.danger,
            "info" to colors.info,
        ).forEach { (name, color) ->
            assertAtLeast("$name/bg", color, colors.bg, 4.5)
        }
        assertAtLeast("onPrimary/primary", colors.onPrimary, colors.primary, 4.5)
        assertAtLeast("onDangerFill/dangerFill", colors.onDangerFill, colors.dangerFill, 4.5)
        assertAtLeast("borderStrong/bg", colors.borderStrong, colors.bg, 3.0)
    }

    @Test
    fun lightTextPairsMeetWcag() {
        val colors = LightAppColors
        assertAtLeast("text/bg", colors.text, colors.bg, 7.0)
        assertAtLeast("text/panel", colors.text, colors.panel, 7.0)
        assertAtLeast("textMuted/bg", colors.textMuted, colors.bg, 4.5)
        assertAtLeast("textMuted/panel", colors.textMuted, colors.panel, 4.5)
    }

    @Test
    fun lightSemanticPairsMeetWcag() {
        val colors = LightAppColors
        listOf(
            "primary" to colors.primary,
            "success" to colors.success,
            "warning" to colors.warning,
            "danger" to colors.danger,
            "info" to colors.info,
        ).forEach { (name, color) ->
            assertAtLeast("$name/bg", color, colors.bg, 4.5)
        }
        assertAtLeast("onPrimary/primary", colors.onPrimary, colors.primary, 4.5)
        assertAtLeast("onDangerFill/dangerFill", colors.onDangerFill, colors.dangerFill, 4.5)
        assertAtLeast("borderStrong/bg", colors.borderStrong, colors.bg, 3.0)
    }

    @Test
    fun suggestedFoodKindFillsMeetWcagWithWhiteText() {
        val kinds = AppFoodKindAccessible
        assertAtLeast("ingredient/onKind", kinds.onKind, kinds.ingredient, 4.5)
        assertAtLeast("preparedFood/onKind", kinds.onKind, kinds.preparedFood, 4.5)
        assertAtLeast("dish/onKind", kinds.onKind, kinds.dish, 4.5)
    }

    @Test
    fun chartPalettesMeetNonTextContrastOnPanel() {
        val dark = DarkAppChartColors
        listOf(
            "carbs" to dark.carbs,
            "protein" to dark.protein,
            "fat" to dark.fat,
            "fiber" to dark.fiber,
            "alcohol" to dark.alcohol,
            "organicAcid" to dark.organicAcid,
            "energy" to dark.energy,
            "other" to dark.other,
        ).forEach { (name, color) ->
            assertAtLeast("dark.chart.$name/panel", color, DarkAppColors.panel, 3.0)
        }
        val light = LightAppChartColors
        listOf(
            "carbs" to light.carbs,
            "protein" to light.protein,
            "fat" to light.fat,
            "fiber" to light.fiber,
            "alcohol" to light.alcohol,
            "organicAcid" to light.organicAcid,
            "energy" to light.energy,
            "other" to light.other,
        ).forEach { (name, color) ->
            assertAtLeast("light.chart.$name/panel", color, LightAppColors.panel, 3.0)
        }
    }

    @Test
    fun chartColorsMapByNutrientId() {
        val chart = DarkAppChartColors
        assertTrue(chart.forNutrient("CHO") == chart.carbs)
        assertTrue(chart.forNutrient("protein") == chart.protein)
        assertTrue(chart.forNutrient("FAT") == chart.fat)
        assertTrue(chart.forNutrient("FIBER") == chart.fiber)
        assertTrue(chart.forNutrient("ALCOHOL") == chart.alcohol)
        assertTrue(chart.forNutrient("ORGANIC_ACID") == chart.organicAcid)
        assertTrue(chart.forNutrient("ENERGY") == chart.energy)
        assertTrue(chart.forNutrient("UNKNOWN") == chart.other)
    }

    @Test
    fun contrastRatioIsSymmetricAndBounded() {
        val black = Color(0xFF000000).toArgb()
        val white = Color(0xFFFFFFFF).toArgb()
        assertTrue(contrastRatio(black, white) >= 20.9)
        assertTrue(contrastRatio(white, black) >= 20.9)
        assertTrue(contrastRatio(white, white) == 1.0)
    }

    /**
     * D-10 回归：主色与提示色必须能一眼分开，且深色下提示色不得比主操作更亮。
     * 旧值 `#4C8DFF / #38BDF8`（深色）与 `#1F5ED0 / #0A6EA8`（浅色）分别只有 ΔE 13.0 / 8.1，
     * 浅色两侧亮度差 0.004，实际界面里会被读成同一个蓝。
     */
    @Test
    fun primaryAndInfoStayDistinguishable() {
        listOf("dark" to DarkAppColors, "light" to LightAppColors).forEach { (theme, colors) ->
            val distance = oklabDistance(colors.primary, colors.info)
            assertTrue(
                "$theme primary/info 色差 ${"%.1f".format(distance)} 过小，需 ≥ 10（JND≈2）",
                distance >= 10.0,
            )
        }
        // 深色下「提示」不应比「主操作」更亮，否则语义层级倒挂。
        val dark = DarkAppColors
        assertTrue(
            "dark info 亮度高于 primary，语义层级倒挂",
            ratio(dark.info, dark.bg) < ratio(dark.primary, dark.bg),
        )
    }

    /**
     * §8.28.1 回归：不在 `Surface` / `Button` / `TextField` 里的 `Text` 会取 `LocalContentColor`，
     * 而 Material3 的 `MaterialTheme` 不提供它——默认值是 `Color.Black`。深色底上就是黑底黑字。
     * `AppTheme` 现在把它兜成 `tokens.colors.text`，这里守住「兜底色绝不能是纯黑」，
     * 防止有人日后把 `AppTheme` 的那层 `CompositionLocalProvider` 删掉而毫无察觉。
     */
    @Test
    fun fallbackContentColorIsNeverPureBlack() {
        listOf("dark" to DarkAppColors, "light" to LightAppColors).forEach { (theme, colors) ->
            assertTrue(
                "$theme 兜底内容色是纯黑，深色下会读不出来",
                colors.text != Color.Black,
            )
            assertAtLeast("$theme text/bg", colors.text, colors.bg, 7.0)
            assertAtLeast("$theme text/panel", colors.text, colors.panel, 7.0)
        }
    }

    private fun linear(channel: Float): Float =
        if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

    /** OKLab 欧氏距离 ×100，JND 约为 2。 */
    private fun oklabDistance(first: Color, second: Color): Double {
        fun toOklab(color: Color): DoubleArray {
            val r = linear(color.red).toDouble()
            val g = linear(color.green).toDouble()
            val b = linear(color.blue).toDouble()
            val l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
            val m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
            val s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
            val lc = Math.cbrt(l)
            val mc = Math.cbrt(m)
            val sc = Math.cbrt(s)
            return doubleArrayOf(
                0.2104542553 * lc + 0.7936177850 * mc - 0.0040720468 * sc,
                1.9779984951 * lc - 2.4285922050 * mc + 0.4505937099 * sc,
                0.0259040371 * lc + 0.7827717662 * mc - 0.8086757660 * sc,
            )
        }
        val a = toOklab(first)
        val b = toOklab(second)
        return Math.sqrt(
            (a[0] - b[0]) * (a[0] - b[0]) +
                (a[1] - b[1]) * (a[1] - b[1]) +
                (a[2] - b[2]) * (a[2] - b[2]),
        ) * 100.0
    }
}
