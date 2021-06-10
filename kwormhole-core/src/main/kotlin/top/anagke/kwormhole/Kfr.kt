@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package top.anagke.kwormhole

import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.util.Hasher
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * A record of a synchronized file.
 *
 * @property path the relative path to the synchronizing root, represents the unique identifier of a file.
 * @property time the last time when the file changed, in epoch millis.
 * @property size the size (length) of the file. Negative value represents the file have been deleted.
 * @property hash the hash value of the file's content.
 * @constructor
 */
data class Kfr(
    val path: String,
    val time: Long,
    val size: Long,
    val hash: Long,
) {

    companion object {
        const val TIME_HEADER_NAME = "KFR-Time"
        const val SIZE_HEADER_NAME = "KFR-Size"
        const val HASH_HEADER_NAME = "KFR-Hash"
    }


    fun canReplace(other: Kfr?): Boolean {
        if (other == null) return true
        check(this.equalsPath(other))
        return this.time >= other.time && !this.equalsContent(other)
    }


    fun equalsPath(other: Kfr): Boolean {
        return this.path == other.path
    }

    fun equalsContent(other: Kfr): Boolean {
        check(this.equalsPath(other))
        return this.size == other.size && this.hash == other.hash
    }


    fun exists(): Boolean {
        return size != -1L && hash != 0L
    }

    fun notExists(): Boolean {
        return size == -1L && hash == 0L
    }


    fun toFile(root: File): File {
        return parseKfrPath(root, path)
    }

    fun toPath(root: Path): Path {
        return parseKfrPath(root.toFile(), path).toPath()
    }

}


fun Kfr(root: File, file: File): Kfr {
    val path = toKfrPath(root, file)
    val time = utcEpochMillis
    val size = if (file.exists()) file.length() else -1
    val hash = if (file.exists()) Hasher.hash(file) else 0
    return Kfr(path, time, size, hash)
}

fun Kfr(root: File, path: String): Kfr {
    val file = parseKfrPath(root, path)
    return Kfr(root, file)
}


fun toKfrPath(root: File, file: File): String {
    val rootPath = root.toPath().toAbsolutePath()
    val filePath = file.toPath().toAbsolutePath()
    check(filePath.startsWith(rootPath))
    val fsSeparator = FileSystems.getDefault().separator
    val relative = rootPath.relativize(filePath)
        .toString()
        .replace(fsSeparator, "/")
    return "/$relative"
}

fun parseKfrPath(root: File, path: String): File {
    val fsSeparator = FileSystems.getDefault().separator
    val relativePath = path
        .removePrefix("/")
        .replace("/", fsSeparator)
    return root.resolve(relativePath)
}
