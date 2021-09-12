package top.anagke.kwormhole.model.local

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import top.anagke.kio.file.createDir
import top.anagke.kio.file.createFile
import top.anagke.kio.file.sameTo
import top.anagke.kwormhole.test.useDir
import java.io.File
import java.util.concurrent.TimeUnit

internal class FileAltMonitorTest {

    companion object {
        private val TEST_DIR = File("test")
    }


    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun test_createFile_depth1to16() {
        for (i in 1..16) {
            test_createFile_depthN(i)
        }
    }

    private fun test_createFile_depthN(maxDepth: Int) = TEST_DIR.useDir {
        FileAltMonitor(TEST_DIR).use { monitor ->
            var prev = TEST_DIR
            repeat(maxDepth) { depth ->
                val testFile = prev.resolve("test_file_depth$depth").also { it.createFile() }
                prev = prev.resolve("test_dir_depth$depth").also { it.createDir() }
                val actualFiles = monitor.take()
                Assertions.assertEquals(1, actualFiles.size)
                val actualFile = actualFiles.first()
                Assertions.assertTrue(testFile.sameTo(actualFile))
            }
        }
    }


    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun test_createManyFiles_depth1to16() {
        for (i in 1..16) {
            test_createManyFiles_depthN(i, 100)
        }
    }

    private fun test_createManyFiles_depthN(maxDepth: Int, maxFile: Int) = TEST_DIR.useDir {
        FileAltMonitor(TEST_DIR).use { monitor ->
            var prev = TEST_DIR
            repeat(maxDepth) { depth ->
                val testFiles = List(maxFile) { file ->
                    prev.resolve("tf_$depth-$file").also { it.createFile() }
                }.sorted()
                prev = prev.resolve("td_$depth").also { it.createDir() }

                val actualFiles = monitor.takeAtLeast(maxFile).sorted()
                Assertions.assertEquals(maxFile, actualFiles.size)
                repeat(maxFile) { i ->
                    Assertions.assertTrue(testFiles[i].sameTo(actualFiles[i]))
                }
            }
        }
    }


    private fun FileAltMonitor.takeAtLeast(atLeast: Int): MutableList<File> {
        val take = mutableListOf<File>()
        while (take.size < atLeast) {
            take += this.take()
        }
        return take
    }

}