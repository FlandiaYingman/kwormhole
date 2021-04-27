package top.anagke.kwormhole

import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import top.anagke.kwormhole.sync.Synchronizer
import top.anagke.kwormhole.sync.Synchronizer.utcEpochMillis
import top.anagke.kwormhole.util.Hasher
import top.anagke.kwormhole.util.fromJson
import java.io.File
import java.math.BigInteger
import kotlin.random.Random

const val TEST_UNIT_TIMES = 8
const val TEST_DATA_LENGTH = 64

val TEST_DIR = File("./test")

val TEST_SERVER_PORT = Random.nextInt(60000, 65536)


fun testServer(module: Application.() -> Unit): CIOApplicationEngine {
    return embeddedServer(CIO, port = TEST_SERVER_PORT, module = module)
}

fun ApplicationEngine.use(block: () -> Unit) {
    try {
        start()
        block()
    } finally {
        stop(0, 0)
    }
}


inline fun <T> File.useDir(block: (File) -> T): T {
    try {
        this.deleteRecursively()
        this.mkdirs()
        return block(this)
    } finally {
        this.deleteRecursively()
    }
}


fun Random.nextHexString(length: Int = 8): String {
    return this.nextBytes(length * 16).let {
        BigInteger(1, it).toString(16).takeLast(length)
    }
}

fun Random.nextFilePath(depth: Int = 4, length: Int = 8): String {
    return List(this.nextInt(1, 1 + depth)) { "/${this.nextHexString(length)}" }.joinToString("")
}

fun Random.nextFileRC(): Pair<FileRecord, FileContent> {
    val bytes = nextBytes(64)
    val content = MemoryFileContent(bytes)

    val path = "/${nextHexString(8)}"
    val size = bytes.size.toLong()
    val time = nextLong(0, utcEpochMillis)
    val hash = Hasher.hash(bytes)
    val record = FileRecord(path, size, time, hash)

    return record to content
}

fun randomFileRecord(): FileRecord {
    val path = Random.nextHexString(8)
    val size = Random.nextLong(Int.MAX_VALUE.toLong())
    val time = Random.nextLong(Synchronizer.utcEpochMillis)
    val hash = Random.nextLong()
    return FileRecord(path, size, time, hash)
}


object Json {

    val gson = Gson()

    inline fun <reified T> toJson(obj: T?): String {
        return gson.toJson(obj)
    }

    inline fun <reified T> fromJson(json: String): T? {
        return gson.fromJson<T>(json)
    }

}