package com.woshiwangnima.healthdietpro.common.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.woshiwangnima.healthdietpro.util.SystemNavigationMode
import com.woshiwangnima.healthdietpro.util.needsNavigationBarInset
import com.woshiwangnima.healthdietpro.util.systemNavigationMode
import kotlin.math.roundToInt

@Composable
internal fun adaptiveNavigationBarWindowInsets(): WindowInsets {
    val context = LocalContext.current
    val needsInset = remember(context) {
        context.systemNavigationMode().needsNavigationBarInset()
    }
    return if (needsInset) {
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    } else {
        WindowInsets(0, 0, 0, 0)
    }
}

/**
 * 从任意 `Context` 往上找到宿主 `Activity`。
 *
 * Dialog 里的 `LocalContext` 是包在 Activity 外面的 `ContextThemeWrapper`，剥壳才能拿到宿主窗口。
 */
private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

/**
 * 宿主 Activity 窗口的某条系统栏高度（px），拿不到返回 0。
 *
 * **为什么绕到宿主窗口**：浮层都跑在 `Dialog` 里，Dialog 自己的窗口在这台设备上
 * `navigationBars` 取出来恒为 0（实测 `dialogViewBottomPx=0`），宿主窗口才是真实值来源。
 */
private fun Context.hostInsetPx(type: Int, fromTop: Boolean): Int {
    val decorView = findHostActivity()?.window?.decorView ?: return 0
    val insets = ViewCompat.getRootWindowInsets(decorView)?.getInsets(type) ?: return 0
    return if (fromTop) insets.top else insets.bottom
}

