package com.woshiwangnima.healthdietpro.model.archive

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 导入文件的真实格式。
 *
 * 用户从系统文件选择器拿到的可能是「明文 JSON」「GZIP 压缩的明文 JSON」或「加密 HDP」，
 * 后缀名又常常被文件管理器改掉（`.json` / `.txt` / 无后缀），所以按**魔数**判断，
 * 不按扩展名、也不让用户在导入前先猜「这份文件加没加密」。
 */
internal enum class ArchivePayloadKind { PLAIN_JSON, GZIP_JSON, ENCRYPTED }

internal object ArchivePayloadCodec {
    private val GZIP_MAGIC = byteArrayOf(0x1F, 0x8B.toByte())
    private val HDP_MAGIC = byteArrayOf('H'.code.toByte(), 'D'.code.toByte(), 'P'.code.toByte(), 1)

    fun kindOf(bytes: ByteArray): ArchivePayloadKind = when {
        bytes.startsWith(HDP_MAGIC) -> ArchivePayloadKind.ENCRYPTED
        bytes.startsWith(GZIP_MAGIC) -> ArchivePayloadKind.GZIP_JSON
        else -> ArchivePayloadKind.PLAIN_JSON
    }

    fun compress(bytes: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        GZIPOutputStream(output).use { it.write(bytes) }
        output.toByteArray()
    }

    fun decompress(bytes: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }.decodeToString()

    /** 明文/压缩明文的解码；加密档案必须先过 [SensitiveArchiveCodec]。 */
    fun decodePlain(bytes: ByteArray): String = when (kindOf(bytes)) {
        ArchivePayloadKind.PLAIN_JSON -> bytes.decodeToString()
        ArchivePayloadKind.GZIP_JSON -> decompress(bytes)
        ArchivePayloadKind.ENCRYPTED -> error("Encrypted archive requires a password")
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }
}
