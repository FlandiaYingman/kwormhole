@file:Suppress("FunctionName")

package top.anagke.kwormhole

import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.*


interface FatKfr : IKfr {

    fun bytes(range: Range = Range(0, size)): ByteString

    fun actualize(dest: Path)

}

private abstract class AbsFatKfr(kfr: Kfr) : FatKfr {
    override val path: String = kfr.path
    override val time: Long = kfr.time
    override val size: Long = kfr.size
    override val hash: Long = kfr.hash
}

private class FileFatKfr(
    kfr: Kfr,
    val file: Path,
) : AbsFatKfr(kfr) {

    override fun bytes(range: Range): ByteString {
        ensurePresent()
        return Files.newByteChannel(file, READ).use { ch ->
            ch.position(range.begin)

            val buf = ByteBuffer.allocateDirect(Math.toIntExact(range.length()))
            do {
                val length = ch.read(buf)
            } while (length != -1 && buf.remaining() != 0)

            check(ch.position() == range.end) {
                "ch.position() == range.end failed, $this"
            }
            check(buf.position() == Math.toIntExact(range.length())) {
                "buf.position() == Math.toIntExact(range.length()) failed, $this"
            }

            buf.flip()
            buf.toByteString()
        }
    }

    override fun actualize(dest: Path) {
        if (this.exists()) {
            Files.createDirectories(dest.parent)
            Files.copy(file, dest, REPLACE_EXISTING)
        } else {
            Files.deleteIfExists(dest)
        }
    }

}

private class BufferFatKfr(
    kfr: Kfr,
    val buffer: ByteString?,
) : AbsFatKfr(kfr) {

    override fun bytes(range: Range): ByteString {
        ensurePresent()
        return buffer!!.substring(range.begin.toInt(), range.end.toInt())
    }

    override fun actualize(dest: Path) {
        if (this.exists()) {
            Files.createDirectories(dest.parent)
            Files.newOutputStream(dest, CREATE, WRITE).use { buffer!!.write(it) }
        } else {
            Files.deleteIfExists(dest)
        }
    }

}


fun FatKfr(kfr: Kfr, file: Path): FatKfr {
    return FileFatKfr(kfr, file)
}

fun FatKfr(kfr: Kfr, file: File): FatKfr {
    return FileFatKfr(kfr, file.toPath())
}

fun FatKfr(kfr: Kfr, buffer: ByteString): FatKfr {
    return BufferFatKfr(kfr, buffer)
}

fun FatKfr(kfr: Kfr, buffer: ByteArray): FatKfr {
    return BufferFatKfr(kfr, buffer.toByteString())
}
