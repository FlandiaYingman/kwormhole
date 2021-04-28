package top.anagke.kwormhole.test

import org.junit.jupiter.api.fail
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.random.Random

const val TEST_ENTRY_COUNT = 8
const val TEST_ENTRY_LENGTH = 64

const val TEST_FILE_NAME_LENGTH = 8
const val TEST_FILE_PATH_DEPTH = 8

val TEST_DIR = File("./test")
val TEST_SERVER_PORT = Random.nextInt(60000, 65536)

fun <T> BlockingQueue<T>.tryPoll(): T {
    val timeout: Long = 1000
    return poll(timeout, TimeUnit.MILLISECONDS) ?: fail("timeout in tryPoll(), $timeout ms")
}