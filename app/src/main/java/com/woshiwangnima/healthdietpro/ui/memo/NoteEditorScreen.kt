package com.woshiwangnima.healthdietpro.ui.memo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ConfirmDialog
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.memo.MemoBlock
import com.woshiwangnima.healthdietpro.model.memo.MemoBlockKind
import com.woshiwangnima.healthdietpro.model.memo.MemoFolder
import com.woshiwangnima.healthdietpro.model.memo.MemoNote
import com.woshiwangnima.healthdietpro.model.memo.MemoNoteVersion
import com.woshiwangnima.healthdietpro.model.memo.hasContent
import com.woshiwangnima.healthdietpro.model.memo.memoVoiceCount
import com.woshiwangnima.healthdietpro.model.memo.memoWordCount
import com.woshiwangnima.healthdietpro.model.memo.mergedWith
import com.woshiwangnima.healthdietpro.model.memo.newMemoId
import com.woshiwangnima.healthdietpro.model.memo.restoredFrom
import com.woshiwangnima.healthdietpro.model.memo.VoiceAdoptResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 编辑器内部块：文本块持有 [TextFieldValue] 才能记住光标，语音块只记文件信息。 */
private data class EditorBlock(
    val id: String,
    val kind: MemoBlockKind,
    val value: TextFieldValue = TextFieldValue(),
    val voiceFile: String = "",
    val durationMs: Long = 0L,
    val sourceName: String = "",
)

private fun MemoNote.toEditorBlocks(): List<EditorBlock> {
    val mapped = blocks.map { block ->
        when (block.kind) {
            MemoBlockKind.TEXT -> EditorBlock(
                id = block.id.ifBlank { newMemoId("block") },
                kind = MemoBlockKind.TEXT,
                value = TextFieldValue(block.text, TextRange(block.text.length)),
            )
            MemoBlockKind.VOICE -> EditorBlock(
                id = block.id.ifBlank { newMemoId("block") },
                kind = MemoBlockKind.VOICE,
                voiceFile = block.voiceFile,
                durationMs = block.durationMs,
                sourceName = block.sourceName,
            )
        }
    }
    return ensureTrailingTextBlock(mapped.ifEmpty { listOf(newTextBlock()) })
}

/**
 * 末尾是语音块时补一个空文本块。
 *
 * 语音块整条是可点的播放按钮，自身没有光标；它后面没有文本块时，用户就没有地方落光标，
 * 「在语音后面接着写」这件事在界面上根本做不到。补一个空文本块，末尾永远有可输入的位置。
 * 空文本块不会落盘（`editorBlocksToMemoBlocks` 会过滤），所以补完不改变存档。
 */
private fun ensureTrailingTextBlock(blocks: List<EditorBlock>): List<EditorBlock> =
    if (blocks.lastOrNull()?.kind == MemoBlockKind.VOICE) blocks + newTextBlock() else blocks

private fun newTextBlock(text: String = ""): EditorBlock = EditorBlock(
    id = newMemoId("block"),
    kind = MemoBlockKind.TEXT,
    value = TextFieldValue(text, TextRange(text.length)),
)

/** 一次可撤销的编辑快照：标题 + 全部内容块（含语音）。 */
private data class EditorSnapshot(
    val title: String,
    val blocks: List<EditorBlock>,
)

/**
 * 撤销 / 重做栈（【上一步】【下一步】）。
 *
 * 连续输入在 [COALESCE_MS] 内合成一步——每个键击都是独立一步的话，
 * 撤销一次只消掉一个字，退到一小时前要按几百下。结构变化（插语音 / 删块 / 恢复历史版本）
 * 永远独立成步：它们不是「接着写」，是换了一段内容。
 */
private class EditorHistory(initial: EditorSnapshot) {
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private val past = ArrayDeque<EditorSnapshot>()
    private val future = ArrayDeque<EditorSnapshot>()
    private var current = initial
    private var lastStepAt = 0L

