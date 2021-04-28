package top.anagke.kwormhole.model

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpHeaders
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.headersOf
import io.ktor.util.cio.writeChannel
import io.ktor.util.network.hostname
import io.ktor.util.network.port
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import top.anagke.kwormhole.DiskFileContent
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.shouldReplace
import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.util.fromJson
import java.io.File
import java.net.SocketAddress
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class RemoteModel(
    val remoteAddr: SocketAddress,
) : Model {

    private val logger = KotlinLogging.logger { }

    private val runner: Thread = thread(start = false) { run() }

    private val barrier = CyclicBarrier(2)

    private val kClient = KwormClient(remoteAddr.hostname, remoteAddr.port)

    init {
        runner.start()
        barrier.await()
    }

    private fun run() {
        val wsClient = HttpClient(CIO) {
            install(WebSockets)
        }
        try {
            barrier.await()
            val time = utcEpochMillis
            initChanges(time)
            monitorChanges(wsClient, time)
        } catch (ignore: InterruptedException) {
        } finally {
            wsClient.close()
        }
    }


    private fun initChanges(before: Long) {
        val remoteRecords = runBlocking { kClient.listFiles(before) }
        remoteRecords.forEach(this::submitChange)
    }

    private fun monitorChanges(wsClient: HttpClient, after: Long) = runBlocking {
        wsClient.ws(
            host = remoteAddr.hostname,
            port = remoteAddr.port,
            path = "/ws/change",
            request = {
                parameter("after", after)
            }
        ) {
            while (!Thread.interrupted()) {
                val frame = incoming.receive()
                require(frame is Frame.Text) { "The incoming frame $frame is required to be text." }

                val record = Gson().fromJson<FileRecord>(frame.readText())
                require(record != null) { "The incoming record $record is required to be non-null." }

                submitChange(record)
            }
        }
    }


    override val records: Map<String, FileRecord> by lazy { recordsMutable }
    private val recordsMutable: MutableMap<String, FileRecord> = ConcurrentHashMap()

    override val contents: Map<String, FileContentProvider> by lazy { contentsMutable }
    private val contentsMutable: MutableMap<String, FileContentProvider> = ConcurrentHashMap()

    override val changes: BlockingQueue<String> = LinkedBlockingQueue()


    private fun submitChange(record: FileRecord, manual: Boolean = false) {
        val existingRecord = records[record.path]
        if (record.shouldReplace(existingRecord)) {
            recordsMutable[record.path] = record
            contentsMutable[record.path] = { runBlocking { kClient.downloadContent(record.path) } }
            if (manual) {
                logger.info { "Submit manual change: $record" }
            } else {
                changes.offer(record.path)
                logger.info { "Submit change: $record" }
            }
        }
    }


    override fun put(record: FileRecord, content: FileContent) {
        runBlocking { kClient.uploadFile(record, content) }
        submitChange(record, manual = true)
    }


    override fun close() {
        runner.interrupt()
    }

    override fun toString(): String {
        return "RemoteModel(remoteAddr=$remoteAddr)"
    }

}

class KwormClient(
    private val host: String,
    private val port: Int,
    private val httpClient: HttpClient = HttpClient {
        install(JsonFeature)
    },
) {

    suspend fun listFiles(before: Long? = null): List<FileRecord> {
        return httpClient.get(host = host, port = port, path = "/") {
            before?.let { parameter("after", it) }
        }
    }

    /**
     * Downloads the corresponding record of a specified path.
     * @param path the specified path
     * @return the record object, or `null` if the record doesn't exist
     */
    suspend fun downloadRecord(path: String): FileRecord? {
        return httpClient.get(host = host, port = port, path = path) {
            parameter("type", "record")
        }
    }

    /**
     * Downloads the corresponding content of a specified path.
     * @param path the specified path
     * @return the content object, or `null` if the record doesn't exist
     */
    suspend fun downloadContent(path: String): FileContent {
        val temp = withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            File.createTempFile("kwormhole", null).also { it.deleteOnExit() }
        }
        httpClient.get<HttpStatement>(host = host, port = port, path = path) {
            parameter("type", "content")
        }.execute { response: HttpResponse ->
            val channel = response.receive<ByteReadChannel>()
            withContext(Dispatchers.IO) {
                channel.copyAndClose(temp.writeChannel(coroutineContext))
            }
        }
        return DiskFileContent(temp)
    }


    suspend fun uploadFile(record: FileRecord, content: FileContent) {
        val parts = formData {
            this.append(
                "record",
                Gson().toJson(record)
            )
            this.append(
                "content",
                InputProvider(content.length()) { content.openStream().asInput() },
                headersOf(HttpHeaders.ContentDisposition, "filename=")
            )
        }
        httpClient.post<Unit>(host = host, port = port, path = record.path) {
            body = MultiPartFormDataContent(parts)
        }
    }

}