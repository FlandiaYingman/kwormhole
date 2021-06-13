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
import top.anagke.kwormhole.model.remote.toHttpHeaders
import java.io.Closeable
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
class MockKWormholeServer : Closeable {

    val host = "localhost"
    val port = Random.nextInt(10000, 65535)

    val records: MutableList<Kfr> = Collections.synchronizedList(mutableListOf())
    val contents: MutableList<ByteArray> = Collections.synchronizedList(mutableListOf())

    val changes: BlockingQueue<Kfr> = LinkedBlockingQueue()
    val wsEvents: BlockingQueue<Kfr> = LinkedBlockingQueue()

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
                    val size = call.request.headers[Kfr.SIZE_HEADER_NAME]!!
                    val time = call.request.headers[Kfr.TIME_HEADER_NAME]!!
                    val hash = call.request.headers[Kfr.HASH_HEADER_NAME]!!
                    val record = Kfr(path, time.toLong(), size.toLong(), hash.toLong())
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


    fun mockRecord(): Kfr {
        return mockPair().first
    }

    fun mockPair(): Pair<Kfr, ByteArray> {
        val kfrContent = MockKfr.mockFatKfr()
        records += kfrContent.kfr
        contents += kfrContent.body()!!.toByteArray()
        changes.put(kfrContent.kfr)
        wsEvents.put(kfrContent.kfr)
        return kfrContent.kfr to kfrContent.body()!!.toByteArray()
    }


    override fun close() {
        server.stop(0, 0)
    }

}