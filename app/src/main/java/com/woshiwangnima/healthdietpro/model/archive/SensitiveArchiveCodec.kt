package com.woshiwangnima.healthdietpro.model.archive

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

internal class SensitiveArchiveCodec {
    fun encryptAndCompress(plainText: String, password: CharArray): ByteArray {
        require(password.isNotEmpty())
        val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_SIZE).also(SecureRandom()::nextBytes)
        val encrypted = cipher(Cipher.ENCRYPT_MODE, password, salt, iv).doFinal(gzip(plainText.encodeToByteArray()))
        return ByteBuffer.allocate(MAGIC.size + salt.size + iv.size + encrypted.size)
            .put(MAGIC)
            .put(salt)
            .put(iv)
            .put(encrypted)
            .array()
    }

    fun decryptAndDecompress(encryptedArchive: ByteArray, password: CharArray): String {
        require(password.isNotEmpty())
        require(encryptedArchive.size > MAGIC.size + SALT_SIZE + IV_SIZE)
        val buffer = ByteBuffer.wrap(encryptedArchive)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        require(magic.contentEquals(MAGIC))
        val salt = ByteArray(SALT_SIZE).also(buffer::get)
        val iv = ByteArray(IV_SIZE).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        return gunzip(cipher(Cipher.DECRYPT_MODE, password, salt, iv).doFinal(encrypted)).decodeToString()
    }

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, iv: ByteArray): Cipher {
        val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, javax.crypto.spec.SecretKeySpec(key.encoded, "AES"), GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
    }

    private fun gzip(input: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        GZIPOutputStream(output).use { it.write(input) }
        output.toByteArray()
    }

    private fun gunzip(input: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(input)).use { it.readBytes() }

    private companion object {
        val MAGIC = byteArrayOf('H'.code.toByte(), 'D'.code.toByte(), 'P'.code.toByte(), 1)
        const val SALT_SIZE = 16
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
        const val KEY_SIZE_BITS = 256
        const val PBKDF2_ITERATIONS = 210_000
    }
}
