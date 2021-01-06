package top.anagke.kwormhole.net

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.features.PartialContent
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.request.contentType
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.param
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.nextBytesList
import top.anagke.kwormhole.nextPath
import top.anagke.kwormhole.store.KwormFile
import top.anagke.kwormhole.store.KwormFile.Companion.utcTimeMillis
import top.anagke.kwormhole.util.MiB
import top.anagke.kwormhole.util.parseRange
import top.anagke.kwormhole.util.use
import java.io.File
import kotlin.random.Random

internal class KwormClientTest {

    companion object {
        private const val RANDOM_TIMES = 16
        private val TEST_FILE = File("TEST_FILE")
    }

    @Test
    fun listFile() {
        val kwormList = List(RANDOM_TIMES) {
            KwormFile(Random.nextPath(8), Random.nextLong(), Random.nextLong(0, utcTimeMillis))
        }
        val server = embeddedServer(CIO, port = 8080) {
            install(PartialContent)
            install(ContentNegotiation) {
                gson()
            }
            routing {
                route("/", HttpMethod.Get) {
                    param("method", "list") {
                        handle {
                            call.respond(kwormList)
                        }
                    }
                }
            }
        }

        server.start()
        val kwormClient = KwormClient(TEST_HOST, 8080)
        runBlocking {
            assertEquals(kwormList, kwormClient.listFiles(0, Int.MAX_VALUE))
        }
        server.stop(0, 0)
    }

    @Test
    fun uploadFile() {
        val contentList = List(RANDOM_TIMES) { Random.nextBytes(16.MiB) }
        val kwormList = List(RANDOM_TIMES) {
            KwormFile(Random.nextPath(8), Random.nextLong(), Random.nextLong(0, utcTimeMillis))
        }

        var count = 0
        var prevIsContent = false
        val server = embeddedServer(CIO, port = 8080) {
            install(PartialContent)
            install(ContentNegotiation) {
                gson()
            }
            routing {
                route("{...}") {
                    param("method", "content-ranged") {
                        handle {
                            val range = parseRange(call.request.headers["Content-Range"]!!)
                            val expected = contentList[count].sliceArray(range)
                            val actual = call.receive<ByteArray>()
                            assertArrayEquals(expected, actual)

                            prevIsContent = true
                            call.respond("OK")
                        }
                    }
                    param("method", "content") {
                        handle {
                            val expected = contentList[count]
                            val actual = call.receive<ByteArray>()
                            assertArrayEquals(expected, actual)

                            prevIsContent = true
                            call.respond("OK")
                        }
                    }
                    param("method", "metadata") {
                        handle {
                            val expectedJson = Gson().toJson(kwormList[count])
                            val actualJson = call.receiveText()
                            assertTrue(prevIsContent)
                            assertEquals(ContentType.Application.Json, call.request.contentType())
                            assertEquals(expectedJson, actualJson)

                            count++
                            prevIsContent = false
                            call.respond("OK")
                        }
                    }
                }
            }
        }

        server.start()
        val kwormClient = KwormClient(TEST_HOST, 8080, HttpClient {
            install(JsonFeature)
        })
        TEST_FILE.use {
            repeat(RANDOM_TIMES) { i ->
                runBlocking {
                    TEST_FILE.writeBytes(contentList[i])
                    kwormClient.uploadFile(kwormList[i], TEST_FILE)
                }
            }
        }
        server.stop(0, 0)
    }

    @Test
    fun downloadFile() {
        val contentList = Random.nextBytesList(RANDOM_TIMES, 16.MiB)
        val kwormList = List(RANDOM_TIMES) {
            KwormFile(Random.nextPath(8), Random.nextLong(), Random.nextLong(0, utcTimeMillis))
        }

        val server = embeddedServer(CIO, port = 8080) {
            install(PartialContent)
            install(ContentNegotiation) {
                gson()
            }
            routing {
                route("{...}") {
                    param("method", "content") {
                        handle {
                            call.respondBytes(
                                contentList[kwormList.indexOfFirst { it.path == call.request.path() }],
                                ContentType.Application.OctetStream
                            )
                        }
                    }
                    param("method", "metadata") {
                        handle {
                            call.respond(kwormList.find { it.path == call.request.path() }!!)
                        }
                    }
                }
            }
        }

        server.start()
        val kwormClient = KwormClient(TEST_HOST, 8080, HttpClient {
            install(JsonFeature)
        })
        repeat(RANDOM_TIMES) {
            TEST_FILE.use { _ ->
                runBlocking {
                    kwormClient.downloadFile(kwormList[it].path, TEST_FILE)
                    val expectedBytes = contentList[it]
                    val actualBytes = TEST_FILE.readBytes()
                    assertArrayEquals(expectedBytes, actualBytes)
                }
            }
        }
        server.stop(0, 0)
    }

}