package com.woshiwangnima.healthdietpro.ui.test

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen

/*
 * 测试指令页的公共骨架与零件。
 *
 * 按记录功能拆页之后，页与页之间只有「造什么数据」不同：标题、说明、参数行、执行按钮
 * 这四样由这里统一给，各页只负责列指令。
 */

/**
 * 一页测试指令：标题栏 + 可滚动的竖排内容。
 *
 * 用 `Column + verticalScroll` 而不是 `LazyColumn`：指令块内部有输入框，
 * 每敲一个字重建整个 LazyColumn 的 item 会让输入框丢焦点；数量级也就十几块，不需要虚拟化。
 */
@Composable
internal fun TestCommandPage(
    title: String,
    description: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BaseScreen(title = title, onBack = onBack, includeStatusBarPadding = false) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

/** 既有指令页的「本页数量」输入：这几个记录功能只按条数造数据，共用一只框。 */
@Composable
internal fun TestCountCard(
    title: String,
    description: String,
    iconRes: Int,
    buttons: List<TestCountAction>,
    initialCount: String = "10",
) {
    var countText by remember { mutableStateOf(initialCount) }
    val count = countText.trim().toIntOrNull()?.coerceIn(1, 100) ?: 1

    ActionSectionCard(title = title, titleIconRes = iconRes) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = countText,
            onValueChange = { value -> if (value.all(Char::isDigit)) countText = value },
            label = { Text("本次添加条目数量（1 至 100）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        buttons.forEach { button ->
            AppIconTextButton(
                button.label,
                button.iconRes,
                { button.onClick(count) },
                Modifier.fillMaxWidth(),
            )
        }
    }
}

/** [TestCountCard] 里的一颗按钮：文案 + 图标 + 吃到本页数量之后的动作。 */
internal data class TestCountAction(
    val label: String,
    @param:DrawableRes val iconRes: Int,
    val onClick: (Int) -> Unit,
)

/** 一条测试指令：标题 + 说明 + 自带参数 + 执行按钮，四样东西永远在同一块里。 */
@Composable
internal fun TestCommandBlock(
    title: String,
    description: String,
    actions: @Composable ColumnScope.() -> Unit,
    params: @Composable ColumnScope.() -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        params()
        actions()
    }
}

@Composable
internal fun TestParamRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

/** 数字参数：输入过程只挡非数字，越界留给模型层夹取——打字时弹红字只会碍事。 */
@Composable
internal fun TestParamNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.all(Char::isDigit)) onValueChange(input) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
internal fun TestParamTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
internal fun TestParamSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

internal fun String.asIntOr(fallback: Int): Int = trim().toIntOrNull() ?: fallback
