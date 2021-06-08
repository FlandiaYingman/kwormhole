package top.anagke.kwormhole

import top.anagke.kio.bytes
import java.io.File
import java.math.BigInteger
import kotlin.random.Random

fun Random.nextHexString(length: Int): String {
    return this.nextBytes(length * 16).let {
        BigInteger(1, it).toString(16).takeLast(length)
    }
}

fun mockString(): String {
    return Random.nextHexString(8)
}

fun mockFile(parent: File, size: Int): File {
    val mock = parent.resolve(mockString())
    mock.bytes = Random.nextBytes(size)
    return mock
}