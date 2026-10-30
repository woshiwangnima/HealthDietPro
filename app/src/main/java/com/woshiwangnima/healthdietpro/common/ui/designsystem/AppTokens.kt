package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 取值来源：docs/UI_SPEC_PROJECT.md；通用规范《通用UI设计规范》§2.2～§2.9、§10.4。

@Immutable
data class AppColorTokens(
    val bg: Color,
    val panel: Color,
    val panelRaised: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textMuted: Color,
    val disabled: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val danger: Color,
    val dangerFill: Color,
    val onDangerFill: Color,
    val info: Color,
    val scrim: Color,
)

/**
 * 深色主题整体为黑色：底 #000000，面板 / 浮起层只用中性灰阶拉开层级，不再带蓝色调。
 * 层级依靠「底 → 面板 → 浮起」三级明度差（§2.7-1 底材），不靠彩色底。
 * 纯黑底与通用规范 §2.3「bg 不使用纯黑」的偏离见 docs/UI_SPEC_PROJECT.md D-9。
 *
 * 主色在纯黑底上重新校准（D-10）：通用规范表 2.3-1 的 `#4C8DFF` 是按 `#0B1020` 深蓝底定的，
 * 换到中性黑后 C=0.182 / L=0.658 的饱和中蓝会在细笔画周围产生光晕，而且亮度低于 warning、info，
 * 抢不过「提示」类语义色。改为 L=0.727 / C=0.117 的浅蓝 `#7FA6F0`（黑底 8.62:1），
 * 既高于 success 的观感权重，又落在 Material 深色 primary 的 tone 80 区间。
 * 提示色改用紫色相 `#C084FC`（H 306°，与主色 H 263° 分开），避免「两个蓝」不可辨。
 * 危险色从 `#E5484D` 提到 `#FF6B6B`：卡片底换成 `panelRaised` `#1A1A1A` 后，原值作正文色只有 4.45:1，
 * 差一点不过 AA（如分级表的「高」标签）；提亮后 6.27:1，且亮度与 primary / success 同档，不再比它们暗一头。
 */
val DarkAppColors = AppColorTokens(
    bg = Color(0xFF000000),
    panel = Color(0xFF0D0D0D),
    panelRaised = Color(0xFF1A1A1A),
    border = Color(0xFF2B2B2B),
    borderStrong = Color(0xFF616161),
    text = Color(0xFFEDEDED),
    textMuted = Color(0xFFA0A0A0),
    disabled = Color(0xFF5A5A5A),
    primary = Color(0xFF7FA6F0),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1B2C4A),
    onPrimaryContainer = Color(0xFFCADCFF),
    success = Color(0xFF3FBF6F),
    onSuccess = Color(0xFF000000),
    warning = Color(0xFFF2B23E),
    onWarning = Color(0xFF000000),
    danger = Color(0xFFFF6B6B),
    dangerFill = Color(0xFFC62828),
    onDangerFill = Color(0xFFFFFFFF),
    info = Color(0xFFC084FC),
    scrim = Color(0x99000000),
)

val LightAppColors = AppColorTokens(
    bg = Color(0xFFF4F6FB),
    panel = Color(0xFFFFFFFF),
    panelRaised = Color(0xFFFFFFFF),
    border = Color(0xFFD3DAE8),
    borderStrong = Color(0xFF7A8398),
    text = Color(0xFF0F1729),
    textMuted = Color(0xFF56607A),
    disabled = Color(0xFFA8B0C4),
    primary = Color(0xFF1F5ED0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E4FF),
    onPrimaryContainer = Color(0xFF0B2A66),
    success = Color(0xFF1B7A45),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFF8A5A00),
    onWarning = Color(0xFFFFFFFF),
    danger = Color(0xFFB3261E),
    dangerFill = Color(0xFFB3261E),
    onDangerFill = Color(0xFFFFFFFF),
    // 与深色同一套「蓝 = 操作 / 紫 = 提示」分工：原 #0A6EA8 与主色亮度几乎相同（ΔL 0.004），
    // 浅色下会被读成同一个蓝。改紫后与主色 ΔE 14.7，白底 7.02:1、页面底 6.49:1。
    info = Color(0xFF7B2FBE),
    scrim = Color(0x99000000),
)

@Immutable
data class AppFoodKindColors(
    val ingredient: Color,
    val preparedFood: Color,
    val dish: Color,
    val onKind: Color,
)

val AppFoodKindCurrent = AppFoodKindColors(
    ingredient = Color(0xFF43A047),
    preparedFood = Color(0xFFF57C00),
    dish = Color(0xFFE53935),
    onKind = Color(0xFFFFFFFF),
)

