package top.anagke.kwormhole

import top.anagke.kwormhole.sync.Synchronizer.utcEpochMillis
import top.anagke.kwormhole.util.Hasher
import java.io.File

/**
 * A record of a synchronized file.
 *
 * @property path the relative path to the synchronizing root, represents the unique identifier of a file.
 * @property size the size (length) of the file. Negative value represents the file have been deleted.
 * @property time the last time when the file changed, in epoch millis.
 * @property hash the hash value of the file's content.
 * @constructor
 */
data class FileRecord(
    val path: String,
    val size: Long,
    val time: Long,
    val hash: Long,
) {
    companion object {
        fun byFile(root: File, file: File): FileRecord {
            val canonicalRoot = root.canonicalFile
            val canonicalFile = file.canonicalFile
            require(canonicalFile.startsWith(canonicalRoot)) { "The file $file is required to belong to $root" }
            val relativeFileString = "/${canonicalFile.toRelativeString(canonicalRoot).replace('\\', '/')}"
            return if (canonicalFile.exists()) {
                FileRecord(relativeFileString, canonicalFile.length(), utcEpochMillis, Hasher.hash(canonicalFile))
            } else {
                FileRecord(relativeFileString, -1, utcEpochMillis, 0)
            }
        }
    }

     fun sameTo(other: FileRecord?): Boolean {
        return other != null &&
                this.path == other.path &&
                this.size == other.size &&
                this.hash == other.hash
    }

     fun differTo(other: FileRecord?): Boolean {
        return !this.sameTo(other)
    }
}
