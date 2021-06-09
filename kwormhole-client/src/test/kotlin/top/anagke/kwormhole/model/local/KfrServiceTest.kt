package top.anagke.kwormhole.model.local

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import top.anagke.kio.bytes
import top.anagke.kio.createDir
import top.anagke.kio.deleteDir
import top.anagke.kio.deleteFile
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.test.TEST_DIR
import java.io.File
import kotlin.random.Random

internal class KfrServiceTest {

    private val testRoot: File
        get() {
            val rootFile = TEST_DIR.resolve("root")
            rootFile.createDir()
            return rootFile
        }

    private val testDatabase: KfrDatabase
        get() {
            val databaseFile = TEST_DIR.resolve("test.db")
            databaseFile.deleteFile()
            databaseFile.deleteOnExit()
            return KfrDatabase(databaseFile)
        }

    @BeforeEach
    fun setUp() {
        TEST_DIR.createDir()
    }

    @AfterEach
    fun tearDown() {
        TEST_DIR.deleteDir()
    }


    @Test
    fun sync_get() {
        val fileCount = 1024
        val fileSize = 64 * 1024
        val root = testRoot
        val database = testDatabase
        KfrService(root, database).use { kfrService ->
            val mockFiles = List(fileCount) { MockKfr.mockOnRandomFile(testRoot) }
            val mockKfrs = mockFiles.map { (_, kfr) -> kfr }
            mockFiles.forEach { (file, _) -> file.bytes = Random.nextBytes(fileSize) }

            kfrService.sync(mockKfrs.map(Kfr::path))
            mockKfrs.forEach { mockKfr ->
                val (kfr, file) = kfrService.get(mockKfr.path)
                Assertions.assertNotNull(kfr)
                Assertions.assertTrue(kfr!!.isValidTo(mockKfr))
            }
        }
    }

    @Test
    fun put_get() {
        val fileCount = 1024
        val fileSize = 64 * 1024
        val root = testRoot
        val database = testDatabase
        KfrService(root, database).use { kfrService ->
            val mockPairs = List(fileCount) { MockKfr.mockPair() }
            mockPairs.forEach { (kfr, bytes) ->
                kfrService.put(kfr, bytes)
            }
            mockPairs.forEach { (mockKfr, mockBytes) ->
                val (kfr, file) = kfrService.get(mockKfr.path)
                Assertions.assertNotNull(kfr)
                Assertions.assertEquals(mockKfr, kfr)
                Assertions.assertArrayEquals(mockBytes, file!!.bytes)
            }
        }
    }

}