val AppFoodKindAccessible = AppFoodKindColors(
    ingredient = Color(0xFF2E7D32),
    preparedFood = Color(0xFFBF360C),
    dish = Color(0xFFC62828),
    onKind = Color(0xFFFFFFFF),
)

/**
 * 图表分类配色。每个色值都针对所在主题的面板底色校准到非文本对比度 ≥ 3:1（通用规范 §11.1、§9.4），
 * 且不能只靠颜色区分——图例必须同时给文字标签。
 */
@Immutable
data class AppChartColors(
    val carbs: Color,
    val protein: Color,
    val fat: Color,
    val fiber: Color,
    val alcohol: Color,
    val organicAcid: Color,
    val energy: Color,
    val other: Color,
) {
    fun forNutrient(nutrientId: String): Color = when (nutrientId.uppercase()) {
        "ENERGY" -> energy
        "CHO" -> carbs
        "PROTEIN" -> protein
        "FAT" -> fat
        "FIBER" -> fiber
        "ALCOHOL" -> alcohol
        "ORGANIC_ACID" -> organicAcid
        else -> other
    }
}

/** 深色面板 #0D0D0D 上实测：9.86 / 5.88 / 4.60 / 4.50 / 5.87 / 8.13 / 9.32 / 6.30，均 ≥ 3:1。 */
val DarkAppChartColors = AppChartColors(
    carbs = Color(0xFFF9A825),
    protein = Color(0xFF43A047),
    fat = Color(0xFFE53935),
    fiber = Color(0xFF00897B),
    alcohol = Color(0xFFA1887F),
    organicAcid = Color(0xFFCE93D8),
    energy = Color(0xFFE0A96D),
    other = Color(0xFF8A93A6),
)

/** 浅色面板 #FFFFFF 上实测：4.48 / 5.13 / 5.62 / 6.61 / 7.61 / 9.39 / 6.99 / 6.27。 */
val LightAppChartColors = AppChartColors(
    carbs = Color(0xFFA66A00),
    protein = Color(0xFF2E7D32),
    fat = Color(0xFFC62828),
    fiber = Color(0xFF00695C),
    alcohol = Color(0xFF5D4037),
    organicAcid = Color(0xFF6A1B9A),
    energy = Color(0xFF8A5A00),
    other = Color(0xFF56607A),
)

@Immutable
data class AppSpacing(
    val s1: Dp,
    val s2: Dp,
    val s3: Dp,
    val s4: Dp,
    val s5: Dp,
    val s6: Dp,
    val s7: Dp,
    val s8: Dp,
    val page: Dp,
    val rowMinHeight: Dp,
    val hitMin: Dp,
)

val AppSpacingDefault = AppSpacing(
    s1 = 4.dp,
    s2 = 8.dp,
    s3 = 12.dp,
    s4 = 16.dp,
    s5 = 24.dp,
    s6 = 32.dp,
    s7 = 48.dp,
    s8 = 64.dp,
    page = 16.dp,
    rowMinHeight = 56.dp,
    hitMin = 48.dp,
)

@Immutable
data class AppRadii(
    val container: Dp,
    val label: Dp,
    val sheet: Dp,
    val pill: Dp,
    val stroke: Dp,
    val highlight: Dp,
)

val AppRadiiDefault = AppRadii(
    container = 4.dp,
    label = 8.dp,
    // 底部抽屉是独立浮层，顶角比卡片大一档，才能和「底 → 面板 → 浮起」的层级一起被读出来。
    sheet = 12.dp,
    pill = 999.dp,
    stroke = 2.dp,
    highlight = 1.dp,
)

/**
 * 同心圆角：`R_outer = R_inner + Padding`（移动规范 07 检查项 11）。
 * 内嵌容器的圆角必须比父级小一个内边距；父级圆角本来就小于内边距时（例如 4dp 卡片 + 12dp 内边距），
 * 内层取 0，否则两层弧线在四角会各画各的、出现「内圆比外圆还弯」的错位。
 */
fun concentricRadius(outer: Dp, inset: Dp): Dp = (outer - inset).coerceAtLeast(0.dp)

@Immutable
data class AppTypeScale(
    val caption: TextStyle,
    val bodyS: TextStyle,
    val body: TextStyle,
    val numeric: TextStyle,
    val titleS: TextStyle,
    val title: TextStyle,
    val titleL: TextStyle,
    val display: TextStyle,
)

val AppTypeDefault = AppTypeScale(
    caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    bodyS = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    numeric = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontFamily = FontFamily.Monospace),
    titleS = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    title = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    titleL = TextStyle(fontSize = 32.sp, lineHeight = 40.sp),
    display = TextStyle(fontSize = 40.sp, lineHeight = 48.sp),
)

