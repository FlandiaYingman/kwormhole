package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.store.FileMetadata
import top.anagke.kwormhole.store.FileStore
import top.anagke.kwormhole.store.KwormFile
import top.anagke.kwormhole.util.KiB
import top.anagke.kwormhole.util.MiB
import top.anagke.kwormhole.util.forEachBlock
import top.anagke.kwormhole.util.hash
import java.io.File
import kotlin.random.Random

internal class FileStoreTest {

    companion object {
        private const val RANDOM_TIMES = 24
    }

    @Test
    fun test() {
        val testBytesList = listOf(
            List(RANDOM_TIMES) { Random.nextBytes(4.KiB) },
            List(RANDOM_TIMES) { Random.nextBytes(16.MiB) }
        ).flatten()
        val testKwormList = List(RANDOM_TIMES * 2) {
            val path = List(Random.nextInt(1, 9)) { "/${Random.nextInt().toString(16)}" }.joinToString("")
            val hash = testBytesList[it].hash()
            val updateTime = Random.nextLong()
            KwormFile(path, FileMetadata(hash, updateTime))
        }

        val testPath = File("test")
        try {
            val fileStore = FileStore(testPath)
            repeat(RANDOM_TIMES * 2) {
                val bytes = testBytesList[it]
                val kworm = testKwormList[it]
                if (bytes.size <= 4.MiB) {
                    fileStore.store(bytes, kworm)
                } else {
                    var total = 0L
                    bytes.forEachBlock(4.MiB) { buf, read ->
                        fileStore.storePart(buf, total until total + read, kworm.path)
                        total += read
                    }
                    fileStore.storePart(kworm)
                }
            }
            repeat(RANDOM_TIMES * 2) {
                assertTrue(fileStore.exists(testKwormList[it].path))
            }
            repeat(RANDOM_TIMES * 2) {
                val (expectedPath, expectedMetadata) = fileStore.find(testKwormList[it].path)
                val (actualPath, actualMetadata) = testKwormList[it]
                assertEquals(expectedPath, actualPath)
                assertEquals(expectedMetadata, actualMetadata)
                val expectedBytes = testBytesList[it]
                val actualBytes = fileStore.resolve(actualPath).readBytes()
                assertArrayEquals(expectedBytes, actualBytes)
            }
            assertEquals(RANDOM_TIMES * 2 + 1, testPath.walk().count(File::isFile))
        } finally {
            testPath.deleteRecursively()
        }
    }

}