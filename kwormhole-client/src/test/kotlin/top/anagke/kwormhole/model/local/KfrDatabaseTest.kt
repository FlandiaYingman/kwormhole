package top.anagke.kwormhole.model.local

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import top.anagke.kio.createDir
import top.anagke.kio.deleteDir
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.test.TEST_DIR

internal class KfrDatabaseTest {

    @BeforeEach
    fun setUp() {
        TEST_DIR.createDir()
    }

    @AfterEach
    fun tearDown() {
        TEST_DIR.deleteDir()
    }

    private val databaseFile = TEST_DIR.resolve("database-test.db")

    @Test
    fun put100AtOnce_thenTestAll() {
        KfrDatabase(databaseFile).use { database ->
            val expectedKfrs = List(100) { MockKfr.mockRecord() }
            database.put(expectedKfrs)
            val actualKfrs = database.all()
            assertEquals(expectedKfrs.toSet(), actualKfrs.toSet())
        }
    }

    @Test
    fun put100AtOnce_thenTestGet() {
        KfrDatabase(databaseFile).use { database ->
            val expectedKfrs = MutableList(100) { MockKfr.mockRecord() }
            database.put(expectedKfrs)
            expectedKfrs.forEach { expectedKfr ->
                assertEquals(expectedKfr, database.get(expectedKfr.path))
            }
        }
    }

    @Test
    fun put100Sequentially_thenTestAll() {
        KfrDatabase(databaseFile).use { database ->
            val expectedKfrs = List(100) { MockKfr.mockRecord() }
            expectedKfrs.forEach { database.put(listOf(it)) }
            val actualKfrs = database.all()
            assertEquals(expectedKfrs.toSet(), actualKfrs.toSet())
        }
    }

}