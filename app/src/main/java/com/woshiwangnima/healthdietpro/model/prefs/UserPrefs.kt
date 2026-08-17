package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context
import android.util.AtomicFile
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Per-user preference entry point. Preferences are stored in
 * `files/user_archives/<uid>/user_preferences.json`.
 */
object UserPrefs {
    fun current(context: Context): UserPrefsScope {
        return UserPrefsScope.create(context, ProfilePrefs.getCurrentUserId(context))
    }

    fun forUser(context: Context, uid: String): UserPrefsScope = UserPrefsScope.create(context, uid)

    internal fun snapshot(context: Context, uid: String): Map<String, Any> =
        UserPrefsScope.create(context, uid).snapshot()

    internal fun replaceAll(context: Context, uid: String, values: Map<String, Any>): Boolean =
        UserPrefsScope.create(context, uid).replaceAll(values)

    /** Deletes the current user's preference domain. */
    fun deleteUserFile(context: Context, uid: String) {
        UserPreferencesArchiveStore(context, UserPrefsScope.normalizedUid(uid)).delete()
    }
}

/** A typed preference scope for one user. */
class UserPrefsScope private constructor(
    val context: Context,
    val uid: String,
    private val store: UserPreferencesArchiveStore,
    private var values: Map<String, Any>,
) {
    companion object {
        private const val fallbackUid = "default"

        internal fun normalizedUid(uid: String): String = uid.ifEmpty { fallbackUid }

        internal fun create(context: Context, uid: String): UserPrefsScope {
            val normalizedUid = normalizedUid(uid)
            val store = UserPreferencesArchiveStore(context, normalizedUid)
            val loaded = store.load()
            return UserPrefsScope(context, uid, store, loaded)
        }

        internal fun isSupportedValue(value: Any): Boolean = when (value) {
            is Boolean, is Int, is Long, is String -> true
            is Float -> value.isFinite()
            is Set<*> -> value.all { it is String }
            else -> false
        }

    }

    fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default
    fun putBoolean(key: String, v: Boolean) { put(key, v) }

    fun getString(key: String, default: String): String = values[key] as? String ?: default
    fun putString(key: String, v: String) { put(key, v) }

    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> =
        (values[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: default
    fun putStringSet(key: String, v: Set<String>) { put(key, v) }

    fun remove(key: String) { update { it - key } }

    fun getInt(key: String, default: Int): Int = values[key] as? Int ?: default
    fun putInt(key: String, v: Int) { put(key, v) }

    fun getFloat(key: String, default: Float): Float = values[key] as? Float ?: default
    fun putFloat(key: String, v: Float) { put(key, v) }

    fun getLong(key: String, default: Long): Long = values[key] as? Long ?: default
    fun putLong(key: String, v: Long) { put(key, v) }

    fun contains(key: String): Boolean = key in values

    internal fun snapshot(): Map<String, Any> = values.toMap()

    internal fun replaceAll(replacement: Map<String, Any>): Boolean {
        val validValues = replacement.filterValues(::isSupportedValue)
        if (validValues.size != replacement.size) return false
        return replace(validValues)
    }

    internal fun putAll(additions: Map<String, Any>): Boolean {
        val validValues = additions.filterValues(::isSupportedValue)
        if (validValues.size != additions.size) return false
        return update { it + validValues }
    }

    private fun put(key: String, value: Any) {
        require(isSupportedValue(value)) { "Unsupported preference value" }
        update { it + (key to value) }
    }

    private fun replace(replacement: Map<String, Any>): Boolean {
        val saved = store.replace(replacement)
        if (saved) values = replacement.toMap()
        return saved
    }

    private fun update(transform: (Map<String, Any>) -> Map<String, Any>): Boolean {
        val updated = store.update(transform)
        if (updated != null) values = updated
        return updated != null
    }

}

@Serializable
private data class UserPreferencesArchive(
    val values: Map<String, UserPreferenceValue>,
)

@Serializable
private data class UserPreferenceValue(
    val type: UserPreferenceType,
    val boolean: Boolean? = null,
    val int: Int? = null,
    val long: Long? = null,
    val float: Float? = null,
    val string: String? = null,
    val strings: List<String>? = null,
)

@Serializable
private enum class UserPreferenceType { BOOLEAN, INT, LONG, FLOAT, STRING, STRING_SET }

private class UserPreferencesArchiveStore(
    context: Context,
    private val uid: String,
) {
    private val context = context.applicationContext

    fun load(): Map<String, Any> = synchronized(lock) { readArchive().orEmpty() }

    fun replace(values: Map<String, Any>): Boolean = synchronized(lock) {
        write(values)
    }

    fun update(transform: (Map<String, Any>) -> Map<String, Any>): Map<String, Any>? = synchronized(lock) {
        val latest = readArchive().orEmpty()
        val updated = transform(latest)
        updated.takeIf { write(it) }
    }

    private fun write(values: Map<String, Any>): Boolean = run {
        val archive = UserPreferencesArchive(values.toSortedMap().mapValues { (_, value) -> encode(value) })
        val saved = runCatching {
            archiveFile.parentFile?.mkdirs()
            val atomicFile = AtomicFile(archiveFile)
            val output = atomicFile.startWrite()
            try {
                output.write(
                    json.encodeDomain(context, DOMAIN_ID, archive, UserPreferencesArchive.serializer())
                        .toByteArray(Charsets.UTF_8),
                )
                atomicFile.finishWrite(output)
            } catch (error: Throwable) {
                atomicFile.failWrite(output)
                throw error
            }
        }.isSuccess
        if (saved) runCatching {
            writeUserArchiveManifest(context, uid)
            ProfilePrefs.noteUserActivity(context, uid)
        }
        saved
    }

    fun delete() = synchronized(lock) {
        archiveFile.delete()
        File(archiveFile.parentFile, "${archiveFile.name}.bak").delete()
        File(archiveFile.parentFile, "${archiveFile.name}.new").delete()
    }

    private fun readArchive(): Map<String, Any>? = runCatching {
        val raw = archiveFile.readText(Charsets.UTF_8)
        val archive = json.decodeDomain(
            raw,
            DOMAIN_ID,
            UserPreferencesArchive.serializer(),
        )
        val values = archive.values.mapValues { (_, value) -> decode(value) }.toSortedMap()
        values
    }.getOrNull()

    private fun encode(value: Any): UserPreferenceValue = when (value) {
        is Boolean -> UserPreferenceValue(UserPreferenceType.BOOLEAN, boolean = value)
        is Int -> UserPreferenceValue(UserPreferenceType.INT, int = value)
        is Long -> UserPreferenceValue(UserPreferenceType.LONG, long = value)
        is Float -> UserPreferenceValue(UserPreferenceType.FLOAT, float = value)
        is String -> UserPreferenceValue(UserPreferenceType.STRING, string = value)
        is Set<*> -> UserPreferenceValue(
            UserPreferenceType.STRING_SET,
            strings = value.filterIsInstance<String>().sorted(),
        )
        else -> error("Unsupported preference value")
    }

    private fun decode(value: UserPreferenceValue): Any = when (value.type) {
        UserPreferenceType.BOOLEAN -> requireNotNull(value.boolean)
        UserPreferenceType.INT -> requireNotNull(value.int)
        UserPreferenceType.LONG -> requireNotNull(value.long)
        UserPreferenceType.FLOAT -> requireNotNull(value.float).also { require(it.isFinite()) }
        UserPreferenceType.STRING -> requireNotNull(value.string)
        UserPreferenceType.STRING_SET -> requireNotNull(value.strings).also { require(it.size == it.toSet().size) }.toSet()
    }

    private val archiveFile: File
        get() = File(context.filesDir, "user_archives/${safeUid(uid)}/user_preferences.json")
    private val lock: Any
        get() = locks.getOrPut(archiveFile.absolutePath) { Any() }

    private companion object {
        const val DOMAIN_ID = "user_preferences"
        val locks = ConcurrentHashMap<String, Any>()
        val json = Json { encodeDefaults = false; explicitNulls = false; ignoreUnknownKeys = false }

        fun safeUid(uid: String): String = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
    }
}
