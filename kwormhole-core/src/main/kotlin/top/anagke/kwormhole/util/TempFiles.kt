package top.anagke.kwormhole.util

import top.anagke.kio.file.createDir
import top.anagke.kio.file.deleteDir
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.concurrent.thread
import kotlin.random.Random

object TempFiles {

    val DEFAULT_TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "top.anagke.TempFiles")

    private val TEMP_DIRS: MutableList<Path> = mutableListOf()

    private val TEMP_FILES: MutableList<Path> = mutableListOf()

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            TEMP_DIRS.forEach {
                it.toFile().deleteDir()
            }
        })
        register(DEFAULT_TEMP_DIR)
    }


    @Synchronized
    fun register(tempDir: Path) {
        tempDir.toFile().deleteDir()
        tempDir.toFile().createDir()
        TEMP_DIRS.add(tempDir.toAbsolutePath())
    }

    @Synchronized
    fun register(tempDir: File) {
        register(tempDir.toPath())
    }

    @Synchronized
    fun Path.isRegistered(): Boolean {
        return this.toAbsolutePath() in TEMP_DIRS
    }

    @Synchronized
    fun Path.isAllocated(): Boolean {
        return this.toAbsolutePath() in TEMP_FILES
    }

    @Synchronized
    fun allocTempFile(tempDir: Path = DEFAULT_TEMP_DIR): Path {
        check(tempDir.isRegistered())
        val tempFile = tempDir.resolve(Random.nextHexString(32)).toAbsolutePath()
        TEMP_FILES.add(tempFile)
        return tempFile
    }

    @Synchronized
    fun allocTempFile(tempDir: File): File {
        return allocTempFile(tempDir.toPath()).toFile()
    }

    @Synchronized
    fun freeTempFile(tempFile: Path) {
        if (tempFile.isAllocated()) {
            Files.deleteIfExists(tempFile)
        }
    }

    @Synchronized
    fun freeTempFile(tempFile: File) {
        freeTempFile(tempFile.toPath())
    }


    fun useTempFile(tempDir: Path, block: (Path) -> Unit) {
        val tempFile = allocTempFile(tempDir)
        try {
            block(tempFile)
        } finally {
            freeTempFile(tempFile)
        }
    }


    private fun Random.nextHexString(length: Int): String {
        return this.nextBytes(length * 16).let {
            BigInteger(1, it).toString(16).takeLast(length)
        }
    }

}