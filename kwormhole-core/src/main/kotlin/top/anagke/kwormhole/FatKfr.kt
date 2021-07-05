package top.anagke.kwormhole

import top.anagke.kio.util.TempFiles
import top.anagke.kwormhole.util.SharedCloseable
import top.anagke.kwormhole.util.shared
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.READ
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.writeBytes
import kotlin.math.ceil
import kotlin.math.min

/**
 * [FatKfr] is a [IKfr] which has an immutable body based on filesystem.
 *
 * A [Kfr] can not represents its body. Therefore, [FatKfr] is out. It
 * can easily exchange between local models - so long as it doesn't
 * transfer through network.
 *
 * The restriction of [FatKfr] is, it is based on filesystem. It can not
 * be transferred to another filesystem. Hence, here's [ThinKfr], take a
 * look if there is a requirement transferring [FatKfr] through network.
 */
interface FatKfr : IKfr, Closeable {

    fun bytes(range: Range = Range(0, size)): ByteArray

    fun channel(): SeekableByteChannel

    fun copy(dest: Path)

    fun move(dest: Path)

}


/**
 * A clean up function. Receives a [Path] to clean it.
 */
typealias Cleanup = (Path) -> Unit

/**
 * The internal implementation for [FatKfr].
 *
 * See [FatKfr] for more information.
 *
 * @property file the file this [FatKfr] based on
 * @property cleanup the clean up function after this [FatKfr] is closed
 *
 * @see FatKfr
 */
private class FileFatKfr(
    kfr: Kfr,
    private val file: Path?,
    private val cleanup: Cleanup?
) : FatKfr {

    override val path: String = kfr.path

    override val time: Long = kfr.time

    override val size: Long = kfr.size

    override val hash: Long = kfr.hash


    private val sharedChannel: SharedCloseable<FileChannel>? =
        if (exists()) {
            shared(file!!.toRealPath()) { FileChannel.open(file, READ).also { it.lock(0, Long.MAX_VALUE, true) } }
        } else {
            null
        }


    override fun bytes(range: Range): ByteArray {
        checkOpen()
        ensurePresent()
        return Files.newByteChannel(file!!, READ).use { ch ->
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
            buf.array()
        }
    }

    override fun channel(): FileChannel {
        checkOpen()
        ensurePresent()
        return FileChannel.open(file, READ)
    }

    override fun copy(dest: Path) {
        checkOpen()
        if (this.exists()) {
            Files.createDirectories(dest.parent)
            Files.copy(file!!, dest, REPLACE_EXISTING)
        } else {
            Files.deleteIfExists(dest)
        }
    }

    override fun move(dest: Path) {
        checkOpen()
        if (this.exists()) {
            Files.createDirectories(dest.parent)
            Files.move(file!!, dest, REPLACE_EXISTING)
        } else {
            Files.deleteIfExists(dest)
        }
        close()
    }


    private var open: Boolean = true

    private fun checkOpen() {
        check(open) { "check open failed, $this" }
    }

    override fun close() {
        open = false
        sharedChannel?.close()
        cleanup?.invoke(file!!)
    }

}


/**
 * Creates a new [FatKfr], based on the given file.
 *
 * If the KFR exists, the file is required to exist. If the KFR doesn't
 * exist, the file is required to not exist.
 *
 * The cleanup function is called after [FatKfr] closes.
 *
 * @param kfr the inherited KFR information
 * @param file the file this [FatKfr] based on
 * @param cleanup the cleanup function called after [FatKfr] closes
 * @return the created new [FatKfr]
 */
fun newFatKfr(kfr: Kfr, file: Path, cleanup: Cleanup = {}): FatKfr {
    when {
        kfr.exists() -> require(file.exists())
        kfr.notExists() -> require(file.notExists())
    }
    return FileFatKfr(kfr, file, cleanup)
}

/**
 * Creates a new [FatKfr], based on none.
 *
 * The KFR is required to not exist, because the [FatKfr] based on nothing.
 *
 * @param kfr the inherited KFR information
 * @return the created new [FatKfr]
 */
fun newFatKfr(kfr: Kfr): FatKfr {
    require(kfr.notExists())
    return FileFatKfr(kfr, null, null)
}


/**
 * Creates a new [FatKfr] with given content, based on a temporary file.
 *
 * If the KFR exists, the content is required to given. If the KFR
 * doesn't exist, the content is required not to given.
 *
 * The cleanup function is set by the internal implementation to clean
 * the temporary file.
 *
 * @param kfr the inherited KFR information
 * @param bytes the content of the created new [FatKfr]
 * @return the created new [FatKfr]
 */
fun tempFatKfr(kfr: Kfr, bytes: ByteArray?): FatKfr {
    return if (kfr.exists()) {
        require(bytes != null)
        val local = TempFiles.allocLocal().also { it.writeBytes(bytes) }
        newFatKfr(kfr, local) { TempFiles.free(it) }
    } else {
        require(bytes == null)
        newFatKfr(kfr)
    }
}


class Slicer(
    val fat: FatKfr,
    val sliceSize: Int,
) : Iterator<ThinKfr>, AutoCloseable {

    var position: Int = 0

    val length: Int = run {
        //If the fat KFR doesn't exist, it still should have at least 1 slice.
        if (fat.notExists()) {
            1
        } else {
            ceil(fat.size.toDouble() / sliceSize.toDouble()).toInt()
        }
    }


    fun isStandalone(): Boolean {
        return length == 1
    }

    fun isTerminate(): Boolean {
        return length == position || isStandalone()
    }


    private val lazyChannel: Lazy<SeekableByteChannel?> = lazy { if (fat.exists()) fat.channel() else null }

    private val channel by lazyChannel


    override fun hasNext(): Boolean {
        //If it is standalone, has next when 0/1, has no next when 1/1
        if (isStandalone()) position < length
        //If it is not standalone, has next when n/n, has no next when n+1/n
        return position <= length
    }

    override fun next(): ThinKfr {
        check(hasNext())
        val thin = when {
            isStandalone() -> nextSlice()
            isTerminate() -> nextTerminate()
            else -> nextSlice()
        }
        position++
        return thin
    }

    private fun nextSlice(): ThinKfr {
        val channel = this.channel!!

        val bodyPos = min(1L * position * sliceSize, fat.size)
        val bodyLen = min(sliceSize, fat.size.toInt())

        val buffer = ByteBuffer.allocate(bodyLen)
        channel.position(bodyPos)
        do {
            val bytes = channel.read(buffer)
        } while (bytes != -1 && buffer.hasRemaining())

        return unsafeThinKfr(
            fat.asPojo(),
            Range(bodyPos, min(bodyPos + bodyLen, fat.size)),
            Progress(position, length),
            buffer.array()
        )
    }

    private fun nextTerminate(): ThinKfr {
        return terminateThinKfr(fat.asPojo(), length)
    }


    override fun close() {
        if (this.lazyChannel.isInitialized()) {
            this.channel?.close()
        }
    }

}

fun FatKfr.slicer(sliceSize: Int): Slicer {
    return Slicer(this, sliceSize)
}