    /** 内容变化后调用；[force] 为 true 时不再与最近的输入合并。 */
    fun record(next: EditorSnapshot, now: Long, force: Boolean = false) {
        if (force || now - lastStepAt > COALESCE_MS) {
            past.addLast(current)
            if (past.size > MAX_STEPS) past.removeFirst()
        }
        current = next
        lastStepAt = now
        future.clear()
        sync()
    }

    fun undo(): EditorSnapshot? {
        val previous = past.removeLastOrNull() ?: return null
        future.addLast(current)
        current = previous
        // 归零而不是刷新：撤销之后的下一次输入必须是独立的新步，
        // 否则会被「连击合并」吃掉，按一次撤销就再也回不去了。
        lastStepAt = 0L
        sync()
        return previous
    }

    fun redo(): EditorSnapshot? {
        val next = future.removeLastOrNull() ?: return null
        past.addLast(current)
        current = next
        lastStepAt = 0L
        sync()
        return next
    }

    private fun sync() {
        canUndo = past.isNotEmpty()
        canRedo = future.isNotEmpty()
    }

    companion object {
        private const val COALESCE_MS = 800L
        private const val MAX_STEPS = 100
    }
}

/**
 * 笔记编辑器。
 *
 * 版面自上而下是「语音 / 导入音频 → 标题 —— 统计 —— 正文」，三段之间用分割线隔开：
 * 标题是名字、统计是说明书、正文是内容，各自是不同性质的东西，不能糊成一团。
 * 右上角有两副面孔：**光标落在标题或正文里**（正在写）时换成【上一步】【下一步】【保存】
 * ——写作时要的是撤销与落盘，移动到 / 删除 / 历史版本这时候只会误触；**没在写**时才回到【更多】。
 * 【保存】点完即收工：清焦点（光标与键盘一起收起），右上角随即换回【更多】。
 *
 * 撤销栈覆盖标题与全部内容块（含语音的插入与删除），连击合并成一步；
 * 【保存】是主动落盘（带「已保存」反馈），退出编辑仍会自动兜底保存一次。
 */
