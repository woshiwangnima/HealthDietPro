package com.woshiwangnima.healthdietpro.model.container

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 用户容器仓库（记容器）：用户级存档 + 容器图片附件。
 *
 * 图片保存在 `user_archives/<userId>/attachments/containers/`，记录仅存相对路径；
 * 删除用户时该目录随 `user_archives/<userId>` 级联清理。
 */
internal class ContainerRepository private constructor(private val context: Context) {
    private val archive = ContainerArchiveStore.current(context)

    fun load(): ContainerArchive = archive.load()

    fun upsert(record: ContainerRecord) = archive.update { current ->
        val now = System.currentTimeMillis()
        val normalized = if (current.containers.any { it.id == record.id }) record else record.copy(createdAtMillis = now)
        current.copy(containers = current.containers.filterNot { it.id == record.id } + normalized)
    }

    fun delete(id: String) = archive.update { current ->
        current.copy(containers = current.containers.filterNot { it.id == id })
    }

    fun saveImage(bitmap: Bitmap): String {
        val directory = attachmentDirectory().apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return file.relativeTo(context.filesDir).invariantSeparatorsPath
    }

    fun loadImage(relativePath: String): Bitmap? {
        val root = context.filesDir.canonicalFile
        val file = File(root, relativePath).canonicalFile
        return file.takeIf { it.path.startsWith(root.path + File.separator) && it.isFile }
            ?.let { BitmapFactory.decodeFile(it.path) }
    }

    fun deleteImage(relativePath: String) {
        val root = context.filesDir.canonicalFile
        val file = File(root, relativePath).canonicalFile
        if (file.path.startsWith(root.path + File.separator) && file.parentFile?.canonicalFile?.startsWith(attachmentDirectory().canonicalPath) == true) {
            file.delete()
        }
    }

    private fun attachmentDirectory(): File {
        val userId = ProfilePrefs.getCurrentUserId(context)
        val safeUserId = userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(context.filesDir, "user_archives/$safeUserId/attachments/containers")
    }

    companion object {
        fun fromContext(context: Context) = ContainerRepository(context.applicationContext)
    }
}
