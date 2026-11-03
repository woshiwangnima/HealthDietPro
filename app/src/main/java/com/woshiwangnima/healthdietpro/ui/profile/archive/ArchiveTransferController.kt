package com.woshiwangnima.healthdietpro.ui.profile.archive

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woshiwangnima.healthdietpro.model.archive.ArchiveCompatibility
import com.woshiwangnima.healthdietpro.model.archive.ArchivePayloadCodec
import com.woshiwangnima.healthdietpro.model.archive.ArchivePayloadKind
import com.woshiwangnima.healthdietpro.model.archive.PlainUserArchiveRepository
import com.woshiwangnima.healthdietpro.model.archive.SensitiveArchiveCodec
import com.woshiwangnima.healthdietpro.model.archive.UserArchiveIntegrity
import com.woshiwangnima.healthdietpro.model.archive.UserArchiveSnapshot
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 导出格式：三个都是真的能落盘的格式，不再有「只有一个选项」的假下拉。 */
internal enum class ArchiveExportFormat(
    val encrypted: Boolean,
    val compressed: Boolean,
    val extension: String,
    val mimeType: String,
) {
    PLAIN_JSON(encrypted = false, compressed = false, extension = "json", mimeType = "application/json"),
    PLAIN_JSON_GZIP(encrypted = false, compressed = true, extension = "json.gz", mimeType = "application/gzip"),
    ENCRYPTED_HDP(encrypted = true, compressed = true, extension = "hdp", mimeType = "application/octet-stream"),
}

internal enum class ArchiveFailure {
    LOAD_FAILED,
    EXPORT_FAILED,
    IMPORT_FAILED,
    UNREADABLE_FILE,
    NOT_AN_ARCHIVE,
    NEWER_SCHEMA,
    WRONG_PASSWORD,
    PASSWORD_REQUIRED,
}

/** 页面顶部的两个同级分区：导出与导入互斥，同一时刻只做一件。 */
internal enum class ArchiveSection { EXPORT, IMPORT }

/** 导入落点：覆盖当前用户 / 新建一个用户。 */
internal enum class ArchiveImportMode { REPLACE_CURRENT, NEW_USER }

internal sealed interface ArchiveNotice {
    data object Exported : ArchiveNotice
    data object Imported : ArchiveNotice
    data class ImportedAsNewUser(val userName: String) : ArchiveNotice
    data object ExportCancelled : ArchiveNotice
}

internal enum class ArchivePreviewMode { EXPORT, IMPORT }

internal data class ArchivePreviewState(
    val snapshot: UserArchiveSnapshot,
    val mode: ArchivePreviewMode,
    /** 导入预览里这份档案是否可导入；[ArchiveCompatibility.NEWER] 时为 false。 */
    val importable: Boolean = true,
    val importMode: ArchiveImportMode = ArchiveImportMode.REPLACE_CURRENT,
)

internal data class ArchiveTransferUiState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val exportFormat: ArchiveExportFormat = ArchiveExportFormat.PLAIN_JSON,
    val snapshot: UserArchiveSnapshot? = null,
    val integrity: UserArchiveIntegrity = UserArchiveIntegrity.MISSING,
    val preview: ArchivePreviewState? = null,
    val failure: ArchiveFailure? = null,
    val notice: ArchiveNotice? = null,
    val passwordPrompt: Boolean = false,
    val passwordRejected: Boolean = false,
    val busy: Boolean = false,
    val importMode: ArchiveImportMode = ArchiveImportMode.REPLACE_CURRENT,
    /** 当前停留的分区。放在状态里而不是 Composable 里，从预览页返回时不会被弹回导出。 */
    val section: ArchiveSection = ArchiveSection.EXPORT,
    /** 本机存在、但注册表没认领的领域文件；非空说明有新领域没进档案。 */
    val unregisteredFiles: List<String> = emptyList(),
) {
    val canPreview: Boolean get() = snapshot != null
}

/**
 * 「导入/导出用户数据」页的状态与动作。
 *
 * 页面用到的 IO（导出打包、文件读取、解密、导入落盘）全部这里做完，Composable 只读
 * [uiState] 并回调方法；系统文件选择器的两个 launcher 由宿主 Activity 注入，
 * 因此界面层不直接接触 `ContentResolver`，也不需要自己拼请求码。
 */
