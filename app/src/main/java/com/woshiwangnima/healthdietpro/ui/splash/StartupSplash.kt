package com.woshiwangnima.healthdietpro.ui.splash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import kotlinx.coroutines.delay

/** 启动页停留时长；题字两行读完约需两秒，留出余量后自然淡出。 */
private const val SplashDurationMillis = 2_400L
private const val SplashFadeMillis = 220L

/** 与启动图标汤面同源的谷物金。只做细描边、光晕和字影，不承担正文对比度。 */
private val SplashGold = Color(0xFFE2BB5E)

/** 浅色启动页的宣纸底；与窗口背景 `splash_background` 完全一致，避免先出现一帧白屏。 */
private val SplashPaper = Color(0xFFF3EDE0)
private val SplashPaperEnd = Color(0xFFEADFC8)

/** 深色启动页的墨绿底；起点与 `values-night/splash_background` 一致。 */
private val SplashDeepGreen = Color(0xFF12261F)
private val SplashDeepGreenEnd = Color(0xFF0B1712)

/**
 * 启动页宿主：先画主界面，再把启动页以不透明全屏层盖在上面。
 *
 * 主界面在底下照常组合，数据预热不会被启动页拖慢；[enabled] 为 false 时完全不进入启动页，
 * 只立即回调 [onFinished]，行为与改造前一致。外部深链（自定义食物编辑 / 详情 / AGP 导入）
 * 由 MainActivity 在传参时跳过启动页，避免工具链多等一秒。
 *
 * 首帧引导（onboarding）等需要弹窗的启动逻辑必须等 [onFinished] 再执行，
 * 否则 `Dialog` 是独立窗口，会盖在启动页上面。
 */
@Composable
internal fun StartupSplashHost(
    enabled: Boolean,
    onFinished: () -> Unit,
    content: @Composable () -> Unit,
) {
    var visible by remember(enabled) { mutableStateOf(enabled) }
    // 首帧只画启动页，主界面下一帧再组合：否则 MainShell 的 ViewModel / 大列表初始化会把
    // 「系统启动窗口 → 第一帧」拉长，用户会先看到一段空白底色（浅色主题下就是白屏）。
    var contentReady by remember(enabled) { mutableStateOf(!enabled) }
    val finished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        if (!enabled) {
            finished()
            return@LaunchedEffect
        }
        withFrameNanos { }
        // 再等一帧：确保画面上确实已经出现启动页，再把主界面放进组合。
        withFrameNanos { }
        contentReady = true
        delay(SplashDurationMillis)
        visible = false
        delay(SplashFadeMillis)
        finished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (contentReady) content()
        AnimatedVisibility(
            visible = visible,
            // 进场不淡入：启动页背景必须从第一帧就不透明，否则会短暂透出底下主界面。
            enter = EnterTransition.None,
            exit = fadeOut(tween(SplashFadeMillis.toInt())),
        ) {
            StartupSplashScreen()
        }
    }
}

/**
 * 启动页：图标居中，题字「自求口实，养正不移。」以衬线斜体、加宽字距、−12° 斜放在图标右下方。
 *
 * 题字用两行（逗号后换行），既保留完整句子，又让窄屏不会横向溢出；浅色纸底用图标碗身的墨绿、
 * 深色墨绿底用谷物金，两者都对底色保持高对比；金色另用于图标外框、背景光晕与字影。
 *
 * 背景使用与系统启动窗口完全一致的固定品牌色（浅色宣纸 / 深色墨绿），不从主题 `bg` 取值：
 * 否则浅色主题的 `#F4F6FB` 在系统窗口阶段看起来就是白屏，和启动页之间会多出一段空白。
 *
 * 不引入第三方字体文件：中文书法字体会让安装包增加数 MB，这里用系统衬线族 + 合成斜体 +
 * 字距与旋转来做出「题字」的观感，缺字时也能回退到系统字体。
 *
 * 启动页是可交互层的遮挡：吞掉点击、拦截返回，避免用户在 2.4 秒里误触到底下的主界面。
 */
@Composable
private fun StartupSplashScreen() {
    val tokens = LocalAppTokens.current
    val isDark = tokens.colors.bg.luminance() < 0.5f
    val background = if (isDark) {
        Brush.verticalGradient(listOf(SplashDeepGreen, SplashDeepGreenEnd))
    } else {
        Brush.verticalGradient(listOf(SplashPaper, SplashPaperEnd))
    }
    // 题字颜色不随时间变化：第一帧就要完整可见，不能再用 alpha 从 0 渐入，否则又是一段空屏。
    val mottoColor = if (isDark) SplashGold else SplashDeepGreen

    BackHandler(enabled = true) {}
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(232.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SplashGold.copy(alpha = 0.13f), Color.Transparent),
                    ),
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(156.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .clip(RoundedCornerShape(42.dp))
                        .border(
                            width = 1.dp,
                            color = SplashGold.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(42.dp),
                        ),
                )
                Image(
                    painter = painterResource(R.drawable.ic_app_badge),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(132.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.splash_motto),
                modifier = Modifier
                    .offset(x = 38.dp)
                    .rotate(-12f),
                color = mottoColor,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = SplashGold.copy(alpha = 0.28f),
                        offset = Offset(0f, 2f),
                        blurRadius = 10f,
                    ),
                ),
                maxLines = 2,
            )
        }
    }
}
