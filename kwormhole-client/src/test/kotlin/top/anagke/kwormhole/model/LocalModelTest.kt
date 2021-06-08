package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import top.anagke.kio.bytes
import top.anagke.kio.createDir
import top.anagke.kio.deleteFile
import top.anagke.kio.notExists
import top.anagke.kwormhole.KFR
import top.anagke.kwormhole.MockKFR
import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.test.pollNonnull
import top.anagke.kwormhole.test.useDir
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

@Timeout(5, unit = SECONDS)
internal class LocalModelTest {

    private val testRoot: File
        get() {
            val rootFile = TEST_DIR.resolve("root")
            rootFile.createDir()
            return rootFile
        }

    private val testDatabase: RecordDatabase
        get() {
            val databaseFile = TEST_DIR.resolve("test.db")
            databaseFile.deleteFile()
            databaseFile.deleteOnExit()
            return RecordDatabase(databaseFile)
        }


    @Test
    fun init_fromDisk() {
        TEST_DIR.useDir {
            val (_, expectedKFR) = MockKFR.mockOnRandomFile(testRoot)
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()
                val actualKFR = model.changes.take()
                assertTrue(expectedKFR.contentEquals(actualKFR))
            }
        }
    }

    @Test
    fun init_fromDatabase() {
        TEST_DIR.useDir {
            val (_, expectedKFR) = MockKFR.mockOnRandomFile(testRoot)
            val database = testDatabase
            database.put(listOf(expectedKFR))
            LocalModel(testRoot, database).use { model ->
                model.open()
                val actualKFR = pollNonnull { model.getRecord(expectedKFR.path) }
                assertEquals(expectedKFR, actualKFR)
            }
        }
    }

    @Test
    fun init_fromDatabaseButDiskChanged() {
        TEST_DIR.useDir {
            val (testFile, testKFR) = MockKFR.mockOnRandomFile(testRoot)
            testFile.bytes = Random.nextBytes(64)
            val database = testDatabase
            database.put(listOf(testKFR))
            LocalModel(testRoot, database).use { model ->
                model.open()
                val actualKFR = model.changes.take()
                assertTrue(actualKFR.isValidTo(testKFR))
            }
        }
    }


    @Test
    fun monitor_createFile() {
        TEST_DIR.useDir {
            val (_, dummyKFR) = MockKFR.mockOnRandomFile(testRoot)
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()
                assertTrue(dummyKFR.contentEquals(model.changes.take()))

                val (_, testKfr) = MockKFR.mockOnRandomFile(testRoot)
                val actualKfr = model.changes.take()
                assertTrue(testKfr.contentEquals(actualKfr))
            }
        }
    }

    @Test
    fun monitor_modifyFile() {
        TEST_DIR.useDir {
            val (testFile, testKfr) = MockKFR.mockOnRandomFile(testRoot)
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()
                assertTrue(testKfr.contentEquals(model.changes.take()))

                testFile.bytes = Random.nextBytes(64)
                val actualKfr = model.changes.take()
                assertFalse(actualKfr.contentEquals(testKfr))
                assertTrue(actualKfr.isValidTo(testKfr))
            }
        }
    }

    @Test
    fun monitor_deleteFile() {
        TEST_DIR.useDir {
            val (testFile, testKfr) = MockKFR.mockOnRandomFile(testRoot)
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()
                assertTrue(testKfr.contentEquals(model.changes.take()))

                testFile.deleteFile()
                val actualKfr = model.changes.take()
                assertFalse(actualKfr.contentEquals(testKfr))
                assertTrue(actualKfr.isValidTo(testKfr))
            }
        }
    }


    @Test
    fun putCreate_get() {
        TEST_DIR.useDir {
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()

                //PUT
                val (file, kfr) = MockKFR.mockOnRandomFile(TEST_DIR)
                val bytes = file.bytes
                model.put(kfr, file)

                //GET
                val path = kfr.path
                val getRecordKfr = model.getRecord(path)
                assertEquals(kfr, getRecordKfr)
                val getContentFile = MockKFR.mockFile(TEST_DIR)
                val getContentKfr = model.getContent(path, getContentFile)
                assertEquals(kfr, getContentKfr)
                assertArrayEquals(bytes, getContentFile.bytes)
            }
        }
    }

    @Test
    fun putDelete_get() {
        TEST_DIR.useDir {
            val (dummyFile, dummyKfr) = MockKFR.mockOnRandomFile(testRoot)
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()
                assertTrue(dummyKfr.contentEquals(model.changes.take()))

                //PUT
                val kfr = KFR(dummyKfr.path, utcEpochMillis, -1, 0)
                model.put(kfr, null)
                assertTrue(dummyFile.notExists())

                //GET
                val path = kfr.path
                val getRecordKfr = model.getRecord(path)
                assertEquals(kfr, getRecordKfr)
                val getContentFile = MockKFR.mockFile(TEST_DIR)
                val getContentKfr = model.getContent(path, getContentFile)
                assertEquals(kfr, getContentKfr)
                assertTrue(getContentFile.notExists())
            }
        }
    }


    @Test
    @Timeout(15, unit = SECONDS)
    fun monitor_modifyFile_many() {
        val fileCount = 1024
        val fileSize = 64 * 1024
        TEST_DIR.useDir {
            val mockFiles = List(fileCount) { MockKFR.mockOnRandomFile(testRoot) }
            val mockKfrs = mockFiles.map { (_, kfr) -> kfr }
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()
                repeat(fileCount) {
                    val tookKfr = model.changes.take()
                    val findKfr = mockKfrs.find { it.path == tookKfr.path }
                    assertNotNull(findKfr)
                }
                mockFiles.forEach { (file, _) -> file.bytes = Random.nextBytes(fileSize) }
                repeat(fileCount) {
                    val actualKfr = model.changes.take()
                    val mockKfr = mockKfrs.find { it.path == actualKfr.path }
                    assertNotNull(mockKfr)
                    assertTrue(actualKfr.isValidTo(mockKfr))
                }
            }
        }
    }

    @Test
    @Timeout(15, unit = SECONDS)
    fun monitor_modifyFile_large() {
        val fileCount = 4
        val fileSize = 64 * 1024 * 1024
        TEST_DIR.useDir {
            val mockFiles = List(fileCount) { MockKFR.mockOnRandomFile(testRoot) }
            val mockKfrs = mockFiles.map { (_, kfr) -> kfr }
            LocalModel(testRoot, testDatabase).use { model ->
                model.open()
                repeat(fileCount) {
                    val tookKfr = model.changes.take()
                    val findKfr = mockKfrs.find { it.path == tookKfr.path }
                    assertNotNull(findKfr)
                }
                mockFiles.forEach { (file, _) -> file.bytes = Random.nextBytes(fileSize) }
                repeat(fileCount) {
                    val actualKfr = model.changes.take()
                    val mockKfr = mockKfrs.find { it.path == actualKfr.path }
                    assertNotNull(mockKfr)
                    assertTrue(actualKfr.isValidTo(mockKfr))
                }
            }
        }
    }

}