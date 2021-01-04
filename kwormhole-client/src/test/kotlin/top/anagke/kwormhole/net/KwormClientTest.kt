package top.anagke.kwormhole.net

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.features.ContentNegotiation
import io.ktor.features.PartialContent
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
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
import top.anagke.kwormhole.store.FileMetadata
import top.anagke.kwormhole.store.KwormFile
import top.anagke.kwormhole.store.KwormFile.Companion.utcTimeMillis
import top.anagke.kwormhole.util.B
import top.anagke.kwormhole.util.MiB
import top.anagke.kwormhole.util.hash
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
            KwormFile(
                Random.nextPath(8),
                FileMetadata(Random.nextLong(), Random.nextLong(0, utcTimeMillis))
            )
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
            assertEquals(kwormList, kwormClient.listFiles())
        }

        server.stop(0, 0)
    }

    @Test
    fun uploadFile() {
        val contentList = listOf(
            List(RANDOM_TIMES) { Random.nextBytes(16.B) },
            List(RANDOM_TIMES) { Random.nextBytes(16.MiB) }
        ).flatten()
        val metadataList = List(RANDOM_TIMES * 2) {
            FileMetadata(contentList[it].hash(), Random.nextLong())
        }

        var count = 0
        var prevIsContent = false
        val handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData = { request ->
            println(request)
            when {
                request.url.parameters["method"] == "content-ranged" -> {
                    prevIsContent = true
                    val range = parseRange(request.headers["Content-Range"]!!)
                    val body = request.body as OutgoingContent.ByteArrayContent
                    val expected = contentList[count].sliceArray(range)
                    val actual = body.bytes()
                    assertArrayEquals(expected, actual)
                }
                request.url.parameters["method"] == "content" -> {
                    prevIsContent = true
                    val expected = contentList[count]
                    val actual = (request.body as OutgoingContent.ByteArrayContent).bytes()
                    assertArrayEquals(expected, actual)
                }
                request.url.parameters["method"] == "metadata" -> {
                    assertTrue(prevIsContent)
                    prevIsContent = false
                    assertEquals(ContentType.Application.Json, request.body.contentType)
                    val expectedJson = Gson().toJson(metadataList[count])
                    val actualJson = (request.body as TextContent).text
                    assertEquals(expectedJson, actualJson)
                    count++
                }
                else -> fail("$request is not content nor metadata")
            }
            respond("OK")
        }

        val kwormClient = KwormClient(TEST_HOST, 8080, HttpClient(MockEngine) {
            install(JsonFeature)
            engine {
                addHandler(handler)
            }
        })

        val file = File("test_file")
        file.deleteOnExit()
        repeat(RANDOM_TIMES * 2) { i ->
            file.writeBytes(contentList[i])
            runBlocking {
                kwormClient.uploadFile("test_file", file, metadataList[i])
            }
        }
    }

    @Test
    fun downloadFile() {
        val contentList = Random.nextBytesList(RANDOM_TIMES, 16.MiB)
        val metadataList = List(RANDOM_TIMES) {
            val hash = contentList[it].hash()
            val updateTime = Random.nextLong(0, utcTimeMillis)
            FileMetadata(hash, updateTime)
        }

        var counter = 0
        val server = embeddedServer(CIO, port = 8080) {
            install(PartialContent)
            install(ContentNegotiation) {
                gson()
            }
            routing {
                route("/TEST_FILE", HttpMethod.Get) {
                    param("method", "content") {
                        handle {
                            call.respondBytes(contentList[counter], ContentType.Application.OctetStream)
                        }
                    }
                    param("method", "metadata") {
                        handle {
                            call.respond(metadataList[counter++])
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
                    kwormClient.downloadFile("TEST_FILE", TEST_FILE)
                    val expectedBytes = contentList[it]
                    val actualBytes = TEST_FILE.readBytes()
                    assertArrayEquals(expectedBytes, actualBytes)
                }
            }
        }
        server.stop(0, 0)
    }

}