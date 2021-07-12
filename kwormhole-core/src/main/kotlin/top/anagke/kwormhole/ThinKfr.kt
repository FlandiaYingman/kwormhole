package top.anagke.kwormhole

import top.anagke.kwormhole.util.Hasher
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class Range(
    val begin: Long,
    val end: Long
) {

    init {
        require((begin - end) >= Integer.MIN_VALUE)
        require((end - begin) <= Integer.MAX_VALUE)
    }

    fun length(): Int = Math.toIntExact(end - begin)

    override fun toString(): String {
        return "[$begin, $end)"
    }

}

data class Progress(
    val position: Int,
    val amount: Int,
) {

    init {
        require(amount != 0) { "denominator != 0 failed, $this" }
    }

    override fun toString(): String {
        return "$position/$amount"
    }


    fun next(): Progress {
        return Progress(position + 1, amount)
    }

    fun position(newPosition: Int): Progress {
        return Progress(newPosition, amount)
    }


    fun isSingle(): Boolean {
        return amount == 1
    }

    fun isTerminal(): Boolean {
        return position == amount
    }

}


/**
 * A [FatKfr] can not transfer through network, because it is too fat!
 * However, here is [ThinKfr]. Firstly, the [FatKfr] can be split into
 * multiple [ThinKfr] parts, which formed a thin KFR sequence. Secondly,
 * the sequence is transferred through network, whatever in sequence or
 * in parallel. Finally, the received sequence can be merged back into
 * the original [FatKfr].
 *
 * If the length of the sequence is `1`, the sequence is "single".
 * Every non-single sequence should end with a additional "terminal"
 * thin KFR. For the terminal thin KFR: the `range` should be `[n, n)`,
 * which `n` is the size of the KFR; the `progress` should be `m/m`,
 * which `m` is the amount of the sequence; the `body` should not exist.
 * However, a single sequence can still end with a terminal thin KFR.
 *
 * If the [FatKfr] is present, the sequence can be either single or not.
 * For each [ThinKfr] in the sequence: the `range` should not be out of
 * the range `[0, s)`, which `s` is the size of the KFR; the `progress`
 * is determined by the given slice size and the size of the KFR; the
 * `body` should exist, unless the thin KFR is "terminal"; if the `body`
 * exists, the length of the `body` should equal to the length of the
 * `range`
 *
 * If the [FatKfr] is absent, the sequence is always single. For the
 * single [ThinKfr] in the sequence: the `range` should be `[-1, -1)`;
 * the `progress` should be `0/1`; the `body` should not exist.
 */
interface ThinKfr : IKfr {

    val range: Range

    val progress: Progress


    fun copy(): ByteArray

    fun move(): ByteArray


    fun isSingle(): Boolean {
        return progress.amount == 1
    }

    fun isTerminal(): Boolean {
        return range.begin == this.size && range.end == this.size && progress.position == progress.amount
    }


    fun hasBody(): Boolean {
        return this.exists() && this.isTerminal().not()
    }

    fun requireHasBody() = require(hasBody()) { "$this has no body" }

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
        if (this.exists()) {
            require(range.begin >= 0 && range.end >= 0)
            require(progress.position >= 0 && progress.amount >= 1)
            if (this.isTerminal()) {
                require(body == null)
            } else {
                require(body != null)
                require(body!!.size == range.length())
            }
        }
        if (this.notExists()) {
            require(range.begin == -1L && range.end == -1L)
            require(body == null)
            if (this.isTerminal()) {
                require(progress.position == 1 && progress.amount == 1)
            } else {
                require(progress.position == 0 && progress.amount == 1)
            }
        }
    }


    override fun copy(): ByteArray {
        requireHasBody()
        requireAvailable()
        val body = this.body!!
        return body.copyOf()
    }

    override fun move(): ByteArray {
        requireHasBody()
        requireAvailable()
        val body = this.body!!
        invalidate()
        return body
    }


    override fun toString(): String {
        return "ThinKfr(kfr=${Kfr(this)}, range=$range, progress=$progress, body=$body)"
    }


    private var available = true

    private fun requireAvailable() {
        require(available) { "available failed, $this" }
    }

    private fun invalidate() {
        requireAvailable()
        available = false
        body = null
    }

}


fun newThinKfr(kfr: Kfr, range: Range, progress: Progress, body: ByteArray?): ThinKfr {
    if (progress.isTerminal()) {
        return terminalThinKfr(kfr, progress.amount)
    }
    if (kfr.exists()) {
        return presentThinKfr(kfr, range, progress, body ?: byteArrayOf())
    } else {
        return absentThinKfr(kfr)
    }
}

fun presentThinKfr(kfr: Kfr, range: Range, progress: Progress, body: ByteArray): ThinKfr {
    kfr.requirePresent()
    return ThinKfrObj(kfr, range, progress, body)
}

