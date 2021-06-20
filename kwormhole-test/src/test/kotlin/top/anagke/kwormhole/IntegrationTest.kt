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
import top.anagke.kio.KiB
import top.anagke.kio.MiB
import top.anagke.kio.file.promise
import top.anagke.kwormhole.util.Hasher
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

class IntegrationTest {

    private var server: ConfigurableApplicationContext? = null
    private var client1: KWormholeClient? = null
    private var client2: KWormholeClient? = null

    private val serverRoot = File("./server_root")
    private val serverDatabase = File("./database-test.mv.db")

    private val client1Root = File("./client_1")
    private val client1Database = File("./client_1.db")
    private val client2Root = File("./client_2")
    private val client2Database = File("./client_2.db")

    @BeforeEach
    fun setUp() {
        serverRoot.promise().directory().new()
        serverDatabase.promise().notExist()
        client1Root.promise().directory().new()
        client2Root.promise().directory().new()
        client1Database.promise().notExist()
        client2Database.promise().notExist()

        server = launchServer()
        client1 = launchClient(client1Root, client1Database)
        client2 = launchClient(client2Root, client2Database)
    }

    @AfterEach
    fun tearDown() {
        SpringApplication.exit(server)

        client1?.close()
        client2?.close()

        serverRoot.promise().notExist()
        serverDatabase.promise().notExist()
        client1Root.promise().notExist()
        client2Root.promise().notExist()
        client1Database.promise().notExist()
        client2Database.promise().notExist()
    }

    @Test
    @Timeout(30, unit = SECONDS)
    fun test_create_basic() {
        test_create(1, 4.KiB)
    }

    @Test
    fun test_create_large() {
        test_create(1, 64.MiB)
    }

    @Test
    @Timeout(300, unit = SECONDS)
    fun test_create_many() {
        test_create(256, 4.KiB)
    }

    @Test
    @Timeout(300, unit = SECONDS)
    fun test_create_many_large() {
        test_create(64, 64.MiB)
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

