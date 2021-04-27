package top.anagke.kwormhole.remote

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.content.PartData
import io.ktor.http.content.PartData.FormItem
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.param
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.Json
import top.anagke.kwormhole.MemoryFileContent
import top.anagke.kwormhole.TEST_DATA_LENGTH
import top.anagke.kwormhole.TEST_SERVER_PORT
import top.anagke.kwormhole.TEST_UNIT_TIMES
import top.anagke.kwormhole.asBytes
import top.anagke.kwormhole.nextFilePath
import top.anagke.kwormhole.nextFileRC
import top.anagke.kwormhole.randomFileRecord
import top.anagke.kwormhole.testServer
import top.anagke.kwormhole.use
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

internal class KwormClientTest {

    @Test
    fun listFiles() {
        val expectedRecords = List(TEST_UNIT_TIMES) { randomFileRecord() }
        testServer {
            routing {
                route("/") {
                    handle {
                        call.respondText(Json.toJson(expectedRecords), ContentType.Application.Json)
                    }
                }
            }
        }.use {
            val client = KwormClient("localhost", TEST_SERVER_PORT)
            val actualRecords = runBlocking { client.listFiles() }
            Assertions.assertEquals(expectedRecords, actualRecords)
        }
    }

    @Test
    fun downloadRecord() {
        val testRecord = randomFileRecord()
        val testPath = testRecord.path
        testServer {
            routing {
                route(testPath) {
                    param("type", "record") {
                        handle {
                            call.respondText(Json.toJson(testRecord), ContentType.Application.Json)
                        }
                    }
                }
            }
        }.use {
            val client = KwormClient("localhost", TEST_SERVER_PORT)
            val downloadRecord = runBlocking { client.downloadRecord(testPath) }
            Assertions.assertEquals(testRecord, downloadRecord)
        }
    }

    @Test
    fun downloadContent() {
        val testPath = Random.nextFilePath()
        val testBytes = Random.nextBytes(TEST_DATA_LENGTH)
        testServer {
            routing {
                route(testPath) {
                    param("type", "content") {
                        handle { call.respondBytes(testBytes) }
                    }
                }
            }
        }.use {
            val client = KwormClient("localhost", TEST_SERVER_PORT)
            val downloadBytes = runBlocking { client.downloadContent(testPath).asBytes() }
            Assertions.assertArrayEquals(testBytes, downloadBytes)
        }
    }

    @Test
    fun uploadFile() {
        val (expectedRecord, expectedContent) = Random.nextFileRC()
        val latch = CountDownLatch(1)
        val atomicActualRecord = AtomicReference<FileRecord>()
        val atomicActualContent = AtomicReference<FileContent>()

        testServer {
            routing {
                route(expectedRecord.path, Post) {
                    handle {
                        val multipart = call.receiveMultipart()
                        val recordPart = multipart.readPart() as FormItem
                        val contentPart = multipart.readPart() as PartData.FileItem

                        atomicActualRecord.set(Json.fromJson(recordPart.value))
                        atomicActualContent.set(MemoryFileContent(contentPart.provider().use { it.readBytes() }))

                        latch.countDown()
                        call.respond("")
                    }
                }
            }
        }.use {
            val client = KwormClient("localhost", TEST_SERVER_PORT)
            runBlocking { client.uploadFile(expectedRecord, expectedContent) }
            latch.await()

            Assertions.assertEquals(expectedRecord, atomicActualRecord.get())
            println(expectedContent.asBytes().toList())
            println(atomicActualContent.get().asBytes().toList())
            Assertions.assertArrayEquals(expectedContent.asBytes(), atomicActualContent.get().asBytes())
        }
    }

}