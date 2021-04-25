package top.anagke.kwormhole

import top.anagke.kwormhole.sync.Synchronizer
import java.io.File
import java.math.BigInteger
import kotlin.random.Random

const val TEST_UNIT_NUM = 8

val TEST_DIR = File("./test")

inline fun <T> File.useDir(block: (File) -> T): T {
    try {
        this.deleteRecursively()
        this.mkdirs()
        return block(this)
    } finally {
        this.deleteRecursively()
    }
}

fun Random.nextHexString(byteLength: Int): String {
    return this.nextBytes(byteLength).let {
        BigInteger(1, it).toString(16)
    }
}

fun randomFileRecord(): FileRecord {
    val path = Random.nextHexString(8).let { "$it.test" }
    val size = Random.nextLong(Int.MAX_VALUE.toLong())
    val time = Random.nextLong(Synchronizer.utcEpochMillis)
    val hash = Random.nextLong()
    return FileRecord(path, size, time, hash)
}