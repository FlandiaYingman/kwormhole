package top.anagke.kwormhole

import kotlin.random.Random

fun Random.nextBytesList(listLen: Int, byteArrayLen: Int): List<ByteArray> {
    return List(listLen) { this.nextBytes(byteArrayLen) }
}

fun Random.nextPath(depth: Int): String {
    return List(this.nextInt(1, 1 + depth)) { "/${this.nextInt().toString(16)}" }.joinToString("")
}