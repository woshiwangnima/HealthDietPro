package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

/**
 * 搜索框（通用规范 §4.4-2 输入框状态、§2.2 热区）。
 *
 * 与旧 `OutlinedTextField` 版搜索框的差别：
 * 1. 只有一个「放大镜在前、清空在后」的固定结构，不再在尾部并排两个图标按钮——
 *    旧版把「提交」和「清空」摆成两个同等重量的按钮，既不告诉用户输入即筛选，也撑着输入区变窄。
 * 2. 高 48 dp 固定行，圆角取 `radii.container`，描边默认 1 dp `borderStrong`、聚焦 2 dp `primary`，
 *    与卡片同一套底材与描边语言。
 * 3. 回车 = 搜索（`ImeAction.Search`），[onSearch] 只用于「提交这次搜索」（写历史、收起面板），
 *    过滤本身由调用方按 [value] 实时计算——避免出现「输入了但列表不动」的两套真相。
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(tokens.radii.container)
    val borderColor = when {
        !enabled -> colors.border
        focused -> colors.primary
        else -> colors.borderStrong
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.spacing.hitMin)
            .onFocusChanged { onFocusChanged?.invoke(it.isFocused) },
        enabled = enabled,
        singleLine = true,
        textStyle = tokens.type.body.copy(color = if (enabled) colors.text else colors.disabled),
        cursorBrush = SolidColor(colors.primary),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke()
                focusManager.clearFocus()
            },
        ),
        decorationBox = { textField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (enabled) colors.panel else colors.bg)
                    .border(
                        width = if (focused) tokens.radii.stroke else tokens.radii.highlight,
                        color = borderColor,
                        shape = shape,
                    )
                    .padding(start = tokens.spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (focused) colors.primary else colors.textMuted,
                )
                Spacer(Modifier.width(tokens.spacing.s2))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = tokens.type.body,
                            color = if (enabled) colors.textMuted else colors.disabled,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    textField()
                }
                if (value.isNotEmpty() && enabled) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(tokens.spacing.hitMin),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.nutrition_search_clear),
                            modifier = Modifier.size(18.dp),
                            tint = colors.textMuted,
                        )
                    }
                }
            }
        },
    )
}
