package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.model.memo.MemoTestCommand

/**
 * 【测试指令 · 记便签】页。
 *
 * 每条指令**带自己的参数输入**——造什么数据就填什么参数，不用为了「删光待办」先去改顶上的数量框。
 * 参数值只在点击【执行】时收敛（越界由模型层夹取），输入过程不做强校验，减少打字摩擦。
 *
 * 只有一条「创建随机笔记」指令，但参数给全：块数 / 字数 / 语音 / 归档 / 置顶 / 历史版本 /
 * 时间跨度都能调，各种列表与卡片形态都由它一档一档拼出来。
 */
@Composable
internal fun MemoTestScreen(
    onRun: (MemoTestCommand) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TestCommandPage(
        title = "测试指令 · 记便签",
        description = "为当前用户生成随机的便签数据：文件夹、笔记（含语音与历史版本）、待办。",
        onBack = onBack,
        modifier = modifier,
    ) {
        NotesCommands(onRun)
        TodosCommands(onRun)
    }
}

@Composable
private fun NotesCommands(onRun: (MemoTestCommand) -> Unit) {
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
        TestCommandBlock(
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
                TestParamRow {
                    TestParamNumberField("条数", richCount, { richCount = it }, Modifier.weight(1f))
                    TestParamNumberField("置顶条数", pinnedCount, { pinnedCount = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("文本块数下限", minBlocks, { minBlocks = it }, Modifier.weight(1f))
                    TestParamNumberField("文本块数上限", maxBlocks, { maxBlocks = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("每块字数下限", minChars, { minChars = it }, Modifier.weight(1f))
                    TestParamNumberField("每块字数上限", maxChars, { maxChars = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("每条语音块数（0 无）", voicePerNote, { voicePerNote = it }, Modifier.weight(1f))
                    TestParamNumberField("历史版本数", historyVersions, { historyVersions = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("更新时间近几天", recentDays, { recentDays = it }, Modifier.weight(1f))
                }
                TestParamSwitchRow("随机归入已有文件夹（无文件夹时保持未归档）", assignToFolders, { assignToFolders = it })
            },
        )

        TestCommandBlock(
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
                TestParamSwitchRow("同时删除语音附件文件", pruneVoiceFiles, { pruneVoiceFiles = it })
            },
        )

        TestCommandBlock(
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
                TestParamRow {
                    TestParamNumberField("总数", folderCount, { folderCount = it }, Modifier.weight(1f))
                    TestParamNumberField("最大嵌套层级", folderDepth, { folderDepth = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamTextField("命名前缀", folderPrefix, { folderPrefix = it }, Modifier.weight(1f))
                }
            },
        )

        TestCommandBlock(
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
        TestCommandBlock(
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
                TestParamRow {
                    TestParamNumberField("条数", todoCount, { todoCount = it }, Modifier.weight(1f))
                    TestParamNumberField("完成项占比（%）", donePercent, { donePercent = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("每条待办项数下限", minItems, { minItems = it }, Modifier.weight(1f))
                    TestParamNumberField("每条待办项数上限", maxItems, { maxItems = it }, Modifier.weight(1f))
                }
                TestParamRow {
                    TestParamNumberField("更新时间近几天", recentDays, { recentDays = it }, Modifier.weight(1f))
                }
            },
        )

        TestCommandBlock(
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

        TestCommandBlock(
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
