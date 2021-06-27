@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package top.anagke.kwormhole

import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.util.Hasher
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.time.format.DateTimeFormatter.ISO_DATE_TIME

/**
 * A record of a synchronized file.
 */
interface IKfr {

    /**
     * The path of the file, relative based on the synchronizing root,
     * represents the unique identifier of a file. The path is started
     * and separated by a slash (`/`).
     */
    val path: String

    /**
     * The last time the file changed. The time is represented by epoch
     * millis.
     */
    val time: Long

    /**
     * The size (length) of the file. A special value `-1` represents a
     * non-existing file (deleted or removed).
     */
    val size: Long

    /**
     * The hash value of the file. Generally, it uses XXHash with
     * seed `0` as the function. A special value `0` represents a
     * non-existing file (deleted or removed).
     */
    val hash: Long

    fun ensurePathEquals(other: Kfr) = check(this.path == other.path) {
        "this.path == other.path failed, $this, $other"
    }


    /**
     * Indicates whether this KFR can replace the other KFR.
     *
     * If the KFR's
     *
     * @throws IllegalArgumentException if paths of this and that KFR
     *     aren't same
     */
    fun canReplace(that: Kfr?): Boolean {
        if (that == null) return true

        ensurePathEquals(that)
        return this.time >= that.time && !this.equalsContent(that)
    }


    fun equalsContent(other: Kfr): Boolean {
        ensurePathEquals(other)
        return this.size == other.size && this.hash == other.hash
    }


    fun exists(): Boolean {
        return size != -1L && hash != 0L
    }

    fun notExists(): Boolean {
        return size == -1L && hash == 0L
    }

    fun ensurePresent() {
        check(this.exists()) { "this.exists() failed, $this" }
    }

    fun ensureAbsent() {
        check(this.notExists()) { "this.notExists() failed, $this" }
    }


    fun toFile(root: File): File {
        return parseKfrPath(root, path)
    }

    fun toPath(root: Path): Path {
        return parseKfrPath(root.toFile(), path).toPath()
    }

}

data class Kfr(
    override val path: String,
    override val time: Long,
    override val size: Long,
    override val hash: Long,
) : IKfr {
    constructor(iKfr: IKfr) : this(iKfr.path, iKfr.time, iKfr.size, iKfr.hash)

    override fun toString(): String {
        val formattedTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), systemDefault()).format(ISO_DATE_TIME)
        val formattedHash = "0x${hash.toULong().toString(16).padStart(ULong.SIZE_BITS / 4, '0')}"
        val formattedSize = "${size}"
        return "Kfr(path=$path, time=$formattedTime, size=$formattedSize, hash=$formattedHash)"
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
