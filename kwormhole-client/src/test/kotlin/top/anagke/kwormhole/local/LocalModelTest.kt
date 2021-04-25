package top.anagke.kwormhole.local

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.TEST_DIR
import top.anagke.kwormhole.TEST_UNIT_NUM
import top.anagke.kwormhole.nextHexString
import top.anagke.kwormhole.useDir
import java.io.File
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.random.Random

internal class LocalModelTest {

    @Test
    fun testInitUpdate() {
        val testFiles = List(TEST_UNIT_NUM) { TEST_DIR.resolve(Random.nextHexString(4).let { "$it.test" }) }
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
        val testFiles = List(TEST_UNIT_NUM) { TEST_DIR.resolve(Random.nextHexString(4).let { "$it.test" }) }
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