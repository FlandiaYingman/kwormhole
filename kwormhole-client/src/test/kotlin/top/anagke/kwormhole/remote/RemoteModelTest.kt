package top.anagke.kwormhole.remote

import com.google.gson.Gson
import io.ktor.application.install
import io.ktor.http.cio.websocket.send
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.TEST_UNIT_NUM
import top.anagke.kwormhole.randomFileRecord
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

internal class RemoteModelTest {

    @Test
    fun testRetrieveAll() {
        val testRecords = List(TEST_UNIT_NUM) { randomFileRecord() }
        val server = embeddedServer(CIO, port = 8080) {
            install(WebSockets)
            routing {
                webSocket("/ws/all") {
                    send(Gson().toJson(testRecords))
                }
                webSocket("/ws/change") {
                    delay(3000) //Do nothing
                }
            }
        }

        try {
            val expectedRecords = testRecords.toMutableList()
            server.start()

            RemoteModel.newRemoteModel(InetSocketAddress("127.0.0.1", 8080)).use { remoteModel ->
                while (expectedRecords.isNotEmpty()) {
                    val record = remoteModel.changes.poll(1000, TimeUnit.MILLISECONDS)
                    Assertions.assertNotNull(record); record!!
                    Assertions.assertTrue(expectedRecords.contains(record))
                    expectedRecords.remove(record)
                }
            }
        } finally {
            server.stop(0, 0)
        }
    }

    @Test
    fun testRetrieveChange() {
        val testRecords = List(TEST_UNIT_NUM) { randomFileRecord() }
        val server = embeddedServer(CIO, port = 8080) {
            install(WebSockets)
            routing {
                webSocket("/ws/all") {
                    send(Gson().toJson(emptyList<FileRecord>()))
                }
                webSocket("/ws/change") {
                    testRecords.forEach {
                        send(Gson().toJson(it))
                    }
                    delay(10000)
                }
            }
        }

        try {
            val expectedRecords = testRecords.toMutableList()
            server.start()

            RemoteModel.newRemoteModel(InetSocketAddress("127.0.0.1", 8080)).use { remoteModel ->
                while (expectedRecords.isNotEmpty()) {
                    val record = remoteModel.changes.poll(1000, TimeUnit.MILLISECONDS)
                    Assertions.assertNotNull(record); record!!
                    Assertions.assertTrue(expectedRecords.contains(record))
                    expectedRecords.remove(record)
                }
            }
        } finally {
            server.stop(0, 0)
        }
    }

}