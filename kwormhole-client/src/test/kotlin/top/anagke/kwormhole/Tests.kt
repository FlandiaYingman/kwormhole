package top.anagke.kwormhole

import java.io.File
import kotlin.random.Random


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

fun Random.nextBytesList(listLen: Int, byteArrayLen: Int): List<ByteArray> {
    return List(listLen) { this.nextBytes(byteArrayLen) }
}

fun Random.nextPath(depth: Int): String {
    return List(this.nextInt(1, 1 + depth)) { "/${this.nextInt().toString(16)}" }.joinToString("")
}