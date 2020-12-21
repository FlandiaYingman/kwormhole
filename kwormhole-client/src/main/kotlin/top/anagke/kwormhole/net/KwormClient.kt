package top.anagke.kwormhole.net

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.contentType
import top.anagke.kwormhole.model.FileMetadata
import top.anagke.kwormhole.util.MiB
import top.anagke.kwormhole.util.contentRange
import top.anagke.kwormhole.util.suspendForEachBlock
import top.anagke.kwormhole.util.suspendReadBytes
import java.io.File

class KwormClient(
    private val host: String,
    private val httpClient: HttpClient = HttpClient {
        install(JsonFeature)
    }
) {

    suspend fun uploadFile(path: String, file: File, metadata: FileMetadata) {
        when {
            file.length() <= 4.MiB ->
                uploadSmallFile(path, file, metadata)
            else ->
                uploadLargeFile(path, file, metadata)
        }
    }

    private suspend fun uploadSmallFile(path: String, file: File, metadata: FileMetadata) {
        httpClient.put<Unit>(host = host, path = path) {
            contentType(ContentType.Application.OctetStream)
            parameter("method", "content")
            body = file.suspendReadBytes()
        }
        httpClient.put<Unit>(host = host, path = path) {
            contentType(ContentType.Application.Json)
            parameter("method", "metadata")
            body = metadata
        }
    }

    private suspend fun uploadLargeFile(path: String, file: File, metadata: FileMetadata) {
        var totalRead = 0
        val fileLength = file.length()
        file.suspendForEachBlock(4.MiB) { buffer, read ->
            httpClient.put<Unit>(host = host, path = path) {
                contentType(ContentType.Application.OctetStream)
                contentRange(totalRead until read + totalRead, fileLength)
                parameter("method", "content-ranged")
                body = buffer
            }
            totalRead += read
        }
        httpClient.put<Unit>(host = host, path = path) {
            contentType(ContentType.Application.Json)
            parameter("method", "metadata")
            body = metadata
        }
    }

}