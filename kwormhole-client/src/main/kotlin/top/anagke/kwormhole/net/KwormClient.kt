@file:Suppress("EXPERIMENTAL_API_USAGE")

package top.anagke.kwormhole.net

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
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.copyTo
import io.ktor.utils.io.streams.asInput
import io.ktor.utils.io.streams.asOutput
import top.anagke.kwormhole.store.Metadata
import top.anagke.kwormhole.util.fromJson
import java.io.File

class KwormClient(
    private val host: String,
    private val port: Int,
    private val httpClient: HttpClient = HttpClient {
        install(JsonFeature)
    }
) {

    suspend fun listFiles(): List<Metadata> {
        return httpClient.get(host = host, port = port, path = "/")
    }

    suspend fun peekFile(path: String): Metadata? {
        return httpClient.get(host = host, port = port, path = path) {
            parameter("peek", "")
        }
    }

    suspend fun downloadFile(path: String, file: File): Metadata {
        val response = httpClient.get<HttpResponse>(host = host, port = port, path = path)
        val parts = response.receive<MultiPartData>()
        val metadataPart = (parts.readPart() ?: throw RuntimeException("Server error")) as PartData.FormItem
        val contentPart = (parts.readPart() ?: throw RuntimeException("Server error")) as PartData.FileItem

        val metadata = Gson().fromJson<Metadata>(metadataPart.value)!!
        contentPart.provider().use { input ->
            file.outputStream().asOutput().use { output ->
                input.copyTo(output)
            }
        }
        return metadata
    }

    suspend fun uploadFile(metadata: Metadata, file: File) {
        httpClient.post<Unit>(host = host, port = port, path = metadata.path) {
            body = MultiPartFormDataContent(
                formData {
                    this.append("metadata", Gson().toJson(metadata))
                    if (metadata.hash != null) {
                        this.append("content", InputProvider(file.length()) { file.inputStream().asInput() })
                    }
                }
            )
        }
    }

}