package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.model.memo.MemoTestCommand

/**
 * 【测试指令 · 记便签】分区。
 *
 * 与既有指令「一个全局数量输入」不同：**每条指令带自己的参数输入**——
 * 造什么数据就填什么参数，不用为了「删光待办」先去改顶上的数量框。
 * 参数值只在点击【执行】时收敛（越界由模型层夹取），输入过程不做强校验，减少打字摩擦。
 */
@Composable
internal fun MemoTestSection(onRun: (MemoTestCommand) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NotesCommands(onRun)
        TodosCommands(onRun)
    }
}

@Composable
private fun NotesCommands(onRun: (MemoTestCommand) -> Unit) {
    var plainCount by remember { mutableStateOf("10") }

    var richCount by remember { mutableStateOf("10") }
    var minBlocks by remember { mutableStateOf("1") }
    var maxBlocks by remember { mutableStateOf("4") }
    var minChars by remember { mutableStateOf("20") }
    var maxChars by remember { mutableStateOf("120") }
    var voicePerNote by remember { mutableStateOf("1") }
    var historyVersions by remember { mutableStateOf("2") }
    var pinnedCount by remember { mutableStateOf("2") }
    var recentDays by remember { mutableStateOf("30") }
    var assignToFolders by remember { mutableStateOf(false) }

    var pruneVoiceFiles by remember { mutableStateOf(true) }

    var folderCount by remember { mutableStateOf("6") }
    var folderDepth by remember { mutableStateOf("3") }
    var folderPrefix by remember { mutableStateOf("随机文件夹") }

    ActionSectionCard(title = "记便签 · 笔记", titleIconRes = R.drawable.ic_memo) {
        CommandBlock(
            title = "创建随机笔记（无语音 · 未归档）",
            description = "只含文本块，不进任何文件夹——最快铺满列表的占位数据。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_add,
                    { onRun(MemoTestCommand.CreatePlainNotes(plainCount.asIntOr(10))) },
                    Modifier.fillMaxWidth(),
                )
            },
            params = {
                ParamRow {
                    ParamNumberField("条数", plainCount, { plainCount = it }, Modifier.weight(1f))
                }
            },
        )

        CommandBlock(
            title = "创建随机笔记（自定义参数）",
            description = "控制块数、字数、语音块、置顶、历史版本、归档与更新时间，覆盖列表 / 排序 / 搜索 / 恢复各形态。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_add,
                    {
                        onRun(
                            MemoTestCommand.CreateRichNotes(
                                count = richCount.asIntOr(10),
                                minTextBlocks = minBlocks.asIntOr(1),
                                maxTextBlocks = maxBlocks.asIntOr(4),
                                minCharsPerBlock = minChars.asIntOr(20),
                                maxCharsPerBlock = maxChars.asIntOr(120),
                                voiceBlocksPerNote = voicePerNote.asIntOr(0),
                                assignToFolders = assignToFolders,
                                pinnedCount = pinnedCount.asIntOr(0),
                                historyVersionsPerNote = historyVersions.asIntOr(0),
                                recentDays = recentDays.asIntOr(30),
                            ),
                        )
                    },
                    Modifier.fillMaxWidth(),
                )
            },
            params = {
                ParamRow {
                    ParamNumberField("条数", richCount, { richCount = it }, Modifier.weight(1f))
                    ParamNumberField("置顶条数", pinnedCount, { pinnedCount = it }, Modifier.weight(1f))
                }
                ParamRow {
                    ParamNumberField("文本块数下限", minBlocks, { minBlocks = it }, Modifier.weight(1f))
                    ParamNumberField("文本块数上限", maxBlocks, { maxBlocks = it }, Modifier.weight(1f))
                }
                ParamRow {
                    ParamNumberField("每块字数下限", minChars, { minChars = it }, Modifier.weight(1f))
                    ParamNumberField("每块字数上限", maxChars, { maxChars = it }, Modifier.weight(1f))
                }
                ParamRow {
                    ParamNumberField("每条语音块数（0 无）", voicePerNote, { voicePerNote = it }, Modifier.weight(1f))
                    ParamNumberField("历史版本数", historyVersions, { historyVersions = it }, Modifier.weight(1f))
                }
                ParamRow {
                    ParamNumberField("更新时间近几天", recentDays, { recentDays = it }, Modifier.weight(1f))
                }
                ParamSwitchRow("随机归入已有文件夹（无文件夹时保持未归档）", assignToFolders, { assignToFolders = it })
            },
        )

        CommandBlock(
            title = "删除所有笔记",
            description = "清空全部笔记；语音附件是笔记的附属文件，默认一并清理。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_delete,
                    { onRun(MemoTestCommand.DeleteAllNotes(pruneVoiceFiles)) },
                    Modifier.fillMaxWidth(),
                )
            },
            params = {
                ParamSwitchRow("同时删除语音附件文件", pruneVoiceFiles, { pruneVoiceFiles = it })
            },
        )

        CommandBlock(
            title = "创建随机笔记文件夹",
            description = "按前缀顺序命名，随机组成不超过指定层级的树；父级总是先于子级创建。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_add,
                    {
                        onRun(
                            MemoTestCommand.CreateFolders(
                                totalCount = folderCount.asIntOr(6),
                                maxDepth = folderDepth.asIntOr(3),
                                namePrefix = folderPrefix,
                            ),
                        )
                    },
                    Modifier.fillMaxWidth(),
                )
            },
            params = {
                ParamRow {
                    ParamNumberField("总数", folderCount, { folderCount = it }, Modifier.weight(1f))
                    ParamNumberField("最大嵌套层级", folderDepth, { folderDepth = it }, Modifier.weight(1f))
                }
                ParamRow {
                    ParamTextField("命名前缀", folderPrefix, { folderPrefix = it }, Modifier.weight(1f))
                }
            },
        )

        CommandBlock(
            title = "删除所有笔记文件夹",
            description = "按仓库规则子文件夹与笔记逐级上提，最后全部笔记回到根级，笔记本身不会被删除。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_delete,
                    { onRun(MemoTestCommand.DeleteAllFolders) },
                    Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

@Composable
private fun TodosCommands(onRun: (MemoTestCommand) -> Unit) {
    var todoCount by remember { mutableStateOf("10") }
    var minItems by remember { mutableStateOf("1") }
    var maxItems by remember { mutableStateOf("6") }
    var donePercent by remember { mutableStateOf("40") }
    var recentDays by remember { mutableStateOf("30") }

    ActionSectionCard(title = "记便签 · 待办", titleIconRes = R.drawable.ic_todo) {
        CommandBlock(
            title = "创建随机待办",
            description = "覆盖单项卡 / 多项清单、未完成与已勾选沉底等列表形态；完成项按勾选时刻沉底。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_add,
                    {
                        onRun(
                            MemoTestCommand.CreateTodos(
                                count = todoCount.asIntOr(10),
                                minItems = minItems.asIntOr(1),
                                maxItems = maxItems.asIntOr(6),
                                donePercent = donePercent.asIntOr(0),
                                recentDays = recentDays.asIntOr(30),
                            ),
                        )
                    },
                    Modifier.fillMaxWidth(),
                )
            },
            params = {
                ParamRow {
                    ParamNumberField("条数", todoCount, { todoCount = it }, Modifier.weight(1f))
                    ParamNumberField("完成项占比（%）", donePercent, { donePercent = it }, Modifier.weight(1f))
                }
                ParamRow {
                    ParamNumberField("每条待办项数下限", minItems, { minItems = it }, Modifier.weight(1f))
                    ParamNumberField("每条待办项数上限", maxItems, { maxItems = it }, Modifier.weight(1f))
                }
                ParamRow {
                    ParamNumberField("更新时间近几天", recentDays, { recentDays = it }, Modifier.weight(1f))
                }
            },
        )

        CommandBlock(
            title = "删除所有待办",
            description = "清空全部待办与待办项。",
            actions = {
                AppIconTextButton(
                    "执行",
                    R.drawable.ic_delete,
                    { onRun(MemoTestCommand.DeleteAllTodos) },
                    Modifier.fillMaxWidth(),
                )
            },
        )

        CommandBlock(
            title = "待办勾选批处理",
            description = "一条指令铺满「全部完成沉底」或「全部重置」两种排序形态，不用手点。",
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIconTextButton(
                        "全部标记完成",
                        R.drawable.ic_check,
                        { onRun(MemoTestCommand.SetAllTodosCompletion(completed = true)) },
                        Modifier.weight(1f),
                    )
                    AppIconTextButton(
                        "全部重置未完成",
                        R.drawable.ic_cancel,
                        { onRun(MemoTestCommand.SetAllTodosCompletion(completed = false)) },
                        Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

/** 一条测试指令：标题 + 说明 + 自带参数 + 执行按钮，四样东西永远在同一块里。 */
@Composable
private fun CommandBlock(
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
private fun ParamRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun ParamNumberField(
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
private fun ParamTextField(
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
private fun ParamSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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

private fun String.asIntOr(fallback: Int): Int = trim().toIntOrNull() ?: fallback
