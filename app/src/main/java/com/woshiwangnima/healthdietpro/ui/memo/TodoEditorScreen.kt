package com.woshiwangnima.healthdietpro.ui.memo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.memo.MemoTodo
import com.woshiwangnima.healthdietpro.model.memo.MemoTodoItem
import com.woshiwangnima.healthdietpro.model.memo.newMemoId

private data class EditorTodoItem(
    val id: String,
    val text: String,
    val done: Boolean,
    val doneAtMillis: Long,
)

/**
 * 待办编辑器：**只有待办项**，没有标题。
 *
 * 列表页已经把多项待办的抬头固定成【待办清单】、单项待办干脆不显示抬头，
 * 编辑器再留一个「待办标题」输入框，用户填了也永远看不到——那是骗人。
 * 所以这里从标题下移到「添加待办项」，一条待办就是它自己的一串事。
 *
 * 交互按需求：**回车即新开一项**，一项一行；勾选只改状态，不在这里重排
 * （编辑中让行序跳动会打断输入），沉底顺序由列表页的 `displayItems()` 派生。
 * 待办不支持历史版本，所以这里没有 `mergedWith` 那一套。
 */
@Composable
internal fun TodoEditorScreen(
    existing: MemoTodo?,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val viewModel: MemoViewModel = viewModel()

    fun newEditorItem(text: String = ""): EditorTodoItem =
        EditorTodoItem(id = newMemoId("item"), text = text, done = false, doneAtMillis = 0L)

    val todoId = remember(existing) { existing?.id ?: newMemoId("todo") }
    var items by remember(todoId) {
        mutableStateOf(
            existing?.items?.map { EditorTodoItem(it.id, it.text, it.done, it.doneAtMillis) }
                ?.ifEmpty { listOf(newEditorItem()) }
                ?: listOf(newEditorItem()),
        )
    }
    var lastSaved by remember(todoId) { mutableStateOf(existing) }
    var pendingFocusId by remember { mutableStateOf<String?>(null) }
    val focusRequesters = remember(todoId) { mutableMapOf<String, FocusRequester>() }

    fun commit() {
        val now = System.currentTimeMillis()
        val savedItems = items.mapNotNull { item ->
            item.text.trim().takeIf(String::isNotEmpty)?.let {
                MemoTodoItem(id = item.id, text = it, done = item.done, doneAtMillis = item.doneAtMillis)
            }
        }
        val merged = MemoTodo(
            id = todoId,
            // 老存档里可能有标题，原样留着（不显示也不丢），新版编辑器不再产生标题。
            title = lastSaved?.title ?: existing?.title.orEmpty(),
            items = savedItems,
            createdAtMillis = existing?.createdAtMillis ?: lastSaved?.createdAtMillis ?: now,
            updatedAtMillis = now,
        )
        val unchanged = lastSaved != null &&
            merged.title == lastSaved?.title &&
            merged.items == lastSaved?.items
        if (unchanged) return
        if (lastSaved == null && merged.title.isBlank() && merged.items.isEmpty()) return
        lastSaved = merged
        viewModel.saveTodo(merged)
    }

    val commitRef = rememberUpdatedState { commit() }

    BackHandler {
        commitRef.value()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose { commitRef.value() }
    }

    fun addItemAfter(index: Int) {
        val item = newEditorItem()
        val updated = items.toMutableList()
        updated.add(index + 1, item)
        items = updated
        pendingFocusId = item.id
    }

    LaunchedEffect(pendingFocusId) {
        val id = pendingFocusId ?: return@LaunchedEffect
        focusRequesters[id]?.requestFocus()
        pendingFocusId = null
    }

    BaseScreen(
        title = stringResource(R.string.memo_todo_title),
        onBack = {
            commitRef.value()
            onBack()
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            // 与首页「新建待办」同一形态、同一位置：进来先看到加项入口，不用先滚到底。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = tokens.spacing.page,
                        end = tokens.spacing.page,
                        top = tokens.spacing.s2,
                        bottom = tokens.spacing.s1,
                    ),
            ) {
                MemoPillAction(
                    text = stringResource(R.string.memo_todo_add_item),
                    iconRes = R.drawable.ic_add,
                    onClick = { addItemAfter(items.lastIndex.coerceAtLeast(0)) },
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.page),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
            ) {
                Spacer(Modifier.size(tokens.spacing.s1))
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = tokens.spacing.rowMinHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { checked ->
                                items = items.map { current ->
                                    if (current.id != item.id) {
                                        current
                                    } else {
                                        current.copy(
                                            done = checked,
                                            doneAtMillis = if (checked) System.currentTimeMillis() else 0L,
                                        )
                                    }
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = tokens.colors.primary,
                                uncheckedColor = tokens.colors.textMuted,
                            ),
                        )
                        val focusRequester = focusRequesters.getOrPut(item.id) { FocusRequester() }
                        BasicTextField(
                            value = item.text,
                            onValueChange = { next ->
                                items = items.map { current ->
                                    if (current.id == item.id) current.copy(text = next) else current
                                }
                            },
                            textStyle = tokens.type.body.copy(
                                color = if (item.done) tokens.colors.disabled else tokens.colors.text,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            ),
                            cursorBrush = SolidColor(tokens.colors.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addItemAfter(index) }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .padding(vertical = tokens.spacing.s2),
                            decorationBox = { inner ->
                                Box {
                                    if (item.text.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.memo_todo_item_hint),
                                            style = tokens.type.body,
                                            color = tokens.colors.textMuted,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                        IconButton(
                            onClick = {
                                if (items.size <= 1) {
                                    items = listOf(newEditorItem())
                                } else {
                                    items = items.filterNot { it.id == item.id }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cancel),
                                contentDescription = stringResource(R.string.memo_delete),
                                tint = tokens.colors.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.size(tokens.spacing.s6))
            }
        }
    }
}