@Composable
internal fun NoteEditorScreen(
    existing: MemoNote?,
    initialFolderId: String?,
    folders: List<MemoFolder>,
    historyDepth: Int,
    onMove: (String, String?) -> Unit,
    onDelete: (MemoNote) -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val context = LocalContext.current
    val viewModel: MemoViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val noteId = remember(existing) { existing?.id ?: newMemoId("note") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var title by remember(noteId) { mutableStateOf(existing?.title.orEmpty()) }
    var blocks by remember(noteId) { mutableStateOf(existing?.toEditorBlocks() ?: listOf(newTextBlock())) }
    var folderId by remember(noteId) { mutableStateOf(existing?.folderId ?: initialFolderId) }
    var lastSaved by remember(noteId) { mutableStateOf(existing) }
    val history = remember(noteId) { EditorHistory(EditorSnapshot(title, blocks)) }
    var titleFocused by remember { mutableStateOf(false) }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var historyOpen by remember { mutableStateOf(false) }
    // 历史版本预览：非空时整页切成只读预览（标题栏换成该版本的时刻），返回回到版本列表。
    var previewVersion by remember { mutableStateOf<MemoNoteVersion?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var moveOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    // 从菜单删掉这条笔记后，退出时的 commit 不能再把它写回去。
    var deleted by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recordStartedAt by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<Int?>(null) }
    var savedFlash by remember { mutableStateOf(false) }

    val recorder = remember { MemoRecorder(context) }
    val player = remember { MemoVoicePlayer() }
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }
    // 离开编辑器必须收掉麦克风，否则录音会在后台一直跑。
    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    // 编辑器里也能整理文件夹（移动到弹窗的行尾菜单）；文件夹被删时笔记上提一级，
    // 编辑器本地记的归属要跟着改，否则退出时会把已删除的文件夹写回去。
    LaunchedEffect(folders) {
        val current = folderId ?: return@LaunchedEffect
        if (folders.none { it.id == current }) folderId = null
    }

    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1_500)
            savedFlash = false
        }
    }

    fun editorBlocksToMemoBlocks(): List<MemoBlock> {
        val meaningful = blocks.mapNotNull { block ->
            when (block.kind) {
                MemoBlockKind.TEXT -> if (block.value.text.isBlank()) {
                    null
                } else {
                    MemoBlock(kind = MemoBlockKind.TEXT, text = block.value.text, id = block.id)
                }
                MemoBlockKind.VOICE -> block.voiceFile.takeIf(String::isNotBlank)?.let { file ->
                    MemoBlock(
                        kind = MemoBlockKind.VOICE,
                        voiceFile = file,
                        durationMs = block.durationMs,
                        sourceName = block.sourceName,
                        id = block.id,
                    )
                }
            }
        }
        return meaningful
    }

    fun commit() {
        if (deleted) return
        val now = System.currentTimeMillis()
        val base = (
            lastSaved ?: MemoNote(
                id = noteId,
                folderId = folderId,
                createdAtMillis = now,
                updatedAtMillis = now,
            )
            ).copy(folderId = folderId)
        val merged = base.mergedWith(title, editorBlocksToMemoBlocks(), historyDepth, now)
        if (merged == base) return
        if (lastSaved == null && !merged.hasContent) return
        lastSaved = merged
        viewModel.saveNote(merged)
    }

    val commitRef = rememberUpdatedState { commit() }

    fun snapshot(): EditorSnapshot = EditorSnapshot(title, blocks)

    fun applyUndo() {
        val previous = history.undo() ?: return
        title = previous.title
        blocks = ensureTrailingTextBlock(previous.blocks)
    }

    fun applyRedo() {
        val next = history.redo() ?: return
        title = next.title
        blocks = ensureTrailingTextBlock(next.blocks)
    }

    /**
     * 退出「正在写」：光标收起、键盘落下，右上角回到【更多】。
     *
     * 【保存】是「这一段写完了」的动作，不是「再存一次稿」。存完还把光标留在正文里，
     * 用户就得先点一下空白处才能看到【移动到 / 历史版本 / 删除】——保存等于什么都没结束。
     * 只清焦点：草稿仍在编辑器里，继续点正文还能接着写。
     */
    fun exitEditing() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    /** 关闭预览并回到版本列表：用户是「从列表点进来看」，退出来理应还在列表上。 */
    fun closePreview() {
        previewVersion = null
        historyOpen = true
    }

    fun restoreVersion(version: MemoNoteVersion) {
        val base = lastSaved ?: return
        val restored = base.restoredFrom(version, historyDepth, System.currentTimeMillis())
        lastSaved = restored
        viewModel.saveNote(restored)
        title = restored.title
        blocks = restored.toEditorBlocks()
        history.record(snapshot(), System.currentTimeMillis(), force = true)
        previewVersion = null
        historyOpen = false
    }

    BackHandler {
        if (previewVersion != null) {
            closePreview()
        } else {
            commitRef.value()
            onBack()
        }
    }

    fun insertVoice(fileName: String, durationMs: Long, sourceName: String) {
        val voice = EditorBlock(
            id = newMemoId("block"),
            kind = MemoBlockKind.VOICE,
            voiceFile = fileName,
            durationMs = durationMs,
            sourceName = sourceName,
        )
        val focusIndex = blocks.indexOfFirst { it.id == focusedBlockId }
        val target = blocks.getOrNull(focusIndex)
        if (target == null || target.kind != MemoBlockKind.TEXT) {
            // 没有可落光标的位置：语音接在末尾。末尾如果正好是一个空文本块，就把它挪到语音后面，
            // 让它继续当「语音之后的落点」，免得语音前面多出一个空行。
            val trailingEmpty = blocks.lastOrNull()
                ?.takeIf { it.kind == MemoBlockKind.TEXT && it.value.text.isEmpty() }
            blocks = if (trailingEmpty != null) {
                blocks.dropLast(1) + voice + trailingEmpty
            } else {
                ensureTrailingTextBlock(blocks + voice)
            }
            history.record(snapshot(), System.currentTimeMillis(), force = true)
            return
        }
        val caret = target.value.selection.start.coerceIn(0, target.value.text.length)
        val before = target.value.text.substring(0, caret)
        val after = target.value.text.substring(caret)
        val updated = blocks.toMutableList()
        updated[focusIndex] = target.copy(value = TextFieldValue(before, TextRange(before.length)))
        updated.add(focusIndex + 1, voice)
        // 光标后半段哪怕为空也要补一个文本块：语音后面必须留着可落光标的位置。
        updated.add(focusIndex + 2, newTextBlock(after))
        blocks = updated
        history.record(snapshot(), System.currentTimeMillis(), force = true)
    }

    fun removeBlock(blockId: String) {
        if (player.playingId == blockId) player.stop()
        val updated = blocks.filterNot { it.id == blockId }.ifEmpty { listOf(newTextBlock()) }
        blocks = ensureTrailingTextBlock(updated)
        history.record(snapshot(), System.currentTimeMillis(), force = true)
    }

    fun startRecording() {
        if (!recorder.start()) {
            notice = R.string.memo_record_failed
            return
        }
        recording = true
        recordStartedAt = System.currentTimeMillis()
        elapsedMs = 0L
    }

    fun stopRecording() {
        if (!recording) return
        val duration = elapsedMs
        val file = recorder.stop()
        recording = false
        if (file == null) {
            notice = R.string.memo_record_failed
            return
        }
        scope.launch {
            when (val result = viewModel.adoptRecording(file)) {
                is VoiceAdoptResult.Saved -> insertVoice(result.fileName, duration, "")
                VoiceAdoptResult.TooLarge -> notice = R.string.memo_voice_too_large
                VoiceAdoptResult.Failed -> notice = R.string.memo_record_failed
            }
        }
    }

    /** 移动只改归属，不退出编辑器：改完还想接着写。新建未保存的笔记只记在本地，退出时随内容一起落盘。 */
    fun moveTo(target: String?) {
        if (target == folderId) return
        folderId = target
        lastSaved?.let { note ->
            onMove(note.id, target)
            lastSaved = note.copy(folderId = target)
        }
    }

    fun deleteAndExit() {
        deleted = true
        lastSaved?.let(onDelete)
        onBack()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else notice = R.string.memo_permission_audio_denied
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = viewModel.importVoice(uri)
            if (imported == null) {
                notice = R.string.memo_import_audio_failed
            } else {
                insertVoice(imported.fileName, imported.durationMs, imported.sourceName)
            }
        }
    }

    LaunchedEffect(recording) {
        while (recording) {
            elapsedMs = System.currentTimeMillis() - recordStartedAt
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose { commitRef.value() }
    }

    val liveBlocks = blocks.map { block ->
        when (block.kind) {
            MemoBlockKind.TEXT -> MemoBlock(kind = MemoBlockKind.TEXT, text = block.value.text)
            MemoBlockKind.VOICE -> MemoBlock(
                kind = MemoBlockKind.VOICE,
                voiceFile = block.voiceFile,
                durationMs = block.durationMs,
            )
        }
    }
    val editingActive = titleFocused || focusedBlockId != null
    // 历史版本预览整页盖在编辑器上：编辑器留在下面继续持有未保存的草稿，
    // 预览只是「读一版旧的」，退回列表后原来的草稿与光标都还在。
    Box(Modifier.fillMaxSize()) {
        BaseScreen(
            title = "",
            onBack = {
                commitRef.value()
                onBack()
            },
            actions = {
                if (editingActive) {
                    // 正在写：给撤销 / 重做 / 主动保存，其余操作收起来防误触。
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { applyUndo() }, enabled = history.canUndo) {
                            Icon(
                                painter = painterResource(R.drawable.ic_undo),
                                contentDescription = stringResource(R.string.memo_editor_undo),
                                tint = if (history.canUndo) tokens.colors.primary else tokens.colors.disabled,
                            )
                        }
                        IconButton(onClick = { applyRedo() }, enabled = history.canRedo) {
                            Icon(
                                painter = painterResource(R.drawable.ic_redo),
                                contentDescription = stringResource(R.string.memo_editor_redo),
                                tint = if (history.canRedo) tokens.colors.primary else tokens.colors.disabled,
                            )
                        }
                        MemoPillAction(
                            text = stringResource(R.string.memo_editor_save),
                            iconRes = R.drawable.ic_check,
                            onClick = {
                                commitRef.value()
                                savedFlash = true
                                exitEditing()
                            },
                        )
                    }
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.memo_note_more),
                                tint = tokens.colors.primary,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.memo_move_to)) },
                                onClick = {
                                    menuOpen = false
                                    moveOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.memo_history)) },
                                onClick = {
                                    menuOpen = false
                                    commitRef.value()
                                    historyOpen = true
                                },
                            )
                            HorizontalDivider(color = tokens.colors.border)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.memo_delete), color = tokens.colors.danger) },
                                onClick = {
                                    menuOpen = false
                                    deleteOpen = true
                                },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                NoteAudioBar(
                    recording = recording,
                    elapsedMs = elapsedMs,
                    onRecord = {
                        notice = null
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStop = { stopRecording() },
                    onImport = {
                        notice = null
                        audioPicker.launch(arrayOf("audio/*"))
                    },
                )

                notice?.let { messageRes ->
                    Text(
                        text = stringResource(messageRes),
                        style = tokens.type.caption,
                        color = tokens.colors.danger,
                        modifier = Modifier.padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s1),
                    )
                }

                if (savedFlash) {
                    Text(
                        text = stringResource(R.string.memo_saved_notice),
                        style = tokens.type.caption,
                        color = tokens.colors.success,
                        modifier = Modifier.padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s1),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = tokens.spacing.page),
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { next ->
                            title = next
                            history.record(snapshot(), System.currentTimeMillis())
                        },
                        textStyle = tokens.type.body.copy(
                            color = tokens.colors.text,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(tokens.colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state -> titleFocused = state.isFocused }
                            .padding(vertical = tokens.spacing.s2),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.memo_note_title_hint),
                                        style = tokens.type.body,
                                        color = tokens.colors.textMuted,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    // 标题是名字、统计是说明书、正文是内容：三段之间用细线隔开，扫视时各自成块。
                    HorizontalDivider(color = tokens.colors.border)
                    // 统计信息紧跟标题：它是这一页的说明书，不是落款。
                    Text(
                        text = lastSaved?.let { saved ->
                            stringResource(
                                R.string.memo_editor_meta,
                                formatMemoTime(saved.updatedAtMillis),
                                liveBlocks.memoWordCount(),
                                liveBlocks.memoVoiceCount(),
                            )
                        } ?: stringResource(
                            R.string.memo_editor_meta_unsaved,
                            liveBlocks.memoWordCount(),
                            liveBlocks.memoVoiceCount(),
                        ),
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                        modifier = Modifier.padding(vertical = tokens.spacing.s2),
                    )
                    HorizontalDivider(color = tokens.colors.border)
                    blocks.forEachIndexed { index, block ->
                        // 紧跟语音的空文本块是「语音之后」的落点：给它更明确的提示与最小可点高度，
                        // 否则一个空 TextField 只有占位字，用户不知道语音后面还能写。
                        val afterVoice = block.kind == MemoBlockKind.TEXT &&
                            (blocks.getOrNull(index - 1)?.kind == MemoBlockKind.VOICE)
                        when (block.kind) {
                            MemoBlockKind.TEXT -> BasicTextField(
                                value = block.value,
                                onValueChange = { next ->
                                    blocks = blocks.map { if (it.id == block.id) it.copy(value = next) else it }
                                    history.record(snapshot(), System.currentTimeMillis())
                                },
                                textStyle = tokens.type.body.copy(color = tokens.colors.text),
                                cursorBrush = SolidColor(tokens.colors.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { state ->
                                        focusedBlockId = if (state.isFocused) {
                                            block.id
                                        } else {
                                            focusedBlockId?.takeIf { it != block.id }
                                        }
                                    }
                                    .heightIn(min = if (afterVoice) 44.dp else 0.dp)
                                    .padding(vertical = tokens.spacing.s1),
                                decorationBox = { inner ->
                                    Box {
                                        if (block.value.text.isEmpty()) {
                                            Text(
                                                text = stringResource(
                                                    if (afterVoice) {
                                                        R.string.memo_note_continue_hint
                                                    } else {
                                                        R.string.memo_note_body_hint
                                                    },
                                                ),
                                                style = tokens.type.body,
                                                color = tokens.colors.textMuted,
                                            )
                                        }
                                        inner()
                                    }
                                },
                            )

                            MemoBlockKind.VOICE -> VoiceBlockRow(
                                block = block,
                                playing = player.playingId == block.id,
                                fileAvailable = viewModel.voices.detailFile(block.voiceFile) != null,
                                onToggle = {
                                    player.toggle(block.id, viewModel.voices.detailFile(block.voiceFile))
                                },
                                onRemove = { removeBlock(block.id) },
                            )
                        }
                    }
                    Spacer(Modifier.size(tokens.spacing.s6))
                }
            }
        }

    // 预览盖在编辑器之上；编辑器一直留在组合里，草稿与光标状态不因为「看一眼旧版本」被销毁。
    previewVersion?.let { version ->
        VersionPreviewScreen(
            version = version,
            onBack = { closePreview() },
            onRestore = { restoreVersion(version) },
        )
    }
    }

    if (moveOpen) {
        MoveNoteSheet(
            folders = folders,
            ops = viewModel.folderOps,
            onDismiss = { moveOpen = false },
            onSelect = { target ->
                moveTo(target)
                moveOpen = false
            },
            onFolderDeleted = { deletedFolder ->
                if (folderId == deletedFolder.id) folderId = deletedFolder.parentId
            },
        )
    }

    if (deleteOpen) {
        ConfirmDialog(
            title = stringResource(R.string.memo_delete_note_title),
            message = stringResource(R.string.memo_delete_note_message),
            confirmText = stringResource(R.string.memo_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                deleteOpen = false
                deleteAndExit()
            },
            onDismiss = { deleteOpen = false },
        )
    }

    if (historyOpen) {
        HistorySheet(
            versions = lastSaved?.versions.orEmpty(),
            depth = historyDepth,
            onDismiss = { historyOpen = false },
            onOpen = { version ->
                // 先收起列表再开预览：两层都不关的话，预览会压在弹窗底下。
                historyOpen = false
                previewVersion = version
            },
        )
    }
}

