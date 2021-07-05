package top.anagke.kwormhole.model.local

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import top.anagke.kio.file.createDir
import top.anagke.kio.file.deleteDir
import top.anagke.kio.file.deleteFile
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.asPojo
import top.anagke.kwormhole.test.TEST_DIR
import java.io.File

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
        val fileCount = 256
        val root = testRoot
        val database = testDatabase
        KfrService(root, database).use { kfrService ->
            val mocks = List(fileCount) { MockKfr.mockOnFile(testRoot) }
            kfrService.sync(mocks.map { (_, mockFile) -> mockFile })

            mocks.forEach { (mockFatKfr, _) ->
                val actualFatKfr = kfrService.get(mockFatKfr.path)
                assertNotNull(actualFatKfr); actualFatKfr!!
                assertTrue(mockFatKfr.equalsContent(actualFatKfr))
                assertEquals(mockFatKfr.bytes(), actualFatKfr.bytes())
            }
        }
    }

    @Test
    fun put_get() {
        val fileCount = 256
        val root = testRoot
        val database = testDatabase
        KfrService(root, database).use { kfrService ->
            val mockFatKfrs = List(fileCount) { MockKfr.mockFatKfr() }
            mockFatKfrs.forEach { mockFatKfr -> kfrService.put(mockFatKfr) }
            mockFatKfrs.forEach { mockFatKfr ->
                val actualFatKfr = kfrService.get(mockFatKfr.path)
                assertNotNull(actualFatKfr); actualFatKfr!!
                assertEquals(mockFatKfr.asPojo(), actualFatKfr.asPojo())
                assertEquals(mockFatKfr.bytes(), actualFatKfr.bytes())
            }
        }
    }

}