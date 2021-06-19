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

/**
 * A [FatKfr] can not transfer through network, because it is too fff...**fat**!
 * However, here's [ThinKfr]. It is thin enough, can easily break through size restrictions on the network.
 *
 * [ThinKfr] is usually created by [FatKfr.forEachSlice].
 *
 * If the thin KFR is transferred through HTTP, here's some conventions: transfer each thin KFR by a single HTTP
 * request or response; use the media type `multipart/form-data`; serialize all field with their name in code into
 * the form; serialize `kfr` to JSON, `number` and `count` to literal, `range` using [Range.toString] and [Range.parse],
 * `body` as-is.
 *
 * @property kfr the KFR, which marks the unique slice sequence
 * @property number represents where this thin KFR is in the slice sequence, start by 0
 * @property count represents how many thin KFRs are in the slice sequence
 * @property range represents where the body is sliced from the original fat KFR's body
 * @property body represents the body of this thin KFR, it is a part from the original fat KFR's body
 */
data class ThinKfr
internal constructor(
    val kfr: Kfr,
    val number: Int,
    val count: Int,
    val range: Range,
    val body: ByteArray,
) {

    data class Range(
        val begin: Long,
        val end: Long
    ) {

        companion object {
            fun parse(str: String): Range {
                val split = str.split("-")
                val begin = split[0].toLong()
                val end = split[1].toLong()
                return Range(begin, end)
            }
        }

        override fun toString(): String {
            return "$begin-$end"
        }

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

}


internal fun ThinKfr(kfr: Kfr): ThinKfr {
    return ThinKfr(kfr, 0, 1, Range(kfr.size, kfr.size), byteArrayOf())
}


fun ThinKfr.isSingle(): Boolean {
    return count == 1
}

fun ThinKfr.isTermination(): Boolean {
    return number == count
}


fun FatKfr.forEachSlice(slice: Int, block: (ThinKfr) -> Unit) {
    if (this.notExists()) {
        block(ThinKfr(kfr))
        return
    }

    var pos = 0L
    var number = 0
    val count = (kfr.size / slice).toInt() + 1
    this.stream()!!.forEachBlock(slice) { buf, len ->
        block(ThinKfr(kfr, number, count, Range(pos, pos + len), buf))
        pos += len
        number++
    }
}

fun ThinKfr.merge(): FatKfr {
    check(isSingle())
    return FatKfr(kfr, body.toByteString())
}

fun ThinKfr.merge(file: Path): FatKfr? {
    Files.newByteChannel(file, CREATE, WRITE).use { ch ->
        ch.position(range.begin)
        ch.write(ByteBuffer.wrap(body))

        // Just for assertion
        require(ch.position() == range.end)
    }

    if (isTermination()) {
        val size = Files.size(file)
        val hash = Hasher.hash(file)

        //TODO: Throw specific exception
        require(size == kfr.size)
        require(hash == kfr.hash)
        return FatKfr(kfr, file)
    } else {
        return null
    }
}