@file:Suppress("EXPERIMENTAL_API_USAGE")

package top.anagke.kwormhole.remote

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpHeaders.ContentDisposition
import io.ktor.http.headersOf
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.anagke.kwormhole.DiskFileContent
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import java.io.File

class KwormClient(
    private val host: String,
    private val port: Int,
    private val httpClient: HttpClient = HttpClient {
        install(JsonFeature)
    },
) {

    suspend fun listFiles(): List<FileRecord> {
        return httpClient.get(host = host, port = port, path = "/")
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
                headersOf(ContentDisposition, "filename=")
            )
        }
        httpClient.post<Unit>(host = host, port = port, path = record.path) {
            body = MultiPartFormDataContent(parts)
        }
    }

}