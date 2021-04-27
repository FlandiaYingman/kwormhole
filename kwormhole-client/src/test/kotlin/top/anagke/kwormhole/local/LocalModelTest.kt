package top.anagke.kwormhole.local

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.test.TEST_ENTRY_COUNT
import top.anagke.kwormhole.test.TEST_FILE_NAME_LENGTH
import top.anagke.kwormhole.test.nextHexString
import top.anagke.kwormhole.test.useDir
import java.io.File
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.random.Random

internal class LocalModelTest {

    @Test
    fun testInitUpdate() {
        val testFiles = List(TEST_ENTRY_COUNT) { TEST_DIR.resolve(Random.nextHexString(TEST_FILE_NAME_LENGTH)) }
        TEST_DIR.useDir {
            testFiles.forEach(File::createNewFile)
            val testPaths = testFiles.map { FileRecord.byFile(TEST_DIR, it).path }.toMutableList()
            LocalModel.newLocalModel(TEST_DIR).use { model ->
                while (testPaths.isNotEmpty()) {
                    val record = model.changes.poll(1000, MILLISECONDS)?.path ?: fail()
                    Assertions.assertTrue { testPaths.contains(record) }
                    testPaths.remove(record)
                }
            }
        }
    }

    @Test
    fun testMonitorUpdate() {
        val testFiles = List(TEST_ENTRY_COUNT) { TEST_DIR.resolve(Random.nextHexString(TEST_FILE_NAME_LENGTH)) }
        TEST_DIR.useDir {
            val testPaths = testFiles.map { FileRecord.byFile(TEST_DIR, it).path }.toMutableList()
            LocalModel.newLocalModel(TEST_DIR).use { model ->
                testFiles.forEach(File::createNewFile)
                while (testPaths.isNotEmpty()) {
                    val record = model.changes.poll(1000, MILLISECONDS)?.path ?: fail()
                    Assertions.assertTrue { testPaths.contains(record) }
                    testPaths.remove(record)
                }
            }
        }
    }

}