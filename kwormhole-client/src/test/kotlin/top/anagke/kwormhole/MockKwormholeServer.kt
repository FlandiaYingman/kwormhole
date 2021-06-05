package top.anagke.kwormhole

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import top.anagke.kwormhole.model.toHttpHeaders
import java.io.Closeable
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Suppress("BlockingMethodInNonBlockingContext")
class MockKwormholeServer : Closeable {

    val host = "localhost"
    val port = 8080

    val records: MutableList<FileRecord> = Collections.synchronizedList(mutableListOf())
    val contents: MutableList<FileContent> = Collections.synchronizedList(mutableListOf())

    val changes: BlockingQueue<FileRecord> = LinkedBlockingQueue()
    val wsQueue: BlockingQueue<FileRecord> = LinkedBlockingQueue()

    val server: ApplicationEngine = embeddedServer(CIO, host = host, port = port) {
        install(WebSockets)
        routing {
            webSocket("/all") {
                while (true) {
                    val record = wsQueue.take()
                    outgoing.send(Frame.Text(Gson().toJson(record)))
                }
            }
            route("/kfr/{...}", HttpMethod.Put) {
                handle {
                    val path = call.request.path().removePrefix("/kfr")
                    val size = call.request.headers[FileRecord.SIZE_HEADER_NAME]!!
                    val time = call.request.headers[FileRecord.TIME_HEADER_NAME]!!
                    val hash = call.request.headers[FileRecord.HASH_HEADER_NAME]!!
                    val record = FileRecord(path, size.toLong(), time.toLong(), hash.toLong())
                    val content = MemoryFileContent(call.receive())
                    records += record
                    contents += content
                    changes.put(record)
                    wsQueue.put(record)
                    call.respond(HttpStatusCode.OK)
                }
            }
            route("/kfr/{...}") {
                handle {
                    val path = call.request.path().removePrefix("/kfr")
                    val record = records.find { it.path == path }
                    val content = contents[records.indexOf(record)]
                    if (record == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        toHttpHeaders(record).forEach { (name, value) -> call.response.header(name, value) }
                        if (call.request.httpMethod == HttpMethod.Head) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.OK, content.asBytes())
                        }
                    }
                }
            }
        }
    }

    init {
        server.start()
    }


    fun mockRecord(): FileRecord {
        return mockBoth().first
    }

    fun mockBoth(): Pair<FileRecord, FileContent> {
        val (record, content) = MockKfr.mockBoth()
        records += record
        contents += content
        changes.put(record)
        wsQueue.put(record)
        return record to content
    }


    override fun close() {
        server.stop(0, 0)
    }

}