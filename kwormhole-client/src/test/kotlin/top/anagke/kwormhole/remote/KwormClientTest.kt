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
import top.anagke.kwormhole.MemoryFileContent
import top.anagke.kwormhole.asBytes
import top.anagke.kwormhole.test.TEST_ENTRY_COUNT
import top.anagke.kwormhole.test.TEST_ENTRY_LENGTH
import top.anagke.kwormhole.test.TEST_FILE_NAME_LENGTH
import top.anagke.kwormhole.test.TEST_FILE_PATH_DEPTH
import top.anagke.kwormhole.test.TEST_SERVER_PORT
import top.anagke.kwormhole.test.fromJson
import top.anagke.kwormhole.test.nextFilePath
import top.anagke.kwormhole.test.nextFileRC
import top.anagke.kwormhole.test.nextFileRecord
import top.anagke.kwormhole.test.testServer
import top.anagke.kwormhole.test.toJson
import top.anagke.kwormhole.test.use
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

internal class KwormClientTest {

    @Test
    fun listFiles() {
        val expectedRecords = List(TEST_ENTRY_COUNT) { Random.nextFileRecord() }
        testServer {
            routing {
                route("/") {
                    handle {
                        call.respondText(toJson(expectedRecords), ContentType.Application.Json)
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
        val testRecord = Random.nextFileRecord()
        val testPath = testRecord.path
        testServer {
            routing {
                route(testPath) {
                    param("type", "record") {
                        handle {
                            call.respondText(toJson(testRecord), ContentType.Application.Json)
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
        val testPath = Random.nextFilePath(TEST_FILE_PATH_DEPTH, TEST_FILE_NAME_LENGTH)
        val testBytes = Random.nextBytes(TEST_ENTRY_LENGTH)
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

                        atomicActualRecord.set(fromJson(recordPart.value))
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