@file:Suppress("FunctionName")

package top.anagke.kwormhole

import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.Closeable
import top.anagke.kwormhole.util.Hasher
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.READ

sealed class FatKfr(val kfr: Kfr) : Closeable {

    var open: Boolean = true
        private set

    override fun close() {
        open = false
    }


    abstract fun body(): ByteString?

    abstract fun actualize(file: Path)


    val path get() = kfr.path

    fun canReplace(other: Kfr?) = this.kfr.canReplace(other)
    fun equalsPath(other: Kfr) = this.kfr.equalsPath(other)
    fun equalsContent(other: Kfr) = this.kfr.equalsContent(other)

    fun canReplace(other: FatKfr?) = this.kfr.canReplace(other?.kfr)
    fun equalsPath(other: FatKfr) = this.kfr.equalsPath(other.kfr)
    fun equalsContent(other: FatKfr) = this.kfr.equalsContent(other.kfr)

    fun exists() = this.kfr.exists()
    fun notExists() = this.kfr.notExists()


    fun toFile(root: File) = this.kfr.toFile(root)
    fun toPath(root: Path) = this.kfr.toPath(root)

}

internal class FatFileKfr(
    kfr: Kfr,
    val file: Path,
    private val cleanup: () -> Unit = {}
) : FatKfr(kfr) {

    private val channel: FileChannel?
    private val lock: FileLock?

    //TODO: An exception thrown in init block might occur an not-closed channel
    init {
        if (kfr.exists()) {
            channel = FileChannel.open(file, READ)
            lock = channel.lock(0, Long.MAX_VALUE, true)
            val fileSize = Files.size(file)
            check(fileSize == kfr.size)
            val fileHash = Hasher.hash(file)
            check(fileHash == kfr.hash)
        } else {
            channel = null
            lock = null
        }
    }

    override fun close() {
        super.close()
        channel?.close()
        lock?.close()

        cleanup.invoke()
    }


    override fun body(): ByteString? {
        return if (kfr.exists()) file.toFile().readBytes().toByteString() else null
    }

    override fun actualize(file: Path) {
        Files.createDirectories(file.parent)
        Files.copy(this.file, file, REPLACE_EXISTING)
    }

}

internal class FatBufferKfr(
    kfr: Kfr,
    val buffer: ByteString
) : FatKfr(kfr) {

    init {
        val bufferSize = buffer.size.toLong()
        check(bufferSize == kfr.size)
        val fileHash = Hasher.hash(buffer.toByteArray()) //TODO: Performance
        check(fileHash == kfr.hash)
    }

    override fun body(): ByteString {
        return this.buffer
    }

    override fun actualize(file: Path) {
        Files.createDirectories(file.parent)
        if (Files.notExists(file)) Files.createFile(file)
        Files.newOutputStream(file).use {
            this.buffer.write(it)
        }
    }

}

internal class FatEmptyKfr(
    kfr: Kfr,
) : FatKfr(kfr) {

    init {
        check(kfr.notExists())
    }

    override fun body(): ByteString? {
        return null
    }

    override fun actualize(file: Path) {
        Files.deleteIfExists(file)
    }

}


fun FatKfr(kfr: Kfr, file: Path, cleanup: () -> Unit = {}): FatKfr {
    return FatFileKfr(kfr, file, cleanup)
}

fun FatKfr(kfr: Kfr, file: File, cleanup: () -> Unit = {}): FatKfr {
    return FatFileKfr(kfr, file.toPath(), cleanup)
}

fun FatKfr(kfr: Kfr, buffer: ByteString): FatKfr {
    return FatBufferKfr(kfr, buffer)
}

fun EmptyFatKfr(kfr: Kfr): FatKfr {
    return FatEmptyKfr(kfr)
}
