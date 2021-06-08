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
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
class MockKWormholeServer : Closeable {

    val host = "localhost"
    val port = Random.nextInt(10000, 65535)

    val records: MutableList<KFR> = Collections.synchronizedList(mutableListOf())
    val contents: MutableList<ByteArray> = Collections.synchronizedList(mutableListOf())

    val changes: BlockingQueue<KFR> = LinkedBlockingQueue()
    val wsEvents: BlockingQueue<KFR> = LinkedBlockingQueue()

    val server: ApplicationEngine = embeddedServer(CIO, host = host, port = port) {
        install(WebSockets)
        routing {
            webSocket("/all") {
                while (!Thread.interrupted()) {
                    val record = wsEvents.take()
                    val json = Gson().toJson(record)
                    outgoing.send(Frame.Text(json))
                }
            }
            route("/kfr/{...}", HttpMethod.Put) {
                handle {
                    val path = call.request.path().removePrefix("/kfr")
                    val size = call.request.headers[KFR.SIZE_HEADER_NAME]!!
                    val time = call.request.headers[KFR.TIME_HEADER_NAME]!!
                    val hash = call.request.headers[KFR.HASH_HEADER_NAME]!!
                    val record = KFR(path, time.toLong(), size.toLong(), hash.toLong())
                    val content = call.receive<ByteArray>()
                    records += record
                    contents += content
                    changes.put(record)
                    wsEvents.put(record)
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
                            call.respond(HttpStatusCode.OK, content)
                        }
                    }
                }
            }
        }
    }

    init {
        server.start()
    }


    fun mockRecord(): KFR {
        return mockPair().first
    }

    fun mockPair(): Pair<KFR, ByteArray> {
        val (record, content) = MockKFR.mockPair()
        records += record
        contents += content
        changes.put(record)
        wsEvents.put(record)
        return record to content
    }


    override fun close() {
        server.stop(0, 0)
    }

}