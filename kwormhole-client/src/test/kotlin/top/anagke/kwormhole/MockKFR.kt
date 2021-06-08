package top.anagke.kwormhole

import top.anagke.kio.bytes
import top.anagke.kwormhole.KFR.Companion.recordAsKFR
import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.test.nextHexString
import top.anagke.kwormhole.util.Hasher
import java.io.File
import kotlin.random.Random

object MockKFR {

    fun mockString(): String {
        return Random.nextHexString(8)
    }

    fun mockFile(parent: File): File {
        return parent.resolve(mockString()).also { it.createNewFile() }
    }

    fun mockRecord(): KFR {
        val path = List(Random.nextInt(4, 8)) { "/${mockString()}" }.joinToString("")
        val size = Random.nextLong(Int.MAX_VALUE.toLong())
        val time = utcEpochMillis
        val hash = Random.nextLong()
        return KFR(path, time, size, hash)
    }

    fun mockOnRandomFile(root: File): Pair<File, KFR> {
        val mockFile = mockFile(root)

        val kfr = mockFile.recordAsKFR(root)
        return (mockFile to kfr)
    }

    fun mockOnFile(root: File, file: File): KFR {
        val randomBytes = Random.nextBytes(64)
        file.bytes = randomBytes

        val kfr = file.recordAsKFR(root)
        return kfr
    }

    fun mockPath(): String {
        return List(Random.nextInt(4, 8)) { "/${mockString()}" }.joinToString("")
    }


    fun mockPair(): Pair<KFR, ByteArray> {
        val bytes = Random.nextBytes(64)

        val path = mockPath()
        val time = utcEpochMillis
        val size = bytes.size.toLong()
        val hash = Hasher.hash(bytes)

        return (KFR(path, time, size, hash) to bytes)
    }

}