/**
 * 标题上方的音频入口：录音中整条换成「红点 + 计时 + 停止」，
 * 按钮不位移，手指不用重新找位置。
 */
@Composable
private fun NoteAudioBar(
    recording: Boolean,
    elapsedMs: Long,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onImport: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = tokens.spacing.page,
                end = tokens.spacing.page,
                top = tokens.spacing.s2,
                bottom = tokens.spacing.s1,
            ),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (recording) {
            Box(
                modifier = Modifier
                    .size(tokens.spacing.s2)
                    .clip(RoundedCornerShape(tokens.radii.pill))
                    .background(tokens.colors.danger),
            )
            Text(
                text = stringResource(R.string.memo_recording, formatDuration(elapsedMs)),
                style = tokens.type.bodyS,
                color = tokens.colors.danger,
                modifier = Modifier.weight(1f),
            )
            EditorActionButton(
                text = stringResource(R.string.memo_record_stop),
                iconRes = R.drawable.ic_stop,
                containerColor = tokens.colors.danger,
                contentColor = tokens.colors.onDangerFill,
                onClick = onStop,
            )
        } else {
            MemoPillAction(
                text = stringResource(R.string.memo_record_voice),
                iconRes = R.drawable.ic_mic,
                onClick = onRecord,
                modifier = Modifier.weight(1f),
            )
            MemoPillAction(
                text = stringResource(R.string.memo_import_audio),
                iconRes = R.drawable.ic_import,
                onClick = onImport,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VoiceBlockRow(
    block: EditorBlock,
    playing: Boolean,
    fileAvailable: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.s1)
            .clip(shape)
            .background(tokens.colors.panel)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .clickable(enabled = fileAvailable, onClick = onToggle)
            .heightIn(min = tokens.spacing.rowMinHeight)
            .padding(start = tokens.spacing.s3, end = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        Icon(
            painter = painterResource(
                when {
                    !fileAvailable -> R.drawable.ic_do_not_disturb
                    playing -> R.drawable.ic_pause
                    else -> R.drawable.ic_play
                },
            ),
            contentDescription = null,
            tint = if (fileAvailable) tokens.colors.primary else tokens.colors.disabled,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (fileAvailable) {
                    stringResource(R.string.memo_voice_length, formatDuration(block.durationMs))
                } else {
                    stringResource(R.string.memo_voice_missing)
                },
                style = tokens.type.bodyS,
                color = if (fileAvailable) tokens.colors.text else tokens.colors.textMuted,
            )
            if (block.sourceName.isNotBlank()) {
                Text(
                    text = block.sourceName,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // 删除是破坏性操作：用危险色 + 文字说明，不用一个没有标签的灰色图标——
        // 图标单独出现时，用户分不清它是「删掉这段语音」还是「停止播放」。
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(tokens.radii.pill))
                .background(tokens.colors.danger.copy(alpha = 0.12f))
                .clickable(onClick = onRemove)
                .heightIn(min = 36.dp)
                .padding(horizontal = tokens.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = tokens.colors.danger,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.memo_delete),
                style = tokens.type.caption.copy(fontWeight = FontWeight.Medium),
                color = tokens.colors.danger,
            )
        }
    }
}

