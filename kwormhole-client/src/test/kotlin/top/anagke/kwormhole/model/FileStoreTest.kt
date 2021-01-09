package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.nextBytesList
import top.anagke.kwormhole.nextPath
import top.anagke.kwormhole.store.FileStore
import top.anagke.kwormhole.store.KwormFile
import top.anagke.kwormhole.store.KwormFile.Companion.utcTimeMillis
import top.anagke.kwormhole.util.KiB
import top.anagke.kwormhole.util.hash
import top.anagke.kwormhole.util.use
import java.io.File
import kotlin.random.Random

internal class FileStoreTest {

    companion object {
        private val RANDOM_TIMES = 10
        private val DATA_LENGTH = 10.KiB
        private val PATH_MAX_DEPTH = 6

        private val TEST_DIR = File("TEST")
    }

    @Test
    fun testStore() {
        val testBytesList = Random.nextBytesList(RANDOM_TIMES, DATA_LENGTH)
        val testKwormList = List(RANDOM_TIMES) {
            val path = Random.nextPath(PATH_MAX_DEPTH)
            val hash = testBytesList[it].hash()
            val time = Random.nextLong(utcTimeMillis)
            KwormFile(path, hash, time)
        }
        TEST_DIR.mkdirs()
        TEST_DIR.use { _ ->
            FileStore(TEST_DIR).use { fileStore ->
                repeat(RANDOM_TIMES) { i ->
                    val file = fileStore.resolve(testKwormList[i].path)
                    file.parentFile.mkdirs()
                    file.writeBytes(testBytesList[i])
                }

                repeat(RANDOM_TIMES) { i ->
                    fileStore.store(testKwormList[i])
                }
                val storingRange = 0..utcTimeMillis

                repeat(RANDOM_TIMES) {
                    val expected = testKwormList[it]
                    val actual = fileStore.find(testKwormList[it].path)!!
                    assertEquals(expected.path, actual.path)
                    assertEquals(expected.hash, actual.hash)
                    assertTrue(actual.time in storingRange)

                    val expectedBytes = testBytesList[it]
                    val actualBytes = fileStore.resolve(actual.path).readBytes()
                    assertArrayEquals(expectedBytes, actualBytes)
                }
            }
        }
    }

    @Test
    fun testStoreBytes() {
        val testBytesList = Random.nextBytesList(RANDOM_TIMES, DATA_LENGTH)
        val testKwormList = List(RANDOM_TIMES) {
            val path = Random.nextPath(PATH_MAX_DEPTH)
            val hash = testBytesList[it].hash()
            val time = Random.nextLong(utcTimeMillis)
            KwormFile(path, hash, time)
        }
        TEST_DIR.mkdirs()
        TEST_DIR.use { _ ->
            FileStore(TEST_DIR).use { fileStore ->
                repeat(RANDOM_TIMES) {
                    val bytes = testBytesList[it]
                    val kworm = testKwormList[it]
                    fileStore.storeBytes(bytes, kworm)
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
    }

    @Test
    fun testUpdate() {
        val testBytesList = Random.nextBytesList(RANDOM_TIMES, DATA_LENGTH)
        val testModifiedBytesList = Random.nextBytesList(RANDOM_TIMES, DATA_LENGTH)
        val testKwormList = List(RANDOM_TIMES) {
            val path = Random.nextPath(PATH_MAX_DEPTH)
            val hash = testBytesList[it].hash()
            val time = Random.nextLong(utcTimeMillis)
            KwormFile(path, hash, time)
        }
        TEST_DIR.mkdirs()
        TEST_DIR.use { _ ->
            FileStore(TEST_DIR).use { fileStore ->
                //Store
                repeat(RANDOM_TIMES) {
                    val bytes = testBytesList[it]
                    val kworm = testKwormList[it]
                    fileStore.storeBytes(bytes, kworm)
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
                    assertTrue(modified.time > unmodified.time)

                    val expectedBytes = testModifiedBytesList[it]
                    val actualBytes = fileStore.resolve(modified.path).readBytes()
                    assertArrayEquals(expectedBytes, actualBytes)
                }
            }
        }
    }

}