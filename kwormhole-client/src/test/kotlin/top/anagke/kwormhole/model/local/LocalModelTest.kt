package top.anagke.kwormhole.model.local

import okio.ByteString.Companion.toByteString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import top.anagke.kio.bytes
import top.anagke.kio.createDir
import top.anagke.kio.deleteFile
import top.anagke.kio.notExists
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.asBytes
import top.anagke.kwormhole.notExists
import top.anagke.kwormhole.ofNotExists
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

    private val testDatabase: KfrDatabase
        get() {
            val databaseFile = TEST_DIR.resolve("test.db")
            databaseFile.deleteFile()
            databaseFile.deleteOnExit()
            return KfrDatabase(databaseFile)
        }


    @Test
    fun init_fromDisk() {
        TEST_DIR.useDir {
            val (_, expectedKfr) = MockKfr.mockOnRandomFile(testRoot)
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
                model.open()
                val actualKfr = model.changes.take()
                assertTrue(expectedKfr.equalsContent(actualKfr))
            }
        }
    }

    @Test
    fun init_fromDatabase() {
        TEST_DIR.useDir {
            val (_, expectedKfr) = MockKfr.mockOnRandomFile(testRoot)
            val database = testDatabase
            database.put(listOf(expectedKfr))
            LocalModel(KfrService(testRoot, database)).use { model ->
                model.open()
                val actualKfr = pollNonnull { model.getRecord(expectedKfr.path) }
                assertEquals(expectedKfr, actualKfr)
            }
        }
    }

    @Test
    fun init_fromDatabaseButDiskChanged() {
        TEST_DIR.useDir {
            val (testFile, testKfr) = MockKfr.mockOnRandomFile(testRoot)
            testFile.bytes = Random.nextBytes(64)
            val database = testDatabase
            database.put(listOf(testKfr))
            LocalModel(KfrService(testRoot, database)).use { model ->
                model.open()
                val actualKfr = model.changes.take()
                assertTrue(actualKfr.canReplace(testKfr))
            }
        }
    }


    @Test
    fun monitor_createFile() {
        TEST_DIR.useDir {
            val (_, dummyKfr) = MockKfr.mockOnRandomFile(testRoot)
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
                model.open()
                assertTrue(dummyKfr.equalsContent(model.changes.take()))

                val (_, testKfr) = MockKfr.mockOnRandomFile(testRoot)
                val actualKfr = model.changes.take()
                assertTrue(testKfr.equalsContent(actualKfr))
            }
        }
    }

    @Test
    fun monitor_modifyFile() {
        TEST_DIR.useDir {
            val (testFile, testKfr) = MockKfr.mockOnRandomFile(testRoot)
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
                model.open()
                assertTrue(testKfr.equalsContent(model.changes.take()))

                testFile.bytes = Random.nextBytes(64)
                val actualKfr = model.changes.take()
                assertFalse(actualKfr.equalsContent(testKfr))
                assertTrue(actualKfr.canReplace(testKfr))
            }
        }
    }

    @Test
    fun monitor_deleteFile() {
        TEST_DIR.useDir {
            val (testFile, testKfr) = MockKfr.mockOnRandomFile(testRoot)
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
                model.open()
                assertTrue(testKfr.equalsContent(model.changes.take()))

                testFile.deleteFile()
                val actualKfr = model.changes.take()
                assertFalse(actualKfr.equalsContent(testKfr))
                assertTrue(actualKfr.canReplace(testKfr))
            }
        }
    }


    @Test
    fun putCreate_get() {
        TEST_DIR.useDir {
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
                model.open()

                //PUT
                val (file, kfr) = MockKfr.mockOnRandomFile(TEST_DIR)
                val bytes = file.bytes
                model.put(FatKfr(kfr, file))

                //GET
                val path = kfr.path
                val getRecordKfr = model.getRecord(path)
                assertEquals(kfr, getRecordKfr)
                val kfrContent = model.getContent(path)
                assertEquals(kfr, kfrContent?.kfr)
                assertEquals(bytes.toByteString(), kfrContent?.asBytes())
            }
        }
    }

    @Test
    fun putDelete_get() {
        TEST_DIR.useDir {
            val (dummyFile, dummyKfr) = MockKfr.mockOnRandomFile(testRoot)
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
                model.open()
                assertTrue(dummyKfr.equalsContent(model.changes.take()))

                //PUT
                val kfr = Kfr(dummyKfr.path, utcEpochMillis, -1, 0)
                model.put(ofNotExists(kfr))
                assertTrue(dummyFile.notExists())

                //GET
                val path = kfr.path
                val getRecordKfr = model.getRecord(path)
                assertEquals(kfr, getRecordKfr)
                val kfrContent = model.getContent(path)
                assertNotNull(kfrContent); kfrContent!!
                assertEquals(kfr, kfrContent.kfr)
                assertTrue(kfrContent.notExists())
            }
        }
    }


    @Test
    @Timeout(15, unit = SECONDS)
    fun monitor_modifyFile_many() {
        val fileCount = 1024
        val fileSize = 64 * 1024
        TEST_DIR.useDir {
            val mockFiles = List(fileCount) { MockKfr.mockOnRandomFile(testRoot) }
            val mockKfrs = mockFiles.map { (_, kfr) -> kfr }
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
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
                    assertTrue(actualKfr.canReplace(mockKfr))
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
            val mockFiles = List(fileCount) { MockKfr.mockOnRandomFile(testRoot) }
            val mockKfrs = mockFiles.map { (_, kfr) -> kfr }
            LocalModel(KfrService(testRoot, testDatabase)).use { model ->
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
                    assertTrue(actualKfr.canReplace(mockKfr))
                }
            }
        }
    }

}