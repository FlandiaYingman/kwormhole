package top.anagke.kwormhole.testing

import org.springframework.boot.SpringApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import top.anagke.kio.file.promise
import top.anagke.kwormhole.KWormholeClient
import top.anagke.kwormhole.KwormholeApp
import java.io.File

object IntegrationTesting {

    val serverRoot = File("./server_root")
    val serverDatabase = File("./database-test.mv.db")

    val client1Root = File("./client_1")
    val client1Database = File("./client_1.db")
    val client2Root = File("./client_2")
    val client2Database = File("./client_2.db")


    fun launchServer(): ConfigurableApplicationContext {
        return runApplication<KwormholeApp>()
    }

    fun launchClient1(): KWormholeClient {
        return KWormholeClient.open(client1Root.toPath(), client1Database.toPath(), "localhost", 8080)
    }

    fun launchClient2(): KWormholeClient {
        return KWormholeClient.open(client2Root.toPath(), client2Database.toPath(), "localhost", 8080)
    }


    private var server: ConfigurableApplicationContext? = null
    private var client1: KWormholeClient? = null
    private var client2: KWormholeClient? = null


    fun setup() {
        serverRoot.promise().directory().new()
        serverDatabase.promise().notExist()
        client1Root.promise().directory().new()
        client2Root.promise().directory().new()
        client1Database.promise().notExist()
        client2Database.promise().notExist()

        server = launchServer()
        client1 = launchClient1()
        client2 = launchClient2()
    }

    fun teardown() {
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

}