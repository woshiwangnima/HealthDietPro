package com.woshiwangnima.healthdietpro.model.archive

import org.junit.Assert.assertEquals
import org.junit.Test

class SensitiveArchiveCodecTest {
    @Test
    fun `round trips encrypted compressed data`() {
        val codec = SensitiveArchiveCodec()
        val plainText = """{"archiveSchemaVersion":{"major":1,"minor":0,"patch":0},"name":"测试用户"}"""
        val encrypted = codec.encryptAndCompress(plainText, "correct horse battery staple".toCharArray())

        assertEquals(plainText, codec.decryptAndDecompress(encrypted, "correct horse battery staple".toCharArray()))
    }
}
