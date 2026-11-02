package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
internal data class DomainEnvelope<T>(
    val domainId: String,
    val schemaVersion: ArchiveSchemaVersion,
    val appVersion: String,
    val updatedAtMillis: Long,
    val payload: T,
)

/**
 * 读取一个领域存档。
 *
 * 版本判断必须是 **小于等于**，不能是相等：`ArchiveSchemaVersion.Current` 每升一次
 * （历史上已从 1.0.0 → 1.0.1 → 1.4.0），磁盘上未升级的旧存档就会全部被判为非法，
 * 而各存储层的读取失败会被折叠成「空档案」，用户下一次写入即覆盖整份历史。
 * 旧存档由各领域的 migrate 函数补齐新字段，缺失键由 `encodeDefaults` 的默认值兜住。
 *
 * 高于当前版本（新版写盘、旧版读取，例如 debug 装新版后又装回 release）仍然拒绝：
 * 那种存档里有当前代码不认识的语义，强行解读再写回会把新字段抹掉。
 * 拒绝时由 `decodeDomainSafely` 隔离副本，保证原文件不被覆盖。
 */
internal fun <T> Json.decodeDomain(
    raw: String,
    expectedDomainId: String,
    payloadSerializer: KSerializer<T>,
): T {
    val envelope = decodeFromString(DomainEnvelope.serializer(payloadSerializer), raw)
    require(envelope.domainId == expectedDomainId) { "Unexpected archive domain: ${envelope.domainId}" }
    require(envelope.schemaVersion <= ArchiveSchemaVersion.Current) {
        "Archive schema ${envelope.schemaVersion} is newer than supported ${ArchiveSchemaVersion.Current}"
    }
    return envelope.payload
}

internal fun <T> Json.encodeDomain(
    context: Context,
    domainId: String,
    payload: T,
    payloadSerializer: KSerializer<T>,
): String = encodeToString(
    DomainEnvelope.serializer(payloadSerializer),
    DomainEnvelope(
        domainId = domainId,
        schemaVersion = ArchiveSchemaVersion.Current,
        appVersion = appVersion(context),
        updatedAtMillis = System.currentTimeMillis(),
        payload = payload,
    ),
)
