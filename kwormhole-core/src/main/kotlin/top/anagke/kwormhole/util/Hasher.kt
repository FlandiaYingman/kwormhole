package top.anagke.kwormhole.util

import net.jpountz.xxhash.XXHashFactory
import java.io.File

object Hasher {

    private const val HASH_SEED = 0L

    val emptyHash: Long = hash(byteArrayOf())


    fun hash(bytes: ByteArray): Long {
        val xxHasher = XXHashFactory.fastestInstance().hash64()
        return xxHasher.hash(bytes, 0, bytes.size, HASH_SEED)
    }

    fun hash(file: File): Long {
        val xxHasher = XXHashFactory.fastestInstance().newStreamingHash64(HASH_SEED)
        file.forEachBlock { bytes, length ->
            xxHasher.update(bytes, 0, length)
        }
        return xxHasher.value
    }

}