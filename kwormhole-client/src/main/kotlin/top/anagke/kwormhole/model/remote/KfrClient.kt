package top.anagke.kwormhole.model.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.internal.EMPTY_BYTE_ARRAY
import okio.ByteString
import top.anagke.kio.createFile
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.util.fromJson
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * The network client of KWormhole.
 */
class KfrClient(
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
     * Fetches all existing suitable records and listens for incoming suitable records.
     * @param before require to fetch and listen all records before this time
     * @param after require to fetch and listen all records after this time
     */
    fun openConnection(before: Long? = null, after: Long? = null): KfrConnection {
        val url = newUrlBuilder()
            .addPathSegments("all")
            .apply { if (before != null) addQueryParameter("before", before.toString()) }
            .apply { if (after != null) addQueryParameter("after", after.toString()) }
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        return KfrConnection().apply { this.ws = client.newWebSocket(request, this) }
    }


    /**
     * Downloads the corresponding record.
     * @param path the path
     * @return the record object, or `null` if the record doesn't exist
     */
    fun head(path: String): Kfr? {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(path.removePrefix("/"))
            .build()
        val request = Request.Builder()
            .url(url)
            .head()
            .build()
        client.newCall(request).execute().use { response ->
            return when {
                response.isSuccessful -> fromHttpHeaders(path, response.headers)
                response.code == 404 -> null
                else -> TODO("Throw exception")
            }
        }
    }

    /**
     * Downloads the corresponding record, and downloads its content to disk.
     * @param path the path
     * @param file where the content to be downloaded into
     * @return the record object, or `null` if the record doesn't exist
     */
    fun get(path: String, file: File): Kfr? {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(path.removePrefix("/"))
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            return when {
                response.isSuccessful -> {
                    (response.body?.byteStream() ?: ByteArrayInputStream(EMPTY_BYTE_ARRAY))
                        .use { contentStream ->
                            file.createFile()
                            file.outputStream().use { fileStream -> contentStream.copyTo(fileStream) }
                        }
                    fromHttpHeaders(path, response.headers)
                }
                response.code == 404 -> null
                else -> TODO("Throw exception")
            }
        }
    }


    /**
     * Uploads a record and its content to the server.
     * @param record the record
     * @param content the content of the record
     */
    fun put(record: Kfr, content: File?) {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(record.path.removePrefix("/"))
            .build()
        val request = Request.Builder()
            .url(url)
            .headers(toHttpHeaders(record))
            .apply { if (record.exists()) put(content!!.asRequestBody()) }
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful.not()) throw IOException("unexpected response code: ${response.code}")
        }
    }

}

class KfrConnection : WebSocketListener(), Closeable {

    var open: Boolean = true
        private set

    var ws: WebSocket? = null
        internal set

    private val buf: BlockingQueue<Kfr> = LinkedBlockingQueue()

    private val gson: Gson = Gson()


    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val record = gson.fromJson<Kfr>(text) ?: TODO("log warning message, 'bad message: json is null'")
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


    fun take(): List<Kfr> {
        if (!open) {
            throw IOException("connection closed")
        }
        return List(buf.size) { buf.take() }.filterNotNull()
    }


    override fun close() {
        open = false
        ws?.close(1000, null)
    }

}