fun absentThinKfr(kfr: Kfr): ThinKfr {
    kfr.ensureAbsent()
    return ThinKfrObj(kfr, Range(-1, -1), Progress(0, 1), null)
}

fun terminalThinKfr(kfr: Kfr, amount: Int): ThinKfr {
    return ThinKfrObj(kfr, Range(kfr.size, kfr.size), Progress(amount, amount), null)
}


fun ThinKfr.merge(file: Path, cleanup: Cleanup = {}): FatKfr? {
    return if (this.exists()) {
        mergeExists(file, cleanup)
    } else {
        mergeNotExists(file, cleanup)
    }
}

private fun ThinKfr.mergeExists(file: Path, cleanup: Cleanup): FatKfr? {
    if (this.isTerminal()) {
        val size = Files.size(file)
        val hash = Hasher.hash(file)

        check(size == this.size) { "the merged fat KFR is broken: $size != ${this.size}" }
        check(hash == this.hash) { "the merged fat KFR is broken：$hash != ${this.hash}" }

        return newFatKfr(Kfr(this), file, cleanup)
    }

    val body = this.move()

    Files.newByteChannel(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { ch ->
        ch.position(range.begin)

        val buf = ByteBuffer.wrap(body)
        do {
            val length = ch.write(buf)
        } while (length != -1 && buf.remaining() != 0)

        check(ch.position() == range.end) { "${ch.position()} != ${range.end} failed" }
        check(buf.position() == range.length()) { "${buf.position()} != ${range.length()}" }
    }

    if (this.isSingle()) {
        val size = Files.size(file)
        val hash = Hasher.hash(file)

        check(size == this.size) { "the merged fat KFR is broken: $size != ${this.size}" }
        check(hash == this.hash) { "the merged fat KFR is broken：$hash != ${this.hash}" }

        return newFatKfr(Kfr(this), file, cleanup)
    } else {
        return null
    }
}

private fun ThinKfr.mergeNotExists(file: Path, cleanup: Cleanup): FatKfr {
    Files.deleteIfExists(file)

    return newFatKfr(this.asPojo(), file, cleanup)
}


private fun IKfr.sliceProgress(position: Int, sliceSize: Int): Progress {
    val amount = if (this.exists()) this.sliceAmount(sliceSize) else 1
    return Progress(position, amount)
}

private fun IKfr.sliceAmount(sliceSize: Int): Int {
    if (this.exists()) {
        return max(ceil(size.toDouble() / sliceSize.toDouble()).toInt(), 1)
    } else {
        return 1
    }
}

private fun IKfr.sliceRange(progress: Progress, sliceSize: Int): Range {
    if (this.exists()) {
        val begin: Long = 1L * progress.position * sliceSize
        val end: Long = min(this.size, begin + sliceSize)
        return Range(begin, end)
    } else {
        return Range(-1, -1)
    }
}

fun FatKfr.slice(position: Int, sliceSize: Int): ThinKfr {
    if (this.exists()) {
        val progress = sliceProgress(position, sliceSize)
        if (progress.isTerminal()) {
            return sliceTerminal(sliceSize)
        }
        val range = this.sliceRange(progress, sliceSize)
        val bytes = this.bytes(range)

        return presentThinKfr(this.asPojo(), range, progress, bytes)
    } else {
        return absentThinKfr(this.asPojo())
    }
}

fun FatKfr.sliceTerminal(sliceSize: Int): ThinKfr {
    return terminalThinKfr(this.asPojo(), this.sliceAmount(sliceSize))
}

fun FatKfr.sliceIter(sliceSize: Int): SliceIter {
    return SliceIter(this, sliceSize)
}

class SliceIter(
    val fat: FatKfr,
    val sliceSize: Int,
) : Iterator<ThinKfr>, AutoCloseable {

    private var progress: Progress = fat.sliceProgress(-1, sliceSize)

    private var channel = (if (fat.exists()) fat.channel() else null)


    private var terminated = false

    override fun hasNext(): Boolean {
        return !terminated
    }

    override fun next(): ThinKfr {
        check(hasNext())
        progress = progress.next()

        return if (fat.exists()) {
            if (progress.isTerminal()) {
                terminated = true
                return nextTerminal()
            }
            if (progress.isSingle()) {
                terminated = true
                return nextSlice()
            }
            return nextSlice()
        } else {
            terminated = true
            absentThinKfr(fat.asPojo())
        }
    }

    private fun nextSlice(): ThinKfr {
        val channel = this.channel!!
        val range = fat.sliceRange(progress, sliceSize)

        val buffer = ByteBuffer.allocate(range.length())
        channel.position(range.begin)
        do {
            val read = channel.read(buffer)
        } while (buffer.hasRemaining() && read != -1)

        return presentThinKfr(fat.asPojo(), range, progress, buffer.array())
    }

    private fun nextTerminal(): ThinKfr {
        return terminalThinKfr(fat.asPojo(), progress.amount)
    }


    override fun close() {
        channel?.close()
    }

}