@Stable
internal class ArchiveTransferController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val launchOpenDocument: (Array<String>) -> Unit,
    private val launchCreateDocument: (mimeType: String, fileName: String) -> Unit,
    private val onImported: () -> Unit,
) {
    private val repository = PlainUserArchiveRepository(context)
    private val codec = SensitiveArchiveCodec()

    var uiState by mutableStateOf(ArchiveTransferUiState())
        private set

    private var pendingExportBytes: ByteArray? = null
    private var pendingEncryptedFile: ByteArray? = null
    private var pendingImportJson: String? = null
    private var pendingImportBytes: Long = 0L

    fun open() {
        uiState = uiState.copy(visible = true, notice = null, failure = null, preview = null)
        if (uiState.snapshot == null) reload()
    }

    fun close() {
        clearSecrets()
        uiState = ArchiveTransferUiState()
    }

    fun consumeFailure() {
        uiState = uiState.copy(failure = null)
    }

    fun consumeNotice() {
        uiState = uiState.copy(notice = null)
    }

    fun reload() {
        if (uiState.loading) return
        uiState = uiState.copy(loading = true, failure = null)
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                repository.exportWithSnapshot().map { export ->
                    Triple(export, repository.currentIntegrity(), repository.unregisteredFiles())
                }
            }
            loaded.fold(
                onSuccess = { (export, integrity, unregistered) ->
                    uiState = uiState.copy(
                        loading = false,
                        snapshot = export.snapshot,
                        integrity = integrity,
                        unregisteredFiles = unregistered,
                        failure = null,
                    )
                },
                onFailure = {
                    uiState = uiState.copy(loading = false, failure = ArchiveFailure.LOAD_FAILED)
                },
            )
        }
    }

    fun selectSection(section: ArchiveSection) {
        uiState = uiState.copy(section = section)
    }

    fun selectFormat(format: ArchiveExportFormat) {
        uiState = uiState.copy(exportFormat = format)
    }

    fun selectImportMode(mode: ArchiveImportMode) {
        uiState = uiState.copy(importMode = mode)
        uiState.preview?.let { preview ->
            if (preview.mode == ArchivePreviewMode.IMPORT) {
                uiState = uiState.copy(preview = preview.copy(importMode = mode))
            }
        }
    }

    fun previewExport() {
        val snapshot = uiState.snapshot ?: return
        uiState = uiState.copy(preview = ArchivePreviewState(snapshot, ArchivePreviewMode.EXPORT))
    }

    fun dismissPreview() {
        if (uiState.preview?.mode == ArchivePreviewMode.IMPORT) {
            pendingImportJson = null
            pendingImportBytes = 0L
        }
        uiState = uiState.copy(preview = null)
    }

    /** 打包并在系统文件选择器里给出带时间戳的默认文件名。 */
    fun export(password: String) {
        val snapshot = uiState.snapshot ?: return
        val format = uiState.exportFormat
        if (format.encrypted && password.isBlank()) {
            uiState = uiState.copy(failure = ArchiveFailure.PASSWORD_REQUIRED)
            return
        }
        uiState = uiState.copy(busy = true)
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = repository.exportCurrentUser().getOrThrow()
                    when {
                        format.encrypted -> {
                            val chars = password.toCharArray()
                            try {
                                codec.encryptAndCompress(raw, chars)
                            } finally {
                                chars.fill('\u0000')
                            }
                        }
                        format.compressed -> ArchivePayloadCodec.compress(raw.toByteArray(Charsets.UTF_8))
                        else -> raw.toByteArray(Charsets.UTF_8)
                    }
                }
            }
            bytes.fold(
                onSuccess = { data ->
                    pendingExportBytes = data
                    uiState = uiState.copy(busy = false)
                    launchCreateDocument(format.mimeType, defaultFileName(snapshot, format))
                },
                onFailure = {
                    uiState = uiState.copy(busy = false, failure = ArchiveFailure.EXPORT_FAILED)
                },
            )
        }
    }

    fun onExportTargetPicked(uri: Uri?) {
        val data = pendingExportBytes
        pendingExportBytes = null
        if (uri == null || data == null) {
            uiState = uiState.copy(notice = ArchiveNotice.ExportCancelled)
            return
        }
        uiState = uiState.copy(busy = true)
        scope.launch {
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                        ?: error("Unable to open export target")
                }
            }
            uiState = uiState.copy(
                busy = false,
                notice = if (written.isSuccess) ArchiveNotice.Exported else null,
                failure = if (written.isSuccess) null else ArchiveFailure.EXPORT_FAILED,
                preview = if (written.isSuccess) null else uiState.preview,
            )
        }
    }

    fun beginImport() {
        launchOpenDocument(arrayOf("*/*"))
    }

    fun onImportFilePicked(uri: Uri?) {
        if (uri == null) return
        uiState = uiState.copy(loading = true, failure = null, passwordRejected = false)
        scope.launch {
            val read = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Unable to read selected file")
                }
            }
            val bytes = read.getOrNull()
            if (bytes == null) {
                uiState = uiState.copy(loading = false, failure = ArchiveFailure.UNREADABLE_FILE)
                return@launch
            }
            pendingImportBytes = bytes.size.toLong()
            if (ArchivePayloadCodec.kindOf(bytes) == ArchivePayloadKind.ENCRYPTED) {
                pendingEncryptedFile = bytes
                uiState = uiState.copy(loading = false, passwordPrompt = true, passwordRejected = false)
            } else {
                openImportPreview(decodePlain(bytes))
            }
        }
    }

    fun submitImportPassword(password: CharArray) {
        val encrypted = pendingEncryptedFile ?: return
        if (password.isEmpty()) {
            uiState = uiState.copy(passwordRejected = true)
            return
        }
        uiState = uiState.copy(loading = true, passwordRejected = false)
        scope.launch {
            val decrypted = withContext(Dispatchers.IO) {
                runCatching { codec.decryptAndDecompress(encrypted, password) }
            }
            password.fill('\u0000')
            decrypted.fold(
                onSuccess = { json ->
                    pendingEncryptedFile = null
                    uiState = uiState.copy(passwordPrompt = false, loading = false)
                    openImportPreview(json)
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        loading = false,
                        passwordRejected = true,
                        failure = if (error.isPasswordFailure()) null else ArchiveFailure.NOT_AN_ARCHIVE,
                    )
                },
            )
        }
    }

    fun cancelImportPassword() {
        pendingEncryptedFile = null
        uiState = uiState.copy(passwordPrompt = false, passwordRejected = false)
    }

    fun confirmImport() {
        val json = pendingImportJson ?: return
        val mode = uiState.importMode
        uiState = uiState.copy(busy = true)
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                when (mode) {
                    ArchiveImportMode.REPLACE_CURRENT -> repository.importIntoCurrentUser(json)
                    ArchiveImportMode.NEW_USER -> repository.importAsNewUser(json)
                }
            }
            imported.fold(
                onSuccess = { result ->
                    pendingImportJson = null
                    pendingImportBytes = 0L
                    onImported()
                    uiState = uiState.copy(
                        busy = false,
                        preview = null,
                        notice = if (result.newUser) {
                            ArchiveNotice.ImportedAsNewUser(result.userName)
                        } else {
                            ArchiveNotice.Imported
                        },
                        failure = null,
                    )
                    reload()
                },
                onFailure = {
                    uiState = uiState.copy(busy = false, failure = ArchiveFailure.IMPORT_FAILED)
                },
            )
        }
    }

    private fun decodePlain(bytes: ByteArray): String? = runCatching {
        ArchivePayloadCodec.decodePlain(bytes)
    }.onFailure {
        uiState = uiState.copy(loading = false, failure = ArchiveFailure.NOT_AN_ARCHIVE)
    }.getOrNull()

    private fun openImportPreview(json: String?) {
        if (json == null) return
        uiState = uiState.copy(loading = true)
        scope.launch {
            val inspected = withContext(Dispatchers.IO) {
                repository.inspectArchive(json, pendingImportBytes.takeIf { it > 0 } ?: json.toByteArray(Charsets.UTF_8).size.toLong())
            }
            inspected.fold(
                onSuccess = { snapshot ->
                    pendingImportJson = json
                    val importable = snapshot.compatibility != ArchiveCompatibility.NEWER
                    // 版本过新时只把「不可导入」写进预览页：再弹一次失败框等于同一件事说两遍。
                    uiState = uiState.copy(
                        loading = false,
                        preview = ArchivePreviewState(
                            snapshot = snapshot,
                            mode = ArchivePreviewMode.IMPORT,
                            importable = importable,
                            importMode = uiState.importMode,
                        ),
                        failure = null,
                    )
                },
                onFailure = {
                    uiState = uiState.copy(loading = false, failure = ArchiveFailure.NOT_AN_ARCHIVE)
                },
            )
        }
    }

    private fun clearSecrets() {
        pendingEncryptedFile = null
        pendingImportJson = null
        pendingImportBytes = 0L
        pendingExportBytes?.fill(0)
        pendingExportBytes = null
    }

    private fun defaultFileName(snapshot: UserArchiveSnapshot, format: ArchiveExportFormat): String {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        val user = snapshot.userName.trim()
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "")
            .take(16)
            .ifBlank { "user" }
        return "health-diet-pro-$user-$stamp.${format.extension}"
    }
}

private fun Throwable.isPasswordFailure(): Boolean =
    this is AEADBadTagException ||
        cause is AEADBadTagException ||
        this is javax.crypto.BadPaddingException ||
        cause is javax.crypto.BadPaddingException
