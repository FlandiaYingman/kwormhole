@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package top.anagke.kwormhole.remote

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.network.hostname
import io.ktor.util.network.port
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.util.fromJson
import top.anagke.kwormhole.util.listFromJson
import java.io.Closeable
import java.net.SocketAddress
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class RemoteModel(
    val remoteAddr: SocketAddress,
) : Closeable {

    private val logger = KotlinLogging.logger { }

    private val barrier = CyclicBarrier(2)

    private val runner: Thread = thread {
        run()
    }


    companion object {
        fun newRemoteModel(remoteAddr: SocketAddress): RemoteModel {
            return RemoteModel(remoteAddr).apply {
                barrier.await()
            }
        }
    }

    val records: Map<String, FileRecord> by lazy { recordsMutable }
    private val recordsMutable: MutableMap<String, FileRecord> by lazy { ConcurrentHashMap() }

    val changes: BlockingQueue<FileRecord> = LinkedBlockingQueue()

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private fun init() {

    }

    private fun run() {
        try {
            init()
            barrier.await()

            runBlocking {
                retrieveAll()
                retrieveChanges()
            }
        } catch (ignore: InterruptedException) {
        } finally {
            client.close()
        }
    }

    private suspend fun retrieveAll() {
        client.ws(
            host = remoteAddr.hostname,
            port = remoteAddr.port,
            path = "/ws/all"
        ) {
            val frame = incoming.receive()
            require(frame is Frame.Text) { "The incoming frame $frame is required to be text." }

            val text = frame.readText()
            val records = Gson().listFromJson<List<FileRecord>>(text)
            records.forEach { record ->
                recordsMutable[record.path] = record
                changes.offer(record)
            }
            logger.info("Retrieve all: total ${records.size} records")
        }
    }

    private suspend fun retrieveChanges() {
        client.ws(
            host = remoteAddr.hostname,
            port = remoteAddr.port,
            path = "/ws/change"
        ) {
            while (!Thread.interrupted()) {
                val frame = incoming.receive()
                require(frame is Frame.Text) { "The incoming frame $frame is required to be text." }

                val record = Gson().fromJson<FileRecord>(frame.readText())
                require(record != null) { "The incoming record $record is required to be non-null." }

                recordsMutable[record.path] = record
                changes.offer(record)
                logger.info("Retrieve change: retrieve record $record")
            }
        }
    }


    override fun close() {
        runner.interrupt()
    }

}