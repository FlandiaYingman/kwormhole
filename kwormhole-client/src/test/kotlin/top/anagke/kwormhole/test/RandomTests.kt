package top.anagke.kwormhole.test

import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.MemoryFileContent
import top.anagke.kwormhole.sync.Synchronizer.utcEpochMillis
import top.anagke.kwormhole.util.Hasher
import java.math.BigInteger
import kotlin.random.Random

fun Random.nextHexString(length: Int): String {
    return this.nextBytes(length * 16).let {
        BigInteger(1, it).toString(16).takeLast(length)
    }
}

fun Random.nextFilePath(depth: Int, length: Int): String {
    return List(this.nextInt(1, 1 + depth)) { "/${this.nextHexString(length)}" }.joinToString("")
}

fun Random.nextFileRC(): Pair<FileRecord, FileContent> {
    val bytes = nextBytes(TEST_ENTRY_LENGTH)
    val content = MemoryFileContent(bytes)

    val path = "/${nextHexString(TEST_FILE_NAME_LENGTH)}"
    val size = bytes.size.toLong()
    val time = nextLong(0, utcEpochMillis)
    val hash = Hasher.hash(bytes)
    val record = FileRecord(path, size, time, hash)

    return record to content
}

fun Random.nextFileRecord(): FileRecord {
    val path = nextHexString(TEST_FILE_NAME_LENGTH)
    val size = nextLong(Int.MAX_VALUE.toLong())
    val time = nextLong(utcEpochMillis)
    val hash = nextLong()
    return FileRecord(path, size, time, hash)
}