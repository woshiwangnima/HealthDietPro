package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color

internal class PagePreloader {
    private val preloadedKeys = mutableSetOf<String>()

    fun preloadData(key: String, block: () -> Unit) {
        if (preloadedKeys.add(key)) block()
    }
}

internal const val PAGE_ENTER_DURATION_MILLIS = 180
internal const val PAGE_EXIT_DURATION_MILLIS = 140

@Composable
internal fun <T> AnimatedPageContent(
    targetState: T,
    modifier: Modifier = Modifier,
    direction: (initialState: T, targetState: T) -> Int = { _, _ -> 1 },
    // 内容底色：默认铺页面底色，保证滑动过程中两页重叠时不会互相透出（§7 层叠）。
    // 嵌在别的容器里（例如白色卡片内）时可传父容器色，否则会在卡片中央留下一块异色矩形。
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            val isForward = direction(initialState, targetState) > 0
            ContentTransform(
                targetContentEnter = slideInHorizontally(
                    animationSpec = tween(PAGE_ENTER_DURATION_MILLIS, easing = FastOutSlowInEasing),
                ) { width ->
                        if (isForward) width / 12 else -width / 12
                },
                initialContentExit = slideOutHorizontally(
                    animationSpec = tween(PAGE_EXIT_DURATION_MILLIS, easing = FastOutSlowInEasing),
                ) { width ->
                        if (isForward) -width / 16 else width / 16
                },
            )
        },
        modifier = modifier.clipToBounds(),
        label = "pageContentTransition",
    ) { state ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor),
        ) {
            content(state)
        }
    }
}
