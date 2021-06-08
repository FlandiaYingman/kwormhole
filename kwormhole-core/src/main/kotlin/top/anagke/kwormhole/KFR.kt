package top.anagke.kwormhole

import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.util.Hasher
import java.io.File

/**
 * A record of a synchronized file.
 *
 * @property path the relative path to the synchronizing root, represents the unique identifier of a file.
 * @property time the last time when the file changed, in epoch millis.
 * @property size the size (length) of the file. Negative value represents the file have been deleted.
 * @property hash the hash value of the file's content.
 * @constructor
 */
data class KFR(
    val path: String,
    val time: Long,
    val size: Long,
    val hash: Long,
) {

    companion object {

        const val TIME_HEADER_NAME = "KFR-Time"
        const val SIZE_HEADER_NAME = "KFR-Size"
        const val HASH_HEADER_NAME = "KFR-Hash"

        fun File.asKfr(root: File): KFR {
            val path = this.toKfrPath(root)
            val time = utcEpochMillis
            val size = if (this.exists()) this.length() else -1
            val hash = if (this.exists()) Hasher.hash(this) else 0
            return KFR(path, time, size, hash)
        }

    }

    fun isValidTo(other: KFR?): Boolean {
        if (other == null) return true
        return this.time > other.time && !this.contentEquals(other)
    }

    fun contentEquals(other: KFR): Boolean {
        check(this.path == other.path) { "paths aren't same: '${this.path}' and '${other.path}" }
        return this.size == other.size && this.hash == other.hash
    }

    fun representsNotExisting(): Boolean {
        return size == -1L && hash == 0L
    }

    fun representsExisting(): Boolean {
        return !(size == -1L && hash == 0L)
    }

}

fun File.toKfrPath(root: File): String {
    val canonicalRoot = root.canonicalFile
    require(canonicalFile.startsWith(canonicalRoot)) { "The file $this is required to belong to $root" }
    return "/${canonicalFile.toRelativeString(canonicalRoot).replace(File.separatorChar, '/')}"
}

fun String.resolveBy(root: File): File {
    return root.resolve(this.removePrefix("/").replace('/', File.separatorChar))
}
