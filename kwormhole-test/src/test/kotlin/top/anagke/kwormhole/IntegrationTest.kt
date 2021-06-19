package top.anagke.kwormhole

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.boot.SpringApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import top.anagke.kio.file.deleteDir
import top.anagke.kio.file.deleteFile
import top.anagke.kwormhole.util.Hasher
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

class IntegrationTest {

    private var server: ConfigurableApplicationContext? = null
    private var client1: KWormholeClient? = null
    private var client2: KWormholeClient? = null

    private val serverDatabase = File("./database-test.mv.db")

    private val client1Root = File("./client_1")
    private val client1Database = File("./client_1.db")
    private val client2Root = File("./client_2")
    private val client2Database = File("./client_2.db")

    @BeforeEach
    fun setUp() {
        serverDatabase.deleteFile()
        client1Root.deleteDir()
        client2Root.deleteDir()
        client1Database.deleteFile()
        client2Database.deleteFile()

        client1Root.mkdirs()
        client2Root.mkdirs()

        server = launchServer()
        client1 = launchClient(client1Root, client1Database)
        client2 = launchClient(client2Root, client2Database)
    }

    @AfterEach
    fun tearDown() {
        SpringApplication.exit(server)

        client1?.close()
        client2?.close()

        serverDatabase.deleteFile()
        client1Root.deleteDir()
        client2Root.deleteDir()
        client1Database.deleteFile()
        client2Database.deleteFile()
    }

    @Test
    @Timeout(30, unit = SECONDS)
    fun test_create_basic() {
        test_create(64, 4 * 1024)
    }

    @Test
    @Timeout(300, unit = SECONDS)
    fun test_create_many() {
        test_create(256, 4 * 1024)
    }

    @Test
    @Timeout(300, unit = SECONDS)
    fun test_create_large() {
        test_create(15, 256 * 1024 * 1024)
    }


    private fun test_create(fileNum: Int, fileSize: Int) {
        val mockFiles = List(fileNum) { mockFile(client1Root, fileSize) }
        val srcFiles = mockFiles
            .map { it.canonicalFile }
            .associateBy { it.name }

        while (client2Root.listFiles()!!.size != fileNum) {
            Thread.sleep(1000)
        }

        val dstFiles = client2Root.walk()
            .map { it.canonicalFile }
            .filter { it != client2Root.canonicalFile }
            .associateBy { it.name }

        srcFiles.keys.forEach {
            assertTrue(it in srcFiles)
            assertTrue(it in dstFiles)
            val srcFile = srcFiles[it]!!
            val dstFile = dstFiles[it]!!
            assertEquals(srcFile.name, dstFile.name)
            assertEquals(srcFile.length(), dstFile.length())
            assertEquals(Hasher.hash(srcFile), Hasher.hash(dstFile))
        }
    }

}

private fun launchServer(): ConfigurableApplicationContext {
    return runApplication<KwormholeApp>()
}

private fun launchClient(root: File, database: File): KWormholeClient {
    return KWormholeClient.open(root.absolutePath, database.absolutePath, "localhost", 8080)
}

