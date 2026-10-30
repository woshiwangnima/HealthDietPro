package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * 浮层（Dialog 承载的底部抽屉、弹窗）底部要让出的系统导航条高度。
 *
 * 与页面用的 [adaptiveNavigationBarWindowInsets] 有意不同：
 * - 页面内容可以延伸到手势条下面（全站约定：手势导航留 0），因为页面底部通常只是留白；
 * - 浮层底部是**可交互内容与面板边界**，三键导航条和手势条都会压住它，所以两种导航方式都要让位。
 *   三键 ≈48 dp、手势 ≈24 dp。
 *
 * 取值顺序（前三档都拿不到时按导航方式给常量，绝不为 0）：
 * 1. `ViewCompat.getRootWindowInsets` —— **Dialog 窗口里唯一可靠的通道**。实测该机型三键导航
 *    `navigationBars.bottom = 177 px @600 dpi = 47.2 dp`；
 * 2. Compose `WindowInsets.navigationBars` —— 同一窗口的次要通道；
 * 3. 系统资源 `navigation_bar_height`；
 * 4. 按 `systemNavigationMode()` 取常量。
 *
 * 注意：**不要 `remember` 最终结果**。组合可能早于窗口下发 insets，缓存会把 0 永久固化。
 */
@Composable
internal fun overlayNavigationBarPadding(): Dp {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    val windowInsetPx = ViewCompat.getRootWindowInsets(view)
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())
        ?.bottom ?: 0
    if (windowInsetPx > 0) return with(density) { windowInsetPx.toDp() }

    val composeInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    if (composeInset > 0.dp) return composeInset

    val resourceInsetPx = remember(context) {
        val id = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id != 0) context.resources.getDimensionPixelSize(id) else 0
    }
    if (resourceInsetPx > 0) return with(density) { resourceInsetPx.toDp() }

    return when (context.systemNavigationMode()) {
        SystemNavigationMode.ThreeButton, SystemNavigationMode.TwoButton -> 48.dp
        SystemNavigationMode.Gestural -> 24.dp
        SystemNavigationMode.Unknown -> 24.dp
    }
}

@Composable
internal fun adaptiveNavigationBarsWindowInsets(): WindowInsets {
    return WindowInsets.navigationBars
}
