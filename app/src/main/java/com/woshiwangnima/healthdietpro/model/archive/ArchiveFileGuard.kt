package com.woshiwangnima.healthdietpro.model.archive

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * 存档文件的安全读写。
 *
 * 各领域存储层原先统一写成 `runCatching { ... }.getOrNull()`：解析失败（版本不符、
 * 字段类型变化、文件被写坏）会被折叠成 null，上层 `load()` 于是返回一份空档案；
 * 用户下一次做任何记录，`save()` 就把「空档案 + 1 条新数据」写回磁盘，
 * 整份历史静默消失——没有报错，事后也无从查证。
 *
 * 这里把「读不出来」和「本来就没有」分开。
 */

/** 同一个文件的坏档副本最多留几份，避免反复读取时把磁盘刷爆。 */
private const val MAX_CORRUPT_SNAPSHOTS = 3

/**
 * 读取并解析一个领域存档。
 *
 * 返回 null 的两种情形：
 * - 文件不存在（首次使用）—— 正常；
 * - 文件存在但解析失败 —— 会先复制一份 `<name>.corrupt-<时间戳>` 留档，
 *   调用方可以放心当作「暂无数据」继续，原始字节不会丢。
 */
internal fun <T> Json.decodeDomainSafely(
    file: File,
    domainId: String,
    payloadSerializer: KSerializer<T>,
): T? {
    if (!file.isFile) return null
    val raw = runCatching { file.readText(Charsets.UTF_8) }
        .getOrNull()
        ?.takeIf(String::isNotBlank)
        ?: return null
    return runCatching { decodeDomain(raw, domainId, payloadSerializer) }
        .onFailure { quarantineArchive(file) }
        .getOrNull()
}

/**
 * 准备写入：建目录、留下一份上一版的 `.prev`，返回待写的临时文件。
 *
 * 调用方拿到临时文件后照原样 `writeText` + `renameTo`，语义不变。
 *
 * 单靠原子替换只能保证「不会写出半个文件」，挡不住「用错的内容整体覆盖」——
 * 前面那次被空档案覆盖就是这种情况。多留一份上一版，误覆盖还能退回去。
 */
internal fun prepareArchiveWrite(file: File): File {
    file.parentFile?.mkdirs()
    if (file.isFile) {
        runCatching { file.copyTo(File(file.parentFile, "${file.name}.prev"), overwrite = true) }
    }
    return File(file.parentFile, "${file.name}.tmp")
}

/** 把解析失败的存档另存快照；原文件保持不动，等人工或迁移工具处理。 */
internal fun quarantineArchive(file: File) {
    runCatching {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val snapshot = File(file.parentFile, "${file.name}.corrupt-$stamp")
        if (!snapshot.exists()) file.copyTo(snapshot, overwrite = false)
        file.parentFile?.listFiles()
            ?.filter { it.name.startsWith("${file.name}.corrupt-") }
            ?.sortedBy(File::lastModified)
            ?.dropLast(MAX_CORRUPT_SNAPSHOTS)
            ?.forEach(File::delete)
    }
}