private fun Context.dpToPx(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

private fun Context.systemResourcePx(name: String): Int {
    val id = resources.getIdentifier(name, "dimen", "android")
    if (id == 0) return 0
    return runCatching { resources.getDimensionPixelSize(id) }.getOrDefault(0)
}

/**
 * 浮层（Dialog 承载的底部抽屉、弹窗）底部要让出的系统导航条高度，单位 px。
 *
 * 与页面用的 [adaptiveNavigationBarWindowInsets] 有意不同：
 * - 页面内容可以延伸到手势条下面（全站约定：手势导航留 0），因为页面底部通常只是留白；
 * - 浮层底部是**可交互内容与面板边界**，三键导航条和手势条都会压住它，两种导航方式都要让位。
 *
 * **取值顺序**：
 * 1. 宿主 Activity 窗口的 `navigationBars.bottom` —— 唯一准的一档；
 * 2. 按导航模式的常量（三键/两键 48 dp、手势 24 dp）；
 * 3. 系统资源 `navigation_bar_height`；
 * 4. 24 dp。
 *
 * 顺序是踩出来的（2026-10-30）：把资源排在常量前面时，模拟器上真的把抽屉沉进了导航栏。
 * 原因是两个坑叠在一起：
 * - **资源不可信**：`navigation_bar_height` 在某些系统上等于「手势条高度」而不是导航栏高度
 *   （模拟器实测 84 px = 24 dp，同机真实 insets 是 168 px = 48 dp），拿它当主值会少让 24 dp；
 * - **首帧读不到宿主 insets**：Dialog 刚创建时这里读出来是 0，只能落到后面几档。
 */
internal fun Context.overlayNavigationBarHeightPx(): Int {
    val hostPx = hostInsetPx(WindowInsetsCompat.Type.navigationBars(), fromTop = false)
    if (hostPx > 0) return hostPx
    return when (systemNavigationMode()) {
        SystemNavigationMode.ThreeButton, SystemNavigationMode.TwoButton -> dpToPx(48)
        SystemNavigationMode.Gestural -> dpToPx(24)
        SystemNavigationMode.Unknown -> {
            val resPx = systemResourcePx("navigation_bar_height")
            if (resPx > 0) resPx else dpToPx(24)
        }
    }
}

/** 浮层顶部要让出的状态栏高度，单位 px。取值顺序同 [overlayNavigationBarHeightPx]。 */
internal fun Context.overlayStatusBarHeightPx(): Int {
    val hostPx = hostInsetPx(WindowInsetsCompat.Type.statusBars(), fromTop = true)
    if (hostPx > 0) return hostPx
    val resPx = systemResourcePx("status_bar_height")
    return if (resPx > 0) resPx else dpToPx(24)
}

/**
 * 浮层底部要避让的导航条高度，单位 **px**，随窗口稳定自动重读。
 *
 * **为什么要状态化**：`ViewCompat.getRootWindowInsets` 不是 Compose 状态，读到 0 之后
 * 值再变成真实高度也不会触发重组——抽屉就会一直停在「资源兜底值」上（实测 24 dp 而非 48 dp，
 * 面板下沿因此陷进系统导航栏）。这里把读到的 px 放进 `mutableIntStateOf`，窗口稳定后主动重读，
 * 值变化必然触发重测，面板才会真的抬起来。
 *
 * **为什么对外返回 px 而不是 dp**：浮层跑在 `Dialog` 窗口里，实测它的密度和宿主 Activity 不同
 * （3.5 vs 2.5）。px 是在宿主窗口量出来的绝对值，若先按宿主密度折成 dp、再在 Dialog 里还原，
 * 会让位距离缩水到原来的 60%（48 dp 只抬起 86 px），面板照样沉进导航栏。
 * 需要 dp 的调用方请在**浮层自己的 composition** 里用 `with(LocalDensity.current) { px.toDp() }`。
 *
 * 更新只增不减：偶发读到 0（动画/窗口切换中途）不能把面板又拽回屏幕底。
 */
@Composable
internal fun overlayNavigationBarHeightPxState(): Int {
    val context = LocalContext.current
    var heightPx by remember { mutableIntStateOf(context.overlayNavigationBarHeightPx()) }
    LaunchedEffect(context) {
        // 用 delay 而不是 withFrameNanos：浮层窗口刚建立时帧时钟可能还没开始分发，
        // withFrameNanos 会一直挂着不返回，兜底值就永远停在首帧那一次。
        delay(50)
        val latest = context.overlayNavigationBarHeightPx()
        if (latest > heightPx) heightPx = latest
        delay(200)
        val settled = context.overlayNavigationBarHeightPx()
        if (settled > heightPx) heightPx = settled
    }
    return heightPx
}

/** 浮层顶部要避让的状态栏高度，单位 **px**，语义同 [overlayNavigationBarHeightPxState]。 */
@Composable
internal fun overlayStatusBarHeightPxState(): Int {
    val context = LocalContext.current
    var heightPx by remember { mutableIntStateOf(context.overlayStatusBarHeightPx()) }
    LaunchedEffect(context) {
        delay(50)
        val latest = context.overlayStatusBarHeightPx()
        if (latest > heightPx) heightPx = latest
        delay(200)
        val settled = context.overlayStatusBarHeightPx()
        if (settled > heightPx) heightPx = settled
    }
    return heightPx
}

/**
 * 浮层底部内边距（dp 形态）。
 *
 * 只有在**浮层自己的 composition**里调用才安全（dp 会按当前密度还原成同一批像素）；
 * 在宿主里取出来再塞进 Dialog 用，会因为密度不同而缩水，详见 [overlayNavigationBarHeightPxState]。
 */
@Composable
internal fun overlayNavigationBarPadding(): Dp {
    val density = LocalDensity.current
    return with(density) { overlayNavigationBarHeightPxState().toDp() }
}

/** 浮层顶部内边距（dp 形态）。密度注意事项同 [overlayNavigationBarPadding]。 */
@Composable
internal fun overlayStatusBarPadding(): Dp {
    val density = LocalDensity.current
    return with(density) { overlayStatusBarHeightPxState().toDp() }
}

/**
 * 浮层窗口与「屏幕安全区」的换算结果，单位 px。
 *
 * @param usableHeightPx 面板最多能占的高度（已扣掉状态栏与导航条）
 * @param centerShiftPx 让「窗口中心」对上「安全区中心」需要的纵向位移（居中弹窗用）
 * @param bottomLiftPx 把贴底面板抬到安全区下沿需要的位移（底部抽屉用）；
 *   窗口本身已被系统裁进安全区时是 0，全屏窗口时等于导航条高度
 */
internal data class OverlayWindowFit(
    val usableHeightPx: Int,
    val centerShiftPx: Int,
    val bottomLiftPx: Int,
)

/**
 * 算出 [OverlayWindowFit]，随窗口稳定自动重读。
 *
 * **为什么不能直接加 inset**：Compose `Dialog` 的窗口在宿主之间不一致——同一台设备上实测
 * `DietRecordActivity` 的浮层窗口被系统裁成 `frame=[0,84][1439,3032]`（已避开状态栏与导航条），
 * 但 Compose 拿到的约束高度仍是整屏 3200，内容原点又在屏幕 y=84。
 * 于是按 inset 再内缩一次就是「让位两次」：面板整体下沉一个状态栏的高度（实测上留白 168 px、下留白 2 px）。
 *
 * 这里统一换算到**屏幕坐标**：用窗口在屏幕上的原点与安全区边界，算出居中位移与可用高度，
 * 全屏窗口、已裁进安全区的窗口都落在正确位置上。
 */
@Composable
internal fun rememberOverlayWindowFit(fallbackWindowHeightPx: Int): OverlayWindowFit {
    val view = LocalView.current
    val context = LocalContext.current
    var fit by remember(view, fallbackWindowHeightPx) {
        mutableStateOf(computeOverlayWindowFit(view, context, fallbackWindowHeightPx))
    }
    // 窗口位置要等第一帧布局完才准，且它不是 Compose 状态：用 delay 主动重读几次。
    LaunchedEffect(view, fallbackWindowHeightPx) {
        delay(50)
        fit = computeOverlayWindowFit(view, context, fallbackWindowHeightPx)
        delay(250)
        val settled = computeOverlayWindowFit(view, context, fallbackWindowHeightPx)
        if (settled != fit) fit = settled
    }
    return fit
}

private fun computeOverlayWindowFit(
    view: android.view.View,
    context: Context,
    fallbackWindowHeightPx: Int,
): OverlayWindowFit {
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    val windowTop = location[1]
    val windowHeight = if (fallbackWindowHeightPx > 0) fallbackWindowHeightPx else view.height
    if (windowHeight <= 0) return OverlayWindowFit(0, 0, 0)

    val screenHeightPx = context.resources.displayMetrics.heightPixels
    val statusPx = context.overlayStatusBarHeightPx()
    val navPx = context.overlayNavigationBarHeightPx()
    val safeHeightPx = screenHeightPx - statusPx - navPx
    // 窗口比安全区还小（系统已经裁过）时以窗口为准；否则以安全区为准，避免面板压到系统栏。
    val usableHeightPx = minOf(windowHeight, safeHeightPx)
    val safeCenter = statusPx + safeHeightPx / 2f
    val windowCenter = windowTop + windowHeight / 2f
    // 安全区下沿在窗口坐标系里的位置：窗口比内容矮时它小于 windowHeight，贴底面板要抬这么多。
    val usableBottomInWindow = (screenHeightPx - windowTop - navPx)
    return OverlayWindowFit(
        usableHeightPx = usableHeightPx,
        centerShiftPx = (safeCenter - windowCenter).roundToInt(),
        bottomLiftPx = (windowHeight - usableBottomInWindow).coerceAtLeast(0),
    )
}

@Composable
internal fun adaptiveNavigationBarsWindowInsets(): WindowInsets {
    return WindowInsets.navigationBars
}
