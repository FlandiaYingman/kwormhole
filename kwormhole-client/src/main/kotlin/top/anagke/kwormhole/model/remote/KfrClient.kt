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
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import top.anagke.kio.MiB
import top.anagke.kio.util.TempFiles
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Fraction
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.Range
import top.anagke.kwormhole.ThinKfr
import top.anagke.kwormhole.forEachSlice
import top.anagke.kwormhole.merge
import top.anagke.kwormhole.util.fromJson
import top.anagke.kwormhole.util.parseFormDisposition
import java.io.Closeable
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS

class KfrClient(
    private val serverHost: String,
    private val serverPort: Int,
) {

    private val client = OkHttpClient.Builder()
        .readTimeout(30, SECONDS)
        .writeTimeout(30, SECONDS)
        .callTimeout(30, SECONDS)
        .build()

    private fun newUrlBuilder(): HttpUrl.Builder {
        return HttpUrl.Builder()
            .host(serverHost)
            .port(serverPort)
            .scheme("http")
    }


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


    fun head(path: String): Kfr? {
        val thin = getThin(path, body = false) ?: return null
        return Kfr(thin)
    }

    fun get(path: String): FatKfr? {
        val first = getFirst(path) ?: return null
        if (first.isStandalone()) return first.merge()

        val temp = TempFiles.allocLocal()

        first.merge(temp)
        for (number in 1 until first.progress.denominator) {
            val thin = getThin(path, number = Math.toIntExact(number))!!
            thin.merge(temp)
        }

        val terminate = getThin(path, number = Math.toIntExact(first.progress.denominator))!!
        return terminate.merge(temp)
    }

    private fun getFirst(path: String): ThinKfr? {
        return getThin(path, number = 0)
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


    fun put(fat: FatKfr) {
        fat.forEachSlice(8.MiB) { thin ->
            putThin(thin)
        }
    }

    private fun putThin(thin: ThinKfr) {
        val url = newUrlBuilder()
            .addPathSegment("kfr")
            .addPathSegments(thin.path.removePrefix("/"))
            .build()
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


    private fun ThinKfr.fillForm(): MultipartBody {
        fun MultipartBody.Builder.addPart(name: String, body: RequestBody): MultipartBody.Builder {
            return this.addPart(Headers.headersOf("Content-Disposition", "form-data; name=\"$name\""), body)
        }
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart("kfr", Gson().toJson(Kfr(this)).toRequestBody("application/json".toMediaType()))
            .addPart("range", Gson().toJson(range).toRequestBody("application/json".toMediaType()))
            .addPart("progress", Gson().toJson(progress).toRequestBody("application/json".toMediaType()))
            .addFormDataPart("body", null, body().toRequestBody("application/octet-stream".toMediaType()))
            .build()
    }

    private fun ResponseBody.parseForm(): ThinKfr {
        var kfr: Kfr? = null
        var range: Range? = null
        var progress: Fraction? = null
        var body: ByteArray? = null
        MultipartReader(this).use { reader ->
            while (true) {
                val part = reader.nextPart() ?: break
                val name = parseFormDisposition(part.headers["Content-Disposition"].orEmpty()).name
                when (name) {
                    "kfr" -> kfr = Gson().fromJson<Kfr>(part.body.readString(UTF_8))
                    "range" -> range = Gson().fromJson<Range>(part.body.readString(UTF_8))
                    "progress" -> progress = Gson().fromJson<Fraction>(part.body.readString(UTF_8))
                    "body" -> body = part.body.readByteArray()
                }
            }
        }
        return ThinKfr(kfr!!, range!!, progress!!, body?.toByteString() ?: EMPTY)
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
