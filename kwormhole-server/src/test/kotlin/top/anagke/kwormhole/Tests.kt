package top.anagke.kwormhole

import java.io.File

val TEST_DIR = File("./test")

fun <T> pollNonnull(block: () -> T): T {
    do {
        val result = block()
        if (result != null) return result
        Thread.sleep(100)
    } while (result == null)
    throw AssertionError()
}