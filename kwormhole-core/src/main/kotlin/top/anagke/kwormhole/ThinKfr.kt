package top.anagke.kwormhole

import top.anagke.kwormhole.util.Hasher
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

data class Range(
    val begin: Long,
    val end: Long
) {

    init {
        require(0 <= begin) { "0 <= begin failed, $this" }
        require(begin <= end) { "begin <= end failed, $this" }
    }

    fun length(): Long = end - begin

    override fun toString(): String {
        return "[$begin, $end)"
    }

}

data class Progress(
    val position: Int,
    val total: Int,
) {

    init {
        require(total != 0) { "denominator != 0 failed, $this" }
    }

    override fun toString(): String {
        return "$position/$total"
    }

}


/**
 * A [FatKfr] can not transfer through network, because it is too fat!
 *
 * However, here's [ThinKfr]. A [FatKfr] can be split into multiple
 * [ThinKfr] parts. The parts can be transferred through network in
 * sequence or parallel. Then the parts can be merged into the original
 * [FatKfr].
 */
interface ThinKfr : IKfr {

    /** Represents which part is this [unsafeThinKfr] from the [newFatKfr]. */
    val range: Range

    /** Represents the start and the end this [unsafeThinKfr] is. */
    val progress: Progress


    fun copy(): ByteArray

    fun move(): ByteArray


    fun isStandalone(): Boolean {
        return progress.total == 1
    }

    fun isTermination(): Boolean {
        return progress.total == progress.position || isStandalone()
    }

}

private class ThinKfrObj(
    kfr: Kfr,
    override val range: Range,
    override val progress: Progress,
    private var body: ByteArray? = null,
) : ThinKfr {

    override val path: String = kfr.path

    override val time: Long = kfr.time

    override val size: Long = kfr.size

    override val hash: Long = kfr.hash


    init {
        if (kfr.exists()) {
            require(body != null)
            require(range.length() == body!!.size.toLong())
            require(progress.position <= progress.total)
        }
        if (kfr.notExists()) {
            require(body == null)
            require(range.begin == 0L && range.end == 0L)
            require(progress.position == 0 && progress.total == 1)
        }
    }


    override fun copy(): ByteArray {
        ensurePresent()
        ensureAvailable()
        val body = this.body!!
        return body.copyOf()
    }

    override fun move(): ByteArray {
        ensurePresent()
        ensureAvailable()
        val body = this.body!!
        invalidate()
        return body
    }


    override fun toString(): String {
        return "ThinKfr(kfr=${Kfr(this)}, range=$range, progress=$progress, body=$body)"
    }


    private var available = true

    private fun ensureAvailable() {
        require(available) { "available failed, $this" }
    }

    private fun invalidate() {
        ensureAvailable()
        available = false
        body = null
    }

}


fun safeThinKfr(kfr: Kfr, range: Range, progress: Progress, body: ByteArray): ThinKfr {
    kfr.ensurePresent()
    return ThinKfrObj(kfr, range, progress, body.copyOf())
}

fun unsafeThinKfr(kfr: Kfr, range: Range, progress: Progress, body: ByteArray): ThinKfr {
    kfr.ensurePresent()
    return ThinKfrObj(kfr, range, progress, body)
}

fun absentThinKfr(kfr: Kfr): ThinKfr {
    kfr.ensureAbsent()
    return ThinKfrObj(kfr, Range(0, 0), Progress(0, 1), null)
}

fun terminateThinKfr(kfr: Kfr, length: Int): ThinKfr {
    kfr.ensurePresent()
    return ThinKfrObj(kfr, Range(kfr.size, kfr.size), Progress(length, length), byteArrayOf())
}


fun ThinKfr.merge(file: Path): FatKfr? {
    val body = this.move()

    Files.newByteChannel(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { ch ->
        ch.position(range.begin)

        val buf = ByteBuffer.wrap(body)
        do {
            val length = ch.write(buf)
        } while (length != -1 && buf.hasRemaining())

        check(ch.position() == range.end) {
            "ch.position() == range.end failed, $this"
        }
        check(buf.position() == Math.toIntExact(range.length())) {
            "buf.position() == Math.toIntExact(range.length()) failed, $this"
        }
    }

    return if (isTermination()) {
        val size = Files.size(file)
        val hash = Hasher.hash(file)

        require(size == this.size)
        require(hash == this.hash)

        newFatKfr(Kfr(this), file)
    } else {
        null
    }
}

fun ThinKfr.merge(file: Path, cleanup: (Path) -> Unit): FatKfr? {
    val body = this.move()

    Files.newByteChannel(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { ch ->
        ch.position(range.begin)

        val buf = ByteBuffer.wrap(body)
        do {
            val length = ch.write(buf)
        } while (length != -1 && buf.remaining() != 0)

        check(ch.position() == range.end) {
            "ch.position() == range.end failed, $this"
        }
        check(buf.position() == Math.toIntExact(range.length())) {
            "buf.position() == Math.toIntExact(range.length()) failed, $this"
        }
    }

    return if (isTermination()) {
        val size = Files.size(file)
        val hash = Hasher.hash(file)

        //TODO: Throw specific exception
        require(size == this.size)
        require(hash == this.hash)

        newFatKfr(Kfr(this), file, cleanup)
    } else {
        null
    }
}