@Immutable
data class AppMotion(
    val instantMs: Int,
    val fastMs: Int,
    val exitMs: Int,
    val baseMs: Int,
    val sheetMs: Int,
    val slowMs: Int,
    val emphasisMs: Int,
    val cinematicMs: Int,
)

val AppMotionDefault = AppMotion(
    instantMs = 80,
    fastMs = 120,
    exitMs = 150,
    baseMs = 180,
    sheetMs = 220,
    slowMs = 280,
    emphasisMs = 400,
    cinematicMs = 800,
)

val AppEaseLinear: Easing = LinearEasing
val AppEaseOut: Easing = LinearOutSlowInEasing
val AppEaseIn: Easing = FastOutLinearInEasing
val AppEaseInOut: Easing = FastOutSlowInEasing

const val APP_Z_SCREEN = 100f
const val APP_Z_HUD = 200f
const val APP_Z_PANEL = 300f
const val APP_Z_OVERLAY = 400f
const val APP_Z_TOAST = 500f

@Immutable
data class AppTokens(
    val colors: AppColorTokens,
    val spacing: AppSpacing,
    val radii: AppRadii,
    val type: AppTypeScale,
    val motion: AppMotion,
    val foodKind: AppFoodKindColors,
    val chart: AppChartColors,
)

val DarkAppTokens = AppTokens(
    colors = DarkAppColors,
    spacing = AppSpacingDefault,
    radii = AppRadiiDefault,
    type = AppTypeDefault,
    motion = AppMotionDefault,
    foodKind = AppFoodKindCurrent,
    chart = DarkAppChartColors,
)

val LightAppTokens = AppTokens(
    colors = LightAppColors,
    spacing = AppSpacingDefault,
    radii = AppRadiiDefault,
    type = AppTypeDefault,
    motion = AppMotionDefault,
    foodKind = AppFoodKindCurrent,
    chart = LightAppChartColors,
)

val LocalAppTokens = staticCompositionLocalOf { DarkAppTokens }

fun AppColorTokens.toMaterialColorScheme(dark: Boolean): ColorScheme {
    val scheme = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = primary,
            secondary = textMuted,
            onSecondary = bg,
            secondaryContainer = panelRaised,
            onSecondaryContainer = text,
            tertiary = info,
            onTertiary = bg,
            tertiaryContainer = panelRaised,
            onTertiaryContainer = info,
            background = bg,
            onBackground = text,
            surface = panel,
            onSurface = text,
            surfaceVariant = panelRaised,
            onSurfaceVariant = textMuted,
            inverseSurface = text,
            inverseOnSurface = bg,
            error = danger,
            onError = onDangerFill,
            errorContainer = dangerFill,
            onErrorContainer = onDangerFill,
            outline = borderStrong,
            outlineVariant = border,
            scrim = scrim,
            surfaceBright = panelRaised,
            surfaceDim = bg,
            surfaceContainer = panel,
            surfaceContainerHigh = panelRaised,
            surfaceContainerHighest = panelRaised,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = primary,
            secondary = textMuted,
            onSecondary = panel,
            secondaryContainer = primaryContainer,
            onSecondaryContainer = onPrimaryContainer,
            tertiary = info,
            onTertiary = panel,
            tertiaryContainer = primaryContainer,
            onTertiaryContainer = onPrimaryContainer,
            background = bg,
            onBackground = text,
            surface = panel,
            onSurface = text,
            surfaceVariant = bg,
            onSurfaceVariant = textMuted,
            inverseSurface = text,
            inverseOnSurface = panel,
            error = danger,
            onError = onDangerFill,
            errorContainer = dangerFill,
            onErrorContainer = onDangerFill,
            outline = borderStrong,
            outlineVariant = border,
            scrim = scrim,
            surfaceBright = panel,
            surfaceDim = bg,
            surfaceContainer = panel,
            surfaceContainerHigh = panel,
            surfaceContainerHighest = panel,
        )
    }
    return scheme
}

fun AppTypeScale.toMaterialTypography(): Typography = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = titleL,
    headlineLarge = titleL,
    headlineMedium = titleL,
    headlineSmall = title,
    titleLarge = title,
    titleMedium = titleS,
    titleSmall = titleS,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = bodyS,
    labelLarge = bodyS,
    labelMedium = bodyS,
    labelSmall = caption,
)

fun contrastRatioOf(foreground: Color, background: Color): Double =
    contrastRatio(foreground.toArgb(), background.toArgb())
