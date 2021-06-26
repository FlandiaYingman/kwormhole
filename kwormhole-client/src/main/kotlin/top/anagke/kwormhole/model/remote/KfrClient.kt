package top.anagke.kwormhole.model.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.MultipartReader
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import top.anagke.kio.MiB
import top.anagke.kio.util.TempFiles
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.ThinKfr
import top.anagke.kwormhole.forEachSlice
import top.anagke.kwormhole.isSingle
import top.anagke.kwormhole.merge
import top.anagke.kwormhole.util.fromJson
import top.anagke.kwormhole.util.parseFormDisposition
import java.io.Closeable
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
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
        val thin = getThin(path, body = false) ?: return null
        return thin.kfr
    }

    /**
     * Downloads the corresponding record, and downloads its content to disk.
     * @param path the path
     * @param file where the content to be downloaded into
     * @return the record object, or `null` if the record doesn't exist
     */
    fun get(path: String): FatKfr? {
        val first = getThin(path, number = 0) ?: return null
        if (first.isSingle()) return first.merge()
        val temp = TempFiles.allocLocal()
        first.merge(temp)
        for (number in 1 until first.total) {
            val thin = getThin(path, number = number)!!
            thin.merge(temp)
        }
        val terminate = getThin(path, number = first.total)!!
        return terminate.merge(temp) { TempFiles.free(temp) }!!
    }

    private fun getThin(path: String, body: Boolean = true, number: Int = 0): ThinKfr? {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(path.removePrefix("/"))
            .addQueryParameter("body", body.toString())
            .addQueryParameter("number", number.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            return when (response.code) {
                200 -> response.body!!.parseForm()
                404 -> null
                //TODO: more specific exception
                else -> throw IOException("response code ${response.code} is neither 200 nor 404")
            }
        }
    }


    /**
     * Uploads a record and its content to the server.
     * @param fat the fat KFR, containing the record and its body
     */
    fun put(fat: FatKfr) {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(fat.path.removePrefix("/"))
            .build()
        fat.forEachSlice(8.MiB) { thin ->
            System.err.println("$thin")
            val request = Request.Builder()
                .url(url)
                .put(thin.fillForm())
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful.not()) {
                    throw IOException("unexpected response code: ${response.code}")
                }
            }
        }
    }


    private fun ThinKfr.fillForm(): MultipartBody {
        fun MultipartBody.Builder.addPart(name: String, body: RequestBody): MultipartBody.Builder {
            return this.addPart(Headers.headersOf("Content-Disposition", "form-data; name=\"$name\""), body)
        }
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart("kfr", Gson().toJson(kfr).toRequestBody("application/json".toMediaType()))
            .addPart("number", Gson().toJson(number).toRequestBody("application/json".toMediaType()))
            .addPart("count", Gson().toJson(total).toRequestBody("application/json".toMediaType()))
            .addPart("range", Gson().toJson(range).toRequestBody("application/json".toMediaType()))
            .addFormDataPart("body", null, body.toRequestBody())
            .build()
    }

    private fun ResponseBody.parseForm(): ThinKfr {
        var kfr: Kfr? = null
        var number: Int? = null
        var count: Int? = null
        var range: ThinKfr.Range? = null
        var body: ByteArray? = null
        MultipartReader(this).use { reader ->
            while (true) {
                val part = reader.nextPart() ?: break
                val name = parseFormDisposition(part.headers["Content-Disposition"].orEmpty()).name
                when (name) {
                    "kfr" -> kfr = Gson().fromJson<Kfr>(part.body.readString(UTF_8))
                    "number" -> number = part.body.readString(UTF_8).toInt()
                    "count" -> count = part.body.readString(UTF_8).toInt()
                    "range" -> range = Gson().fromJson<ThinKfr.Range>(part.body.readString(UTF_8))
                    "body" -> body = part.body.readByteArray()
                }
            }
        }
        return ThinKfr(kfr!!, number!!, count!!, range!!, body ?: byteArrayOf())
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
