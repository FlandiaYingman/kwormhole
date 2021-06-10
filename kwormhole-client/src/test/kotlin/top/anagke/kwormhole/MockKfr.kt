package top.anagke.kwormhole

import top.anagke.kio.bytes
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

    fun mockRecord(): Kfr {
        val path = List(Random.nextInt(4, 8)) { "/${mockString()}" }.joinToString("")
        val size = Random.nextLong(Int.MAX_VALUE.toLong())
        val time = utcEpochMillis
        val hash = Random.nextLong()
        return Kfr(path, time, size, hash)
    }

    fun mockOnRandomFile(root: File): Pair<File, Kfr> {
        val mockFile = mockFile(root)

        val kfr = Kfr(root, mockFile)
        return (mockFile to kfr)
    }

    fun mockOnFile(root: File, file: File): Kfr {
        val randomBytes = Random.nextBytes(64)
        file.bytes = randomBytes

        val kfr = Kfr(root, file)
        return kfr
    }

    fun mockPath(): String {
        return List(Random.nextInt(4, 8)) { "/${mockString()}" }.joinToString("")
    }


    fun mockPair(): Pair<Kfr, ByteArray> {
        val bytes = Random.nextBytes(64)

        val path = mockPath()
        val time = utcEpochMillis
        val size = bytes.size.toLong()
        val hash = Hasher.hash(bytes)

        return (Kfr(path, time, size, hash) to bytes)
    }

}