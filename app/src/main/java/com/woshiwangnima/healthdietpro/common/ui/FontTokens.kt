package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

object FontTokens {
    val headline @Composable get() = dimensionResource(R.dimen.text_size_headline).value.sp
    val title @Composable get() = dimensionResource(R.dimen.text_size_title).value.sp
    val subtitle @Composable get() = dimensionResource(R.dimen.text_size_subtitle).value.sp
    val body @Composable get() = dimensionResource(R.dimen.text_size_body).value.sp
    val label @Composable get() = dimensionResource(R.dimen.text_size_label).value.sp
    val caption @Composable get() = dimensionResource(R.dimen.text_size_caption).value.sp
    val micro @Composable get() = dimensionResource(R.dimen.text_size_micro).value.sp
}

@Composable
fun rememberFontStyleAlphaMid(): Float {
    val context = LocalContext.current
    return remember { AppPrefs.getFontStyleTokenAlphaMid(context) }
}

@Composable
fun appTypography(): Typography {
    val headline = FontTokens.headline
    val title = FontTokens.title
    val body = FontTokens.body
    return Typography(
        headlineLarge = TextStyle(fontSize = headline),
        headlineMedium = TextStyle(fontSize = headline),
        titleLarge = TextStyle(fontSize = title),
        titleMedium = TextStyle(fontSize = title),
        bodyLarge = TextStyle(fontSize = body),
        bodyMedium = TextStyle(fontSize = body),
    )
}