@Composable
private fun EditorActionButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color? = null,
    contentColor: androidx.compose.ui.graphics.Color? = null,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(containerColor ?: tokens.colors.primaryContainer)
            .clickable(onClick = onClick)
            .heightIn(min = tokens.spacing.hitMin)
            .padding(horizontal = tokens.spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor ?: tokens.colors.onPrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = tokens.type.bodyS,
            color = contentColor ?: tokens.colors.onPrimaryContainer,
        )
    }
}

/**
 * 历史版本列表。
 *
 * 每个版本是一张**可点的卡**：时刻（带年份）、标题、完整统计（字数 / 语音数）与正文摘要都在卡上，
 * 点进去是只读预览——不点进去只靠一行摘要，用户没法判断两个版本到底差在哪。
 * 恢复动作收进预览页：先看清楚，再决定要不要用它覆盖当前内容。
 */
@Composable
private fun HistorySheet(
    versions: List<MemoNoteVersion>,
    depth: Int,
    onDismiss: () -> Unit,
    onOpen: (MemoNoteVersion) -> Unit,
) {
    val tokens = LocalAppTokens.current
    AppBottomSheet(
        title = stringResource(R.string.memo_history),
        subtitle = stringResource(R.string.memo_history_depth, depth),
        onDismiss = onDismiss,
    ) {
        if (versions.isEmpty()) {
            AppEmptyState(
                title = stringResource(R.string.memo_history_empty_title),
                description = stringResource(R.string.memo_history_empty_desc),
                iconRes = R.drawable.ic_history,
            )
        } else {
            Text(
                text = stringResource(R.string.memo_history_open_hint),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                modifier = Modifier.padding(bottom = tokens.spacing.s2),
            )
        }
        versions.reversed().forEach { version ->
            VersionCard(version = version, onClick = { onOpen(version) })
        }
        Spacer(Modifier.size(tokens.spacing.s3))
    }
}

