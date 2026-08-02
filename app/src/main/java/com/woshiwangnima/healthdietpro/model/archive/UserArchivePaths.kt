package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import java.io.File

internal fun userArchiveDirectory(context: Context, userId: String): File =
    File(context.filesDir, "user_archives/${safeArchiveUserId(userId)}")

internal fun resolveAvatarFile(context: Context, userId: String, reference: String): File? {
    if (reference.isBlank()) return null
    val userRoot = userArchiveDirectory(context, userId).canonicalFile
    val attached = File(userRoot, reference).canonicalFile
    if (attached.path.startsWith(userRoot.path + File.separator) && attached.isFile) return attached
    return File(context.filesDir, "avatars/$reference").takeIf(File::isFile)
}

internal fun migrateAvatarReference(context: Context, userId: String, reference: String): String {
    if (reference.isBlank() || reference.startsWith("attachments/avatar/")) return reference
    val source = File(context.filesDir, "avatars/$reference")
    if (!source.isFile) return reference
    val fileName = reference.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
    val target = File(userArchiveDirectory(context, userId), "attachments/avatar/$fileName")
    target.parentFile?.mkdirs()
    if (!target.isFile) source.copyTo(target)
    return target.relativeTo(userArchiveDirectory(context, userId)).invariantSeparatorsPath
}

internal fun deleteAvatarReference(context: Context, userId: String, reference: String) {
    if (reference.isBlank()) return
    val root = userArchiveDirectory(context, userId).canonicalFile
    val attached = File(root, reference).canonicalFile
    if (attached.path.startsWith(root.path + File.separator)) {
        attached.delete()
    } else {
        File(context.filesDir, "avatars/$reference").delete()
    }
}

internal fun safeArchiveUserId(userId: String): String =
    userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
