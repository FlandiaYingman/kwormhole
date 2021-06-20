package top.anagke.kwormhole

import okio.ByteString.Companion.toByteString
import top.anagke.kio.forEachBlock
import top.anagke.kwormhole.ThinKfr.Range
import top.anagke.kwormhole.util.Hasher
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import kotlin.math.ceil
import kotlin.math.min

/**
 * A [FatKfr] can not transfer through network, because it is too fff...**fat**!
 * However, here's [EmptyThinKfr]. It is thin enough, can easily break through size restrictions on the network.
 *
 * [EmptyThinKfr] is usually created by [FatKfr.forEachSlice].
 *
 * If the thin KFR is transferred through HTTP, here's some conventions: transfer each thin KFR by a single HTTP
 * request or response; use the media type `multipart/form-data`; serialize all field with their name in code into
 * the form; serialize `kfr` to JSON, `number` and `count` to literal, `range` using [Range.toString] and [Range.parse],
 * `body` as-is.
 *
 * @property kfr the KFR, which marks the unique slice sequence
 * @property number represents where this thin KFR is in the slice sequence, start by 0
 * @property total represents how many thin KFRs are in the slice sequence
 * @property range represents where the body is sliced from the original fat KFR's body
 * @property body represents the body of this thin KFR, it is a part from the original fat KFR's body
 */
data class ThinKfr(
    val kfr: Kfr,
    val number: Int,
    val total: Int,
    val range: Range,
    val body: ByteArray,
) {

    init {
        check(0 <= range.begin) { "0 <= range.begin <= range.end <= kfr.size: $this" }
        check(range.begin <= range.end) { "0 <= range.begin <= range.end <= kfr.size: $this" }
        check(range.end <= kfr.size) { "0 <= range.begin <= range.end <= kfr.size: $this" }
        check(number <= total) { "number <= total: $this" }
        check(!(isSingle() && isTermination())) { "single and termination are mutex: $this" }
    }

    data class Range(
        val begin: Long,
        val end: Long
    ) {
        fun len() = end - begin
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThinKfr

        if (kfr != other.kfr) return false
        if (range != other.range) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kfr.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "ThinKfr(kfr=$kfr, number=$number, count=$total, range=$range)"
    }

}


internal fun EmptyThinKfr(kfr: Kfr): ThinKfr {
    return ThinKfr(kfr, 0, 1, Range(0, kfr.size), byteArrayOf())
}


fun ThinKfr.isSingle(): Boolean {
    return total == 1
}

fun ThinKfr.isTermination(): Boolean {
    return number == total
}


fun FatKfr.forEachSlice(slice: Int, block: (ThinKfr) -> Unit) {
    if (notExists()) {
        block(EmptyThinKfr(kfr))
        return
    }
    var pos = 0L
    var number = 0
    val count = ceil(kfr.size / slice.toDouble()).toInt()
    this.stream()!!.forEachBlock(slice) { buf, bufLen ->
        block(ThinKfr(kfr, number, count, Range(pos, pos + bufLen), buf.copyOf(bufLen)))
        pos += bufLen
        number++
    }
    if (kfr.size > slice) {
        block(ThinKfr(kfr, number, count, Range(kfr.size, kfr.size), byteArrayOf()))
    }
}

fun FatKfr.slice(slice: Int, number: Int): ThinKfr {
    val total = ceil(kfr.size / slice.toDouble()).toInt()

    check(number <= total)
    if (this.notExists() && number == 0) return EmptyThinKfr(kfr)
    if (number == total) {
        return ThinKfr(kfr, number, total, Range(kfr.size, kfr.size), byteArrayOf())
    }
    val pos = (number * slice).toLong()
    val range = Range(pos, min(pos + slice, kfr.size))
    val part = this.part(range)
    return ThinKfr(kfr, number, total, range, part!!)
}

fun ThinKfr.merge(): FatKfr {
    check(isSingle())
    return FatKfr(kfr, body.toByteString())
}

fun ThinKfr.merge(file: Path, cleanup: () -> Unit = {}): FatKfr? {
    Files.newByteChannel(file, CREATE, WRITE).use { ch ->
        ch.position(range.begin)
        ch.write(ByteBuffer.wrap(body))

        // Just for assertion
        val pos = ch.position()
        require(pos == range.end) { "$pos != ${range.end}" }
    }

    if (isTermination()) {
        val size = Files.size(file)
        val hash = Hasher.hash(file)

        //TODO: Throw specific exception
        require(size == kfr.size)
        require(hash == kfr.hash)
        return FatKfr(kfr, file, cleanup)
    } else {
        return null
    }
}