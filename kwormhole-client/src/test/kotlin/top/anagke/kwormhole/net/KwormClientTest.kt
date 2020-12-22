package top.anagke.kwormhole.net

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.store.FileMetadata
import top.anagke.kwormhole.util.B
import top.anagke.kwormhole.util.MiB
import top.anagke.kwormhole.util.hash
import top.anagke.kwormhole.util.parseRange
import java.io.File
import kotlin.random.Random

internal class KwormClientTest {

    companion object {
        private const val RANDOM_TIMES = 16
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

        val kwormClient = KwormClient(TEST_HOST, HttpClient(MockEngine) {
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

}