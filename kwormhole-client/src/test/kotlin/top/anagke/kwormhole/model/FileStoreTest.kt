package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.nextBytesList
import top.anagke.kwormhole.nextPath
import top.anagke.kwormhole.store.FileMetadata
import top.anagke.kwormhole.store.FileStore
import top.anagke.kwormhole.store.KwormFile
import top.anagke.kwormhole.util.*
import java.io.File
import kotlin.random.Random

internal class FileStoreTest {

    companion object {
        private const val RANDOM_TIMES = 12
        private const val PATH_MAX_DEPTH = 6
        private val TEST_DIR = File("TEST")
    }

    @Test
    fun testStoreFind() {
        val testBytesList = Random.nextBytesList(RANDOM_TIMES, 4.KiB)
        val testKwormList = List(RANDOM_TIMES) {
            val path = Random.nextPath(PATH_MAX_DEPTH)
            val hash = testBytesList[it].hash()
            val updateTime = Random.nextLong(KwormFile.utcTimeMillis)
            KwormFile(path, FileMetadata(hash, updateTime))
        }
        TEST_DIR.mkdirs()
        TEST_DIR.use { _ ->
            val fileStore = FileStore(TEST_DIR)
            repeat(RANDOM_TIMES) {
                val bytes = testBytesList[it]
                val kworm = testKwormList[it]
                fileStore.store(bytes, kworm)
            }
            repeat(RANDOM_TIMES) {
                val expected = testKwormList[it]
                val actual = fileStore.find(testKwormList[it].path)!!
                assertEquals(expected, actual)

                val expectedBytes = testBytesList[it]
                val actualBytes = fileStore.resolve(actual.path).readBytes()
                assertArrayEquals(expectedBytes, actualBytes)
            }
        }
    }

    @Test
    fun testStorePartFind() {
        val testBytesList = Random.nextBytesList(RANDOM_TIMES, 16.MiB)
        val testKwormList = List(RANDOM_TIMES) {
            val path = Random.nextPath(PATH_MAX_DEPTH)
            val hash = testBytesList[it].hash()
            val updateTime = Random.nextLong(KwormFile.utcTimeMillis)
            KwormFile(path, FileMetadata(hash, updateTime))
        }
        TEST_DIR.mkdirs()
        TEST_DIR.use { _ ->
            val fileStore = FileStore(TEST_DIR)
            repeat(RANDOM_TIMES) {
                val bytes = testBytesList[it]
                val kworm = testKwormList[it]
                var total = 0L
                bytes.forEachBlock(4.MiB) { buf, read ->
                    fileStore.storePart(buf, total until total + read, kworm.path)
                    total += read
                }
                fileStore.storePart(kworm)
            }
            repeat(RANDOM_TIMES) {
                val expected = testKwormList[it]
                val actual = fileStore.find(testKwormList[it].path)!!
                assertEquals(expected, actual)

                val expectedBytes = testBytesList[it]
                val actualBytes = fileStore.resolve(actual.path).readBytes()
                assertArrayEquals(expectedBytes, actualBytes)
            }
        }
    }

    @Test
    fun testStoreExistingFind() {
        val testBytesList = Random.nextBytesList(RANDOM_TIMES, 16.MiB)
        val testKwormList = List(RANDOM_TIMES) {
            val path = Random.nextPath(PATH_MAX_DEPTH)
            val hash = testBytesList[it].hash()
            val updateTime = Random.nextLong(KwormFile.utcTimeMillis)
            KwormFile(path, FileMetadata(hash, updateTime))
        }
        TEST_DIR.mkdirs()
        TEST_DIR.use { _ ->
            val fileStore = FileStore(TEST_DIR)
            repeat(RANDOM_TIMES) { i ->
                val file = fileStore.resolve(testKwormList[i].path)
                file.parentFile.mkdirs()
                file.writeBytes(testBytesList[i])
            }

            val startStoringTime = KwormFile.utcTimeMillis
            repeat(RANDOM_TIMES) { i ->
                fileStore.storeExisting(testKwormList[i].path)
            }
            val endStoringTime = KwormFile.utcTimeMillis
            val storingRange = startStoringTime..endStoringTime

            repeat(RANDOM_TIMES) {
                val expected = testKwormList[it]
                val actual = fileStore.find(testKwormList[it].path)!!
                assertEquals(expected.path, actual.path)
                assertEquals(expected.hash, actual.hash)
                assertTrue(actual.updateTime in storingRange)

                val expectedBytes = testBytesList[it]
                val actualBytes = fileStore.resolve(actual.path).readBytes()
                assertArrayEquals(expectedBytes, actualBytes)
            }
        }
    }

    @Test
    fun testUpdateFind() {
        val testBytesList = Random.nextBytesList(RANDOM_TIMES, 4.KiB)
        val testModifiedBytesList = Random.nextBytesList(RANDOM_TIMES, 4.KiB)
        val testKwormList = List(RANDOM_TIMES) {
            val path = Random.nextPath(PATH_MAX_DEPTH)
            val hash = testBytesList[it].hash()
            val updateTime = Random.nextLong(KwormFile.utcTimeMillis)
            KwormFile(path, FileMetadata(hash, updateTime))
        }
        TEST_DIR.mkdirs()
        TEST_DIR.use { _ ->
            val fileStore = FileStore(TEST_DIR)
            //Store
            repeat(RANDOM_TIMES) {
                val bytes = testBytesList[it]
                val kworm = testKwormList[it]
                fileStore.store(bytes, kworm)
            }
            //Modify & Update
            repeat(RANDOM_TIMES) {
                fileStore.resolve(testKwormList[it].path).writeBytes(testModifiedBytesList[it])
                fileStore.update(testKwormList[it])
            }
            //Assert
            repeat(RANDOM_TIMES) {
                val unmodified = testKwormList[it]
                val modified = fileStore.find(testKwormList[it].path)!!
                assertNotEquals(unmodified.hash, modified.hash)
                assertTrue(modified.updateTime > unmodified.updateTime)

                val expectedBytes = testModifiedBytesList[it]
                val actualBytes = fileStore.resolve(modified.path).readBytes()
                assertArrayEquals(expectedBytes, actualBytes)
            }
        }
    }

}