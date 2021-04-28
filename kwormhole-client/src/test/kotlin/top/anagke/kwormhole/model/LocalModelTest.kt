package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.asBytes
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.test.TEST_FILE_NAME_LENGTH
import top.anagke.kwormhole.test.nextFileRC
import top.anagke.kwormhole.test.nextHexString
import top.anagke.kwormhole.test.tryPoll
import top.anagke.kwormhole.test.useDir
import top.anagke.kwormhole.toRecordPath
import kotlin.random.Random

internal class LocalModelTest {

    @Test
    fun testInitChanges() = TEST_DIR.useDir {
        val testFile = TEST_DIR.resolve(Random.nextHexString(TEST_FILE_NAME_LENGTH))

        testFile.createNewFile()
        LocalModel(TEST_DIR).use { model ->
            val expectedPath = toRecordPath(TEST_DIR, testFile)
            Assertions.assertEquals(expectedPath, model.changes.tryPoll())
            Assertions.assertNotNull(model.records[expectedPath])
            Assertions.assertNotNull(model.contents[expectedPath])
        }
    }

    @Test
    fun testMonitorChanges() = TEST_DIR.useDir {
        val dummyFile = TEST_DIR.resolve(Random.nextHexString(TEST_FILE_NAME_LENGTH))
        val testFile = TEST_DIR.resolve(Random.nextHexString(TEST_FILE_NAME_LENGTH))

        dummyFile.createNewFile()
        LocalModel(TEST_DIR).use { model ->
            val expectedDummyPath = toRecordPath(TEST_DIR, dummyFile)
            Assertions.assertEquals(expectedDummyPath, model.changes.tryPoll())
            Assertions.assertNotNull(model.records[expectedDummyPath])
            Assertions.assertNotNull(model.contents[expectedDummyPath])

            testFile.createNewFile()
            val expectedTestPath = toRecordPath(TEST_DIR, testFile)
            Assertions.assertEquals(expectedTestPath, model.changes.tryPoll())
            Assertions.assertNotNull(model.records[expectedTestPath])
            Assertions.assertNotNull(model.contents[expectedTestPath])
        }
    }

    @Test
    fun testPut() = TEST_DIR.useDir {
        val (testRecord, testContent) = Random.nextFileRC()

        LocalModel(TEST_DIR).use { model ->
            model.put(testRecord, testContent)

            val expectedPath = testRecord.path
            Assertions.assertTrue(model.changes.isEmpty())

            val actualRecord = model.records[expectedPath]
            Assertions.assertNotNull(actualRecord)
            Assertions.assertEquals(testRecord, actualRecord)
            Assertions.assertEquals(1, model.records.size)

            val actualContent = model.contents[expectedPath]
            Assertions.assertNotNull(actualContent)
            Assertions.assertArrayEquals(testContent.asBytes(), actualContent!!().asBytes())
            Assertions.assertEquals(1, model.contents.size)
        }
    }

}