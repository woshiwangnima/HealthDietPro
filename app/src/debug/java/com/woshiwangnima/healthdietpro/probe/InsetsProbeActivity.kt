package com.woshiwangnima.healthdietpro.probe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppModal
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSheetHeight
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.overlayNavigationBarPadding
import com.woshiwangnima.healthdietpro.common.ui.overlayStatusBarPadding
import com.woshiwangnima.healthdietpro.util.systemNavigationMode

/**
 * 仅 debug：浮层安全区探针。
 *
 * 复刻「选容器抽屉」「添加食物弹窗」的结构，并把 insets 取值链上的每一档都打在屏上，
 * 用来判断是「值取错了」还是「值对了但布局没用上」。
 */
class InsetsProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设备锁屏也能看到探针页，避免验证时被锁屏挡住。
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()
        setContent {
            HealthDietProTheme {
                ProbeScreen()
            }
        }
    }
}

@Composable
private fun ProbeScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val tokens = LocalAppTokens.current
    var showSheet by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }
    val screenHeightPx = (context as? android.app.Activity)?.window?.decorView?.height ?: view.rootView.height
    var sheetBounds by remember { mutableStateOf("") }
    var modalBounds by remember { mutableStateOf("") }

    val overlayBottom = overlayNavigationBarPadding()
    val overlayTop = overlayStatusBarPadding()
    val viewBottomPx = ViewCompat.getRootWindowInsets(view)
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: -1
    val hostDecorBottomPx = (context as? android.app.Activity)
        ?.window?.decorView
        ?.let { ViewCompat.getRootWindowInsets(it)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom }
        ?: -1
    val resourceBottomPx = remember(context) {
        val id = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id != 0) context.resources.getDimensionPixelSize(id) else -1
    }
    val composeBottomPx = with(density) {
        androidx.compose.foundation.layout.WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
            .toPx()
    }.toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            android.util.Log.i(
                "InsetsProbe",
                "navMode=${context.systemNavigationMode()} density=${density.density} " +
                    "overlayNavDp=$overlayBottom overlayStatusDp=$overlayTop " +
                    "viewBottomPx=$viewBottomPx hostDecorBottomPx=$hostDecorBottomPx " +
                    "resourceBottomPx=$resourceBottomPx composeBottomPx=$composeBottomPx",
            )
        }
        Text("screen: ${density.density} density", style = tokens.type.bodyS)
        Text("navMode: ${context.systemNavigationMode()}", style = tokens.type.bodyS)
        Text("overlayNav = $overlayBottom", style = tokens.type.bodyS)
        Text("overlayStatus = $overlayTop", style = tokens.type.bodyS)
        Text("view insets bottom px = $viewBottomPx", style = tokens.type.bodyS)
        Text("host decor bottom px = $hostDecorBottomPx", style = tokens.type.bodyS)
        Text("resource navigation_bar_height px = $resourceBottomPx", style = tokens.type.bodyS)
        Text("compose navigationBars bottom px = $composeBottomPx", style = tokens.type.bodyS)
        Text("sheet bounds = $sheetBounds", style = tokens.type.bodyS)
        Text("modal bounds = $modalBounds", style = tokens.type.bodyS)
        Button(onClick = { showSheet = true }, modifier = Modifier.fillMaxWidth()) {
            Text("open sheet (选容器结构)")
        }
        Button(onClick = { showModal = true }, modifier = Modifier.fillMaxWidth()) {
            Text("open modal (添加食物结构)")
        }
    }

    if (showSheet) {
        AppBottomSheet(
            title = "选容器探针",
            onDismiss = { showSheet = false },
            initialSnap = AppSheetHeight.Half,
            contentScrollable = false,
            modifier = Modifier.onGloballyPositioned {
                val b = it.boundsInWindow()
                sheetBounds = "top=${b.top.toInt()} bottom=${b.bottom.toInt()} h=${b.height.toInt()}"
                android.util.Log.i(
                    "InsetsProbe",
                    "sheet $sheetBounds decorH=$screenHeightPx " +
                        "navFromBottom=${screenHeightPx - b.bottom.toInt()}",
                )
            },
        ) {
            val padInDialog = overlayNavigationBarPadding()
            androidx.compose.runtime.LaunchedEffect(padInDialog) {
                android.util.Log.i(
                    "InsetsDialog",
                    "overlayNavigationBarPadding in dialog = $padInDialog",
                )
            }
            val dialogContext = LocalContext.current
            val dialogView = LocalView.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                var ctx: android.content.Context? = dialogContext
                val chain = mutableListOf<String>()
                var guard = 0
                while (ctx != null && guard++ < 8) {
                    chain.add(ctx.javaClass.name)
                    ctx = (ctx as? android.content.ContextWrapper)?.baseContext
                }
                val act = chain.any { it.contains("Activity") }
                val hostBottom = run {
                    var c: android.content.Context? = dialogContext
                    var g = 0
                    while (c != null && c !is android.app.Activity && g++ < 8) {
                        c = (c as? android.content.ContextWrapper)?.baseContext
                    }
                    (c as? android.app.Activity)?.window?.decorView?.let {
                        ViewCompat.getRootWindowInsets(it)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom
                    } ?: -1
                }
                val viewBottom = ViewCompat.getRootWindowInsets(dialogView)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: -1
                val resId = dialogContext.resources.getIdentifier("navigation_bar_height", "dimen", "android")
                val resPx = if (resId != 0) dialogContext.resources.getDimensionPixelSize(resId) else -1
                android.util.Log.i(
                    "InsetsDialog",
                    "chain=$chain hasActivity=$act hostDecorBottomPx=$hostBottom " +
                        "dialogViewBottomPx=$viewBottom resourcePx=$resPx",
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .onGloballyPositioned {
                        val b = it.boundsInWindow()
                        sheetBounds = "top=${b.top.toInt()} bottom=${b.bottom.toInt()}"
                    },
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items((1..15).toList()) { index ->
                    Surface(
                        color = tokens.colors.panel,
                        shape = RoundedCornerShape(tokens.radii.label),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "容器卡片 #$index（这一行用来量底部是否被导航栏压住）",
                            style = tokens.type.body,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        }
    }

    if (showModal) {
        AppModal(
            title = "添加食物探针",
            onDismiss = { showModal = false },
            dismissOnScrimClick = false,
            contentScrollable = false,
            modifier = Modifier.onGloballyPositioned {
                val b = it.boundsInWindow()
                modalBounds = "top=${b.top.toInt()} bottom=${b.bottom.toInt()} h=${b.height.toInt()}"
                android.util.Log.i(
                    "InsetsProbe",
                    "modal $modalBounds decorH=$screenHeightPx " +
                        "navFromBottom=${screenHeightPx - b.bottom.toInt()}",
                )
            },
            actions = {
                Button(onClick = { showModal = false }) { Text("确定") }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .onGloballyPositioned {
                        val b = it.boundsInWindow()
                        modalBounds = "top=${b.top.toInt()} bottom=${b.bottom.toInt()}"
                    },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items((1..6).toList()) { index ->
                        Text(
                            "食物行 #$index",
                            style = tokens.type.body,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
