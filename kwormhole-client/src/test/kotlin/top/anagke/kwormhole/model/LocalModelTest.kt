package top.anagke.kwormhole.model

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.asBytes
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.test.useDir
import top.anagke.kwormhole.toRealPath
import top.anagke.kwormhole.toRecordPath

internal class LocalModelTest {

    @Test
    fun testInitFilesystem() {
        TEST_DIR.useDir {
            val testFile = MockKfr.mockFile(TEST_DIR)
            LocalModel(TEST_DIR).use { model ->
                val expectedPath = toRecordPath(TEST_DIR, testFile)
                val actualPath = model.changes.take()
                assertThat(actualPath, equalTo(expectedPath))
            }
        }
    }

    @Test
    fun testInitDatabase() {
        TEST_DIR.useDir {
            val database = RecordDatabase(MockKfr.mockFile(TEST_DIR))
            val expectedRecord = MockKfr.mockRecord()
            database.put(listOf(expectedRecord))
            LocalModel(TEST_DIR, database).use { model ->
                val actualPath = model.changes.take()
                val actualRecord = model.records[actualPath]
                assertThat(actualRecord, notNullValue())
                assertThat(actualRecord, equalTo(expectedRecord))
            }
        }
    }

    @Test
    fun testMonitor() {
        TEST_DIR.useDir {
            LocalModel(TEST_DIR).use { model ->
                val testFile = MockKfr.mockFile(TEST_DIR)
                val expectedPath = toRecordPath(TEST_DIR, testFile)
                val actualPath = model.changes.take()
                assertThat(actualPath, equalTo(expectedPath))
            }
        }
    }

    @Test
    fun testPutFilesystem() {
        TEST_DIR.useDir {
            LocalModel(TEST_DIR).use { model ->
                val (expectedRecord, expectedContent) = MockKfr.mockBoth()
                val expectedFile = toRealPath(TEST_DIR, expectedRecord.path)
                model.put(expectedRecord, expectedContent)

                assertTrue(expectedFile.exists())
                assertArrayEquals(expectedContent.asBytes(), expectedFile.readBytes())
            }
        }
    }

    @Test
    fun testPutDatabase() {
        TEST_DIR.useDir {
            val database = RecordDatabase(MockKfr.mockFile(TEST_DIR))
            LocalModel(TEST_DIR, database).use { model ->
                val (expectedRecord, expectedContent) = MockKfr.mockBoth()
                model.put(expectedRecord, expectedContent)

                assertTrue(expectedRecord in database.all())
            }
        }
    }

}