package top.anagke.kwormhole.model.local

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import top.anagke.kio.file.bytes
import top.anagke.kio.file.createDir
import top.anagke.kio.file.deleteDir
import top.anagke.kio.file.deleteFile
import top.anagke.kio.file.notExists
import top.anagke.kwormhole.EmptyFatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.test.pollNonnull
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

@Timeout(5, unit = SECONDS)
internal class LocalModelTest {

    @BeforeEach
    fun setup() {
        TEST_DIR.createDir()
    }

    @AfterEach
    fun teardown() {
        _current?.close()
        TEST_DIR.deleteDir()
    }

    private var _current: LocalModel? = null

    private fun newLocalModel(): LocalModel {
        val mockRoot = TEST_DIR.resolve(MockKfr.mockString())
        val mockDatabase = KfrDatabase(TEST_DIR.resolve(MockKfr.mockString()))

        mockRoot.createDir()
        return LocalModel(KfrService(mockRoot, mockDatabase)).also { _current = it }
    }


    @Test
    fun init_fromDisk() {
        val model = newLocalModel()
        val (exFatKfr, _) = MockKfr.mockOnFile(model.kfrService.root)

        model.open()
        val actualKfr = model.changes.take()
        assertTrue(exFatKfr.kfr.equalsContent(actualKfr))
    }

    @Test
    fun init_fromDatabase() {
        val model = newLocalModel()
        val (exFatKfr, _) = MockKfr.mockOnFile(model.kfrService.root)
        model.kfrService.database.put(listOf(exFatKfr.kfr))

        model.open()
        val actualKfr = pollNonnull { model.head(exFatKfr.kfr.path) }
        assertEquals(exFatKfr.kfr, actualKfr)
    }

    @Test
    fun init_fromDatabaseButDiskChanged() {
        val model = newLocalModel()
        val (testFatKfr, testFile) = MockKfr.mockOnFile(model.kfrService.root)
        val testKfr = testFatKfr.kfr

        testFile.bytes = Random.nextBytes(64)
        val database = model.kfrService.database
        database.put(listOf(testKfr))
        model.open()
        val actualKfr = model.changes.take()
        assertTrue(actualKfr.canReplace(testKfr))
    }


    @Test
    fun monitor_create_delete() {
        val model = newLocalModel()
        model.open()

        val mockList = MutableList(16) { MockKfr.mockOnFile(model.kfrService.root) }
        val removingMockList = mockList.takeWhile { Random.nextBoolean() }
        removingMockList.forEach { pair -> mockList.remove(pair) }
        removingMockList.forEach { (_, file) -> file.deleteFile() }

        // Wait For...
        pollNonnull {
            mockList.forEach { (mockFat, _) ->
                model.get(mockFat.path).use { modelFat ->
                    if (modelFat == null) return@pollNonnull null
                    if (mockFat.body() != modelFat.body()) return@pollNonnull null
                }
            }
        }

        mockList.forEach { (mockFat, _) ->
            model.get(mockFat.path).use { modelFat ->
                assertNotNull(modelFat); modelFat!!
                assertEquals(mockFat.body(), modelFat.body())
            }
        }

    }


    @Test
    fun putCreate_get() {
        val model = newLocalModel()
        model.kfrService.database
        model.open()

        //PUT
        val (testFatKfr, _) = MockKfr.mockOnFile(TEST_DIR)
        val testKfr = testFatKfr.kfr
        model.put(testFatKfr)

        //GET
        val path = testFatKfr.kfr.path
        val getRecordKfr = model.head(path)
        assertEquals(testKfr, getRecordKfr)
        val kfrContent = model.get(path)
        assertEquals(testKfr, kfrContent?.kfr)
        assertEquals(testFatKfr.body(), kfrContent?.body())

    }

    @Test
    fun putDelete_get() {
        val model = newLocalModel()
        val (dummyFatKfr, dummyFile) = MockKfr.mockOnFile(model.kfrService.root)
        val dummyKfr = dummyFatKfr.kfr
        model.kfrService.database
        model.open()
        assertTrue(dummyKfr.equalsContent(model.changes.take()))

        //PUT
        val kfr = Kfr(dummyKfr.path, utcEpochMillis, -1, 0)
        model.put(EmptyFatKfr(kfr))
        assertTrue(dummyFile.notExists())

        //GET
        val path = kfr.path
        val getRecordKfr = model.head(path)
        assertEquals(kfr, getRecordKfr)
        val kfrContent = model.get(path)
        assertNotNull(kfrContent); kfrContent!!
        assertEquals(kfr, kfrContent.kfr)
        assertTrue(kfrContent.notExists())
    }

}