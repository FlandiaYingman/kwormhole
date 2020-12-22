package top.anagke.kwormhole.util

import net.jpountz.xxhash.XXHashFactory
import java.io.File

fun File.hash(): Long {
    val hash = XXHashFactory.fastestInstance().newStreamingHash64(0)
    this.forEachBlock { bytes, length ->
        hash.update(bytes, 0, length)
    }
    return hash.value
}

fun File.requireHashEquals(hash: Long) {
    val thisHash = this.hash()
    require(thisHash == hash) { "The hash of $this ($thisHash) should be $hash" }
}

fun ByteArray.hash(): Long {
    val hash = XXHashFactory.fastestInstance().hash64()
    return hash.hash(this, 0, this.size, 0)
}