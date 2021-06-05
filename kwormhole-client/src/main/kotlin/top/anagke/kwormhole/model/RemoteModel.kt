package top.anagke.kwormhole.model

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.MemoryFileContent
import top.anagke.kwormhole.asBytes
import top.anagke.kwormhole.util.fromJson
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class RemoteModel(
    private val host: String,
    private val port: Int
) : AbstractModel() {

    private val kwormClient = KwormClient(host, port)

    private val wsAllSession = kwormClient.wsAll()


    override fun init(): List<Pair<FileRecord, FileContent>> {
        return emptyList()
    }

    override fun poll(): List<Pair<FileRecord, FileContent>> {
        val record = wsAllSession.buf.take()
        return if (record.submittable()) {
            val (serverRecord, serverContent) = kwormClient.get(record.path)
            listOf((serverRecord to serverContent))
        } else {
            emptyList()
        }
    }

    override fun onPut(record: FileRecord, content: FileContent) {
        kwormClient.put(record, content)
    }

    override fun close() {
        super.close()
        wsAllSession.close()
    }

    override fun toString(): String {
        return "RemoteModel(host=$host, port=$port)"
    }

}


class KwormClient(
    private val serverHost: String,
    private val serverPort: Int,
) {

    private val client = OkHttpClient()


    private fun newUrlBuilder(): HttpUrl.Builder {
        return HttpUrl.Builder()
            .host(serverHost)
            .port(serverPort)
            .scheme("http")
    }


    /**
     * Downloads all records.
     * @param before downloads all records before this time
     * @return a list containing the records
     */
    fun wsAll(before: Long? = null, after: Long? = null): WsAllSession {
        val url = newUrlBuilder()
            .addPathSegments("all")
            .apply { if (before != null) addQueryParameter("before", before.toString()) }
            .apply { if (after != null) addQueryParameter("after", after.toString()) }
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        return WsAllSession().apply { this.ws = client.newWebSocket(request, this) }
    }

    /**
     * Downloads the corresponding record of a specified path.
     * @param path the specified path
     * @return the record object, or `null` if the record doesn't exist
     */
    fun head(path: String): FileRecord {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(path.removePrefix("/"))
            .build()
        val request = Request.Builder()
            .url(url)
            .head()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return fromHttpHeaders(path, response.headers)
            } else {
                TODO("Throw exception")
            }
        }
    }

    /**
     * Downloads the corresponding content of a specified path.
     * @param path the specified path
     * @return the content object, or `null` if the record doesn't exist
     */
    fun get(path: String): Pair<FileRecord, MemoryFileContent> {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(path.removePrefix("/"))
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val record = fromHttpHeaders(path, response.headers)
            val content = MemoryFileContent(response.body!!.bytes())
            return record to content
        }
    }

    /**
     * Uploads the record and the content.
     * @param record the record to be upload
     * @param content the content to be upload
     */
    fun put(record: FileRecord, content: FileContent) {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(record.path.removePrefix("/"))
            .build()
        val request = Request.Builder()
            .url(url)
            .headers(toHttpHeaders(record))
            .put(content.asBytes().toRequestBody())
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful.not()) throw IOException("unexpected response code: ${response.code}")
        }
    }

}

class WsAllSession : WebSocketListener(), Closeable {

    var ws: WebSocket? = null

    val buf: BlockingQueue<FileRecord> = LinkedBlockingQueue()

    private val gson: Gson = Gson()

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val record = gson.fromJson<FileRecord>(text) ?: TODO("log warning message, 'bad message: json is null'")
            buf.put(record)
        } catch (e: Exception) {
            when (e) {
                is JsonSyntaxException,
                is JsonParseException -> {
                    TODO("log warning message, 'bad message: bad json")
                }
                else -> throw e
            }
        }

    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        TODO("log warning message, 'bad message: bytes format'")
    }

    override fun close() {
        ws?.close(1000, null)
    }

}

fun toHttpHeaders(record: FileRecord): Headers {
    return mapOf(
        FileRecord.SIZE_HEADER_NAME to record.size.toString(),
        FileRecord.TIME_HEADER_NAME to record.time.toString(),
        FileRecord.HASH_HEADER_NAME to record.hash.toString()
    ).toHeaders()
}

fun fromHttpHeaders(path: String, headers: Headers): FileRecord {
    val size = headers[FileRecord.SIZE_HEADER_NAME]!!
    val time = headers[FileRecord.TIME_HEADER_NAME]!!
    val hash = headers[FileRecord.HASH_HEADER_NAME]!!
    return FileRecord(path, size.toLong(), time.toLong(), hash.toLong())
}