@Composable
private fun VersionCard(version: MemoNoteVersion, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.s1)
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .padding(tokens.spacing.s3),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatMemoTimeFull(version.savedAtMillis),
                style = tokens.type.bodyS.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = tokens.colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = version.title.ifBlank { stringResource(R.string.memo_untitled_note) },
            style = tokens.type.bodyS,
            color = tokens.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = versionStats(version),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
        val preview = versionBodyPreview(version)
        if (preview.isNotEmpty()) {
            Text(
                text = preview,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 版本的完整统计：字数与语音数都给，0 条也写出来，免得「没写」和「只有语音」分不清。 */
@Composable
private fun versionStats(version: MemoNoteVersion): String = listOf(
    stringResource(R.string.memo_meta_words, version.blocks.memoWordCount()),
    stringResource(R.string.memo_meta_voices, version.blocks.memoVoiceCount()),
).joinToString(" · ")

/**
 * 历史版本预览：整页只读，标题栏是这个版本的时刻，右上角是【恢复此版本】。
 *
 * 语音块只展示时长与来源，不提供播放——预览要回答的是「这版写了什么」，
 * 播放留给正文编辑器，也避免两个页面同时抢播放器。
 */
@Composable
private fun VersionPreviewScreen(
    version: MemoNoteVersion,
    onBack: () -> Unit,
    onRestore: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    BackHandler { onBack() }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = tokens.colors.bg,
    ) {
        BaseScreen(
            title = formatMemoTimeFull(version.savedAtMillis),
            onBack = onBack,
            actions = {
                MemoPillAction(
                    text = stringResource(R.string.memo_history_restore_this),
                    iconRes = R.drawable.ic_history,
                    onClick = onRestore,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.page),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            ) {
                Text(
                    text = version.title.ifBlank { stringResource(R.string.memo_untitled_note) },
                    style = tokens.type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = if (version.title.isBlank()) tokens.colors.textMuted else tokens.colors.text,
                )
                Text(
                    text = versionStats(version),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
                HorizontalDivider(color = tokens.colors.border)
                version.blocks.forEach { block ->
                    when (block.kind) {
                        MemoBlockKind.TEXT -> if (block.text.isNotBlank()) {
                            Text(
                                text = block.text,
                                style = tokens.type.body,
                                color = tokens.colors.text,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        MemoBlockKind.VOICE -> VersionVoiceRow(block = block)
                    }
                }
                Spacer(Modifier.size(tokens.spacing.s6))
            }
        }
    }
}

@Composable
private fun VersionVoiceRow(block: MemoBlock) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panel)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = null,
            tint = tokens.colors.primary,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.memo_voice_length, formatDuration(block.durationMs)),
                style = tokens.type.bodyS,
                color = tokens.colors.text,
            )
            if (block.sourceName.isNotBlank()) {
                Text(
                    text = block.sourceName,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 版本正文摘要：压平空白、去掉语音块，最多 [limit] 字。 */
private fun versionBodyPreview(version: MemoNoteVersion, limit: Int = 100): String =
    version.blocks.filter { it.kind == MemoBlockKind.TEXT }
        .joinToString(" ") { it.text.trim() }
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(limit)

/** 语音条目的时长文案：`0:07` / `1:23`。 */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
