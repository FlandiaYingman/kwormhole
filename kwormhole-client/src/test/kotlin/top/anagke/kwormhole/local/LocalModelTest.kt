package top.anagke.kwormhole.local

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.TEST_DIR
import top.anagke.kwormhole.useDir
import java.io.File
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class LocalModelTest {

    @Test
    fun testInitUpdate() {
        val testFiles = listOf(
            TEST_DIR.resolve("foo.file.init"),
            TEST_DIR.resolve("bar.file.init"),
            TEST_DIR.resolve("foo_bar.file.init"),
        )
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
        val testFiles = listOf(
            TEST_DIR.resolve("foo.file.update"),
            TEST_DIR.resolve("bar.file.update"),
            TEST_DIR.resolve("foo_bar.file.update"),
        )
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