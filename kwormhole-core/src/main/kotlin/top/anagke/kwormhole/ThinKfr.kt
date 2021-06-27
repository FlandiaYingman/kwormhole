package top.anagke.kwormhole

import okio.ByteString
import top.anagke.kwormhole.util.Hasher
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import kotlin.math.ceil
import kotlin.math.min

/**
 * A [FatKfr] can not transfer through network, because it is too fat!
 *
 * However, here's [ThinKfr]. A [FatKfr] can be split into multiple
 * [ThinKfr] parts. The parts can be transferred through network in
 * sequence or parallel. Then the parts can be merged into the original
 * [FatKfr].
 */
interface ThinKfr : IKfr {

    /** Represents which part is this [ThinKfr] from the [FatKfr]. */
    val range: Range

    /** Represents the start and the end this [ThinKfr] is. */
    val progress: Fraction

    /** The body of this [ThinKfr]. */
    val body: ByteString?


    fun body(): ByteString {
        ensurePresent()
        return body!!
    }

    fun isStandalone(): Boolean {
        return progress.denominator == 1L
    }

    fun isTermination(): Boolean {
        return progress.denominator == progress.numerator || isStandalone()
    }

}

private data class ThinKfrObj(
    override val path: String,
    override val time: Long,
    override val size: Long,
    override val hash: Long,
    override val range: Range,
    override val progress: Fraction,
    override val body: ByteString?,
) : ThinKfr {

    constructor(
        kfr: Kfr,
        range: Range,
        progress: Fraction,
        body: ByteString?
    ) : this(kfr.path, kfr.time, kfr.size, kfr.hash, range, progress, body)

    init {
        require(progress.numerator <= progress.denominator) {
            "progress.numerator <= progress.denominator failed, $this"
        }
    }

    override fun toString(): String {
        return "ThinKfr(kfr=${Kfr(this)}, range=$range, progress=$progress, body=$body)"
    }

}


fun ThinKfr(kfr: Kfr, range: Range, progress: Fraction, body: ByteString): ThinKfr {
    kfr.ensurePresent()
    return ThinKfrObj(kfr, range, progress, body)
}

fun ThinKfr(kfr: Kfr): ThinKfr {
    kfr.ensureAbsent()
    return ThinKfrObj(kfr, Range(0, kfr.size), Fraction(0, 1), null)
}

fun IKfr.total(sliceSize: Int): Long {
    val doubleTotal = size.toDouble() / sliceSize.toDouble()
    val ceilingTotal = ceil(doubleTotal).toLong()
    return ceilingTotal
}

fun FatKfr.forEachSlice(sliceSize: Int, block: (ThinKfr) -> Unit) {
    if (this.notExists()) {
        block(ThinKfr(Kfr(this)))
        return
    }
    val total = this.total(sliceSize)
    repeat(Math.toIntExact(total)) { number ->
        val slice = this.slice(sliceSize, number.toLong())
        block(slice)
    }
    if (total > 1) {
        val termination = this.slice(sliceSize, total)
        block(termination)
    }
}

fun FatKfr.slice(sliceSize: Int, number: Long): ThinKfr {
    if (this.notExists()) {
        return ThinKfr(Kfr(this))
    }
    val total = this.total(sliceSize)
    val progress = Fraction(number, total)
    if (number == total) {
        return ThinKfr(Kfr(this), Range(size, size), progress, ByteString.EMPTY)
    } else {
        val begin = (number * sliceSize)
        val end = begin + sliceSize
        val range = Range(begin, min(end, size))
        return ThinKfr(Kfr(this), range, progress, bytes(range))
    }
}

fun ThinKfr.merge(): FatKfr {
    check(isStandalone())
    val buffer = body()
    //TODO: Check Legal
    return FatKfr(Kfr(this), buffer)
}

fun ThinKfr.merge(file: Path): FatKfr? {
    Files.newByteChannel(file, CREATE, WRITE).use { ch ->
        ch.position(range.begin)

        val buf = body().asByteBuffer()
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

    if (isTermination()) {
        val size = Files.size(file)
        val hash = Hasher.hash(file)

        //TODO: Throw specific exception
        require(size == this.size)
        require(hash == this.hash)

        return FatKfr(Kfr(this), file)
    } else {
        return null
    }
}