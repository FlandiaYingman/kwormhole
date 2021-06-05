package top.anagke.kwormhole

import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.test.nextHexString
import top.anagke.kwormhole.util.Hasher
import java.io.File
import kotlin.random.Random

object MockKfr {

    fun mockString(): String {
        return Random.nextHexString(8)
    }

    fun mockFile(parent: File): File {
        return parent.resolve(mockString()).also { it.createNewFile() }
    }

    fun mockRecord(): FileRecord {
        val path = List(Random.nextInt(4, 8)) { "/${mockString()}" }.joinToString("")
        val size = Random.nextLong(Int.MAX_VALUE.toLong())
        val time = utcEpochMillis
        val hash = Random.nextLong()
        return FileRecord(path, size, time, hash)
    }

    fun mockBoth(): Pair<FileRecord, FileContent> {
        val bytes = Random.nextBytes(64)
        val content = MemoryFileContent(bytes)

        val path = List(Random.nextInt(4, 8)) { "/${mockString()}" }.joinToString("")
        val size = bytes.size.toLong()
        val time = Random.nextLong(0, utcEpochMillis)
        val hash = Hasher.hash(bytes)
        val record = FileRecord(path, size, time, hash)

        return record to content
    }

}