package com.woshiwangnima.healthdietpro.model.archive

import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 导出的用户档案在界面上的「虚拟文件系统」。
 *
 * 导出的仍是一个 JSON 文件（`UserArchiveBundle`），但导入时会原样还原成
 * `user_archives/<uid>/` 下的目录结构：每个领域一个 `.json`，附件按 `attachments/…` 落盘。
 * 预览页要按文件夹层级展示，就得先把扁平 JSON 还原成这棵树，否则用户只能看到一坨 base64。
 */
internal enum class ArchiveNodeKind { FOLDER, JSON, IMAGE, BINARY }

internal data class ArchiveNode(
    val name: String,
    val path: String,
    val kind: ArchiveNodeKind,
    val byteSize: Long = 0L,
    /** 领域 id（`body_metrics` 等），UI 用它取本地化文件名与说明。 */
    val domainId: String? = null,
    /** JSON 文件的可读内容；二进制文件为 null。 */
    val text: String? = null,
    /** 领域里的记录条数，取不到时为 null（不硬编 0）。 */
    val recordCount: Int? = null,
    val children: List<ArchiveNode> = emptyList(),
) {
    val isFolder: Boolean get() = kind == ArchiveNodeKind.FOLDER
}

/** 档案格式版本相对当前安装包的兼容性。 */
internal enum class ArchiveCompatibility {
    /** 与本机同版本，可直接导入。 */
    CURRENT,

    /** 旧档案，导入后由各领域的迁移链补齐，可直接导入。 */
    OLDER,

    /** 更新版本写出的档案，本机字段不全，拒绝导入并提示升级。 */
    NEWER,
}

internal data class UserArchiveSnapshot(
    val formatVersion: ArchiveSchemaVersion,
    val compatibility: ArchiveCompatibility,
    val appVersion: String,
    val exportedAtMillis: Long?,
    val sourceUserId: String,
    val userName: String,
    val gender: String,
    val preferenceCount: Int,
    /** 档案 JSON 的字节数（导出后的真实体积）。 */
    val totalBytes: Long,
    /** 附件解码后的总字节数。 */
    val attachmentsBytes: Long,
    val root: ArchiveNode,
    /** 整份档案的缩进 JSON，供预览页的「原文」视图使用。 */
    val rawJson: String,
) {
    val files: List<ArchiveNode> = flattenFiles(root)
    val fileCount: Int get() = files.size

    /** 目录树里的文件夹数量（不含根）。 */
    val folderCount: Int get() = countFolders(root) - 1
}

private fun flattenFiles(node: ArchiveNode): List<ArchiveNode> = buildList {
    node.children.forEach { child ->
        if (child.isFolder) addAll(flattenFiles(child)) else add(child)
    }
}

private fun countFolders(node: ArchiveNode): Int =
    1 + node.children.filter { it.isFolder }.sumOf(::countFolders)

/**
 * 把档案 JSON 还原成目录树。
 *
 * 只做「读」，不写盘；调用方拿到的节点文本已经过 [stableJsonString] 归一化，
 * 因此预览看到的键序与实际导出的字节序一致。
 */
internal fun buildUserArchiveSnapshot(
    rawJson: String,
    byteSize: Long = rawJson.toByteArray(Charsets.UTF_8).size.toLong(),
): UserArchiveSnapshot {
    val bundle = snapshotJson.decodeFromString<UserArchiveBundle>(rawJson)
    val compatibility = when {
        bundle.formatVersion > ArchiveSchemaVersion.Current -> ArchiveCompatibility.NEWER
        bundle.formatVersion < ArchiveSchemaVersion.Current -> ArchiveCompatibility.OLDER
        else -> ArchiveCompatibility.CURRENT
    }
    val builder = ArchiveTreeBuilder()
    var attachmentsBytes = 0L
    var preferenceCount = 0
    // 领域清单只从注册表来：加领域不用改这里，也就不会出现「导出有、预览没有」。
    ArchiveDomains.forEach { domain ->
        val element = domain.extract(bundle) ?: return@forEach
        if (domain.id == "user_preferences") {
            preferenceCount = bundle.userPreferences.size
        }
        builder.addFile(
            path = domain.fileName,
            kind = ArchiveNodeKind.JSON,
            byteSize = element.toString().toByteArray(Charsets.UTF_8).size.toLong(),
            domainId = domain.id,
            text = stableJsonString(element.toString(), prettyPrint = true),
            recordCount = if (domain.id == "user_preferences") {
                // 偏好那一份的「条」是 key-value 项，不是业务记录。
                bundle.userPreferences.size
            } else {
                countArchiveRecords(element)
            },
        )
    }
    bundle.attachments.forEach { attachment ->
        val bytes = base64DecodedSize(attachment.contentBase64)
        attachmentsBytes += bytes
        builder.addFile(
            path = attachment.path,
            kind = if (attachment.path.isImagePath()) ArchiveNodeKind.IMAGE else ArchiveNodeKind.BINARY,
            byteSize = bytes,
        )
    }
    return UserArchiveSnapshot(
        formatVersion = bundle.formatVersion,
        compatibility = compatibility,
        appVersion = bundle.appVersion,
        exportedAtMillis = runCatching { Instant.parse(bundle.exportedAt).toEpochMilli() }.getOrNull(),
        sourceUserId = bundle.sourceUserId,
        userName = bundle.metadata?.name?.takeIf(String::isNotBlank)
            ?: bundle.profile.stringOrNull("name")
            ?: "",
        gender = bundle.metadata?.gender?.takeIf(String::isNotBlank)
            ?: bundle.profile.stringOrNull("gender")
            ?: "",
        preferenceCount = preferenceCount,
        totalBytes = byteSize,
        attachmentsBytes = attachmentsBytes,
        root = builder.build(),
        // 存**紧凑**文本，不在这里缩进：附件是几十兆的 base64，缩进一遍等于复制一整份字符串，
        // 而原文视图（JsonTextPanel）本来就会在后台线程自己做格式化。
        rawJson = stableJsonString(rawJson, prettyPrint = false),
    )
}

/** 附件在档案里的根目录，与 `user_archives/<uid>/attachments` 一致。 */
internal const val ARCHIVE_ATTACHMENTS_FOLDER = "attachments"

private fun JsonElement.stringOrNull(key: String): String? =
    runCatching { jsonObject[key]?.jsonPrimitive?.content }.getOrNull()?.takeIf(String::isNotBlank)

/**
 * 记录条数：领域信封的 `payload` 里可能叫 `records` / `foods` / `heightRecords`……
 * 都取不到时返回 null（界面显示「—」），而不是假装 0 条。
 */
private fun countArchiveRecords(element: JsonElement): Int? = runCatching {
    val payload = element.jsonObject["payload"]?.jsonObject ?: element.jsonObject
    val arrays = payload.values.filterIsInstance<JsonArray>()
    arrays.maxOfOrNull(JsonArray::size)
}.getOrNull()

/** base64 解码后的字节数，纯算术：预览里逐个解码上兆附件没有意义。 */
private fun base64DecodedSize(base64: String): Long {
    if (base64.isEmpty()) return 0L
    val padding = base64.takeLast(2).count { it == '=' }
    return (base64.length / 4).toLong() * 3L - padding
}

private fun String.isImagePath(): Boolean {
    val extension = substringAfterLast('.', "").lowercase()
    return extension in setOf("png", "jpg", "jpeg", "webp", "heic", "gif", "bmp")
}

private class ArchiveTreeBuilder {
    private val root = MutableNode(name = "", path = "")

    fun addFile(
        path: String,
        kind: ArchiveNodeKind,
        byteSize: Long,
        domainId: String? = null,
        text: String? = null,
        recordCount: Int? = null,
    ) {
        val segments = path.split('/').filter(String::isNotBlank)
        if (segments.isEmpty()) return
        var node = root
        for (index in 0 until segments.size - 1) {
            node = node.folder(segments[index])
        }
        node.files[segments.last()] = ArchiveNode(
            name = segments.last(),
            path = segments.joinToString("/"),
            kind = kind,
            byteSize = byteSize,
            domainId = domainId,
            text = text,
            recordCount = recordCount,
        )
    }

    fun build(): ArchiveNode = root.toNode()

    /** 同级排序：文件夹在前（按名字），文件在后（按名字），与桌面文件管理器一致。 */
    private class MutableNode(val name: String, val path: String) {
        val folders = linkedMapOf<String, MutableNode>()
        val files = linkedMapOf<String, ArchiveNode>()

        fun folder(name: String): MutableNode = folders.getOrPut(name) {
            MutableNode(name = name, path = if (path.isEmpty()) name else "$path/$name")
        }

        fun toNode(): ArchiveNode {
            val folderNodes = folders.values.sortedBy { it.name }.map(MutableNode::toNode)
            val fileNodes = files.values.sortedBy { it.name }
            return ArchiveNode(
                name = name,
                path = path,
                kind = ArchiveNodeKind.FOLDER,
                byteSize = folderNodes.sumOf { it.byteSize } + fileNodes.sumOf { it.byteSize },
                children = folderNodes + fileNodes,
            )
        }
    }
}

private val snapshotJson = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}
