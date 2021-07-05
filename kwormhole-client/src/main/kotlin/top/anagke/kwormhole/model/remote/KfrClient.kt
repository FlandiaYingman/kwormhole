package top.anagke.kwormhole.model.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import top.anagke.kio.MiB
import top.anagke.kio.util.TempFiles
import top.anagke.kwormhole.*
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

        val temp = TempFiles.allocLocal()

        if (first.isStandalone()) return first.merge(temp) { TempFiles.free(it) }!!

        first.merge(temp)
        for (number in 1 until first.progress.total) {
            val thin = getThin(path, number = number)!!
            thin.merge(temp)
        }

        val terminate = getThin(path, number = first.progress.total)!!
        return terminate.merge(temp) { TempFiles.free(it) }
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

    private fun ResponseBody.parseForm(): ThinKfr {
        var kfr: Kfr? = null
        var range: Range? = null
        var progress: Progress? = null
        var body: ByteArray? = null
        MultipartReader(this).use { reader ->
            while (true) {
                val part = reader.nextPart() ?: break
                val name = parseFormDisposition(part.headers["Content-Disposition"].orEmpty()).name
                when (name) {
                    "kfr" -> kfr = Gson().fromJson<Kfr>(part.body.readString(UTF_8))
                    "range" -> range = Gson().fromJson<Range>(part.body.readString(UTF_8))
                    "progress" -> progress = Gson().fromJson<Progress>(part.body.readString(UTF_8))
                    "body" -> body = part.body.readByteArray()
                }
            }
        }
        return unsafeThinKfr(kfr!!, range!!, progress!!, body ?: byteArrayOf())
    }


    fun put(fat: FatKfr) {
        fat.slicer(8.MiB).use {
            it.forEach { thin ->
                putThin(thin)
            }
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
            .addFormDataPart("body", null, this.move().toRequestBody("application/octet-stream".toMediaType()))
            .build()
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
