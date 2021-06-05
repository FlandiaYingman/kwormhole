package top.anagke.kwormhole

import top.anagke.kwormhole.sync.utcEpochMillis
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

        const val SIZE_HEADER_NAME = "KFR-Size"
        const val TIME_HEADER_NAME = "KFR-Time"
        const val HASH_HEADER_NAME = "KFR-Hash"

        fun record(root: File, file: File): FileRecord {
            val recordPath = toRecordPath(root, file)
            return if (file.exists()) {
                FileRecord(recordPath, file.length(), utcEpochMillis, Hasher.hash(file))
            } else {
                FileRecord(recordPath, -1, utcEpochMillis, 0)
            }
        }
    }
}

fun toRecordPath(root: File, file: File): String {
    val canonicalRoot = root.canonicalFile
    val canonicalFile = file.canonicalFile
    require(canonicalFile.startsWith(canonicalRoot)) { "The file $file is required to belong to $root" }
    return "/${canonicalFile.toRelativeString(canonicalRoot).replace(File.separatorChar, '/')}"
}

fun toRealPath(root: File, path: String): File {
    return root.resolve(path.removePrefix("/").replace('/', File.separatorChar))
}

fun FileRecord.shouldReplace(other: FileRecord?): Boolean {
    return when {
        // If the other record is null,
        // we should replace it.
        other == null -> true

        // Otherwise, if this record is newer than the other,
        // and this record's content differs from the other's
        // we should replace it.
        (this.time > other.time) && (this.size != other.size || this.hash != other.hash) -> true

        // Otherwise, we shouldn't replace it.
        else -> false
    }
}