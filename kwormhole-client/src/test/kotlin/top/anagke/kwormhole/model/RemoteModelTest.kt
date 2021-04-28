package top.anagke.kwormhole.model

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.send
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.test.TEST_SERVER_PORT
import top.anagke.kwormhole.test.nextFileRC
import top.anagke.kwormhole.test.nextFileRecord
import top.anagke.kwormhole.test.testServer
import top.anagke.kwormhole.test.toJson
import top.anagke.kwormhole.test.tryPoll
import top.anagke.kwormhole.test.use
import java.net.InetSocketAddress
import kotlin.random.Random

internal class RemoteModelTest {

    private fun simpleServer(
        initRecords: List<FileRecord> = emptyList(),
        monitorRecords: List<FileRecord> = emptyList(),
    ) = testServer {
        install(WebSockets)
        routing {
            route("/") {
                handle {
                    call.respondText(toJson(initRecords), ContentType.Application.Json)
                }
            }
            route("{...}", HttpMethod.Post) {
                handle {
                    call.respond(HttpStatusCode.OK)
                }
            }
            webSocket("/ws/change") {
                monitorRecords.forEach {
                    send(toJson(it))
                }
            }
        }
    }


    @Test
    fun testInitChanges() {
        val testRecord = Random.nextFileRecord()
        simpleServer(initRecords = listOf(testRecord)).use {
            RemoteModel(InetSocketAddress("localhost", TEST_SERVER_PORT)).use { model ->
                val expectedPath = testRecord.path
                Assertions.assertEquals(expectedPath, model.changes.tryPoll())
                Assertions.assertNotNull(model.records[expectedPath])
                Assertions.assertNotNull(model.contents[expectedPath])
            }
        }
    }

    @Test
    fun testMonitorChanges() {
        val dummyRecord = Random.nextFileRecord()
        val testRecord = Random.nextFileRecord()
        simpleServer(initRecords = listOf(dummyRecord), monitorRecords = listOf(testRecord)).use {
            RemoteModel(InetSocketAddress("localhost", TEST_SERVER_PORT)).use { model ->
                val expectedDummyPath = dummyRecord.path
                Assertions.assertEquals(expectedDummyPath, model.changes.tryPoll())
                Assertions.assertNotNull(model.records[expectedDummyPath])
                Assertions.assertNotNull(model.contents[expectedDummyPath])

                val expectedTestPath = testRecord.path
                Assertions.assertEquals(expectedTestPath, model.changes.tryPoll())
                Assertions.assertNotNull(model.records[expectedTestPath])
                Assertions.assertNotNull(model.contents[expectedTestPath])
            }
        }
    }

    @Test
    fun testPut() {
        val (testRecord, testContent) = Random.nextFileRC()
        simpleServer().use {
            RemoteModel(InetSocketAddress("localhost", TEST_SERVER_PORT)).use { model ->
                model.put(testRecord, testContent)

                val expectedPath = testRecord.path
                Assertions.assertTrue(model.changes.isEmpty())

                val actualRecord = model.records[expectedPath]
                Assertions.assertNotNull(actualRecord)
                Assertions.assertEquals(testRecord, actualRecord)
                Assertions.assertEquals(1, model.records.size)

//                val actualContent = model.contents[expectedPath]
//                Assertions.assertNotNull(actualContent)
//                Assertions.assertArrayEquals(testContent.asBytes(), actualContent!!().asBytes())
//                Assertions.assertEquals(1, model.contents.size)
            }
        }
    }

}