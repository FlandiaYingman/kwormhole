package top.anagke.kwormhole.net

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import top.anagke.kwormhole.store.KwormFile
import top.anagke.kwormhole.util.MiB
import top.anagke.kwormhole.util.contentRange
import top.anagke.kwormhole.util.suspendForEachBlock
import top.anagke.kwormhole.util.suspendReadBytes
import java.io.File

class KwormClient(
    private val host: String,
    private val port: Int,
    private val httpClient: HttpClient = HttpClient {
        install(JsonFeature)
    }
) {

    /**
     * Counts the files in the remote.
     * @return the count of files
     */
    suspend fun countFiles(): Int {
        return httpClient.get(host = host, port = port, path = "/count") {
        }
    }


    /**
     * Lists all files in the remote.
     * @param start the starting index of listing
     * @param limit the limit of listing
     * @return the file list
     */
    suspend fun listFiles(start: Int, limit: Int): List<KwormFile> {
        return httpClient.get(host = host, port = port, path = "/list") {
            parameter("start", start)
            parameter("limit", limit)
        }
    }


    /**
     * Downloads a remote file to a local file.
     * @param path the path of the remote file
     * @param file the local file
     * @return the metadata of the remote file
     */
    suspend fun downloadFile(path: String, file: File): KwormFile {
        httpClient.get<HttpStatement>(host = host, port = port, path = path) {
            parameter("method", "content")
        }.execute {
            val size = 4.MiB
            val buffer = ByteArray(size)
            val channel = it.receive<ByteReadChannel>()
            var read = 0
            file.outputStream().use {
                do {
                    it.write(buffer, 0, read)
                    read = channel.readAvailable(buffer, 0, size)
                } while (read >= 0)
            }
        }
        return httpClient.get(host = host, port = port, path = path) {
            parameter("method", "metadata")
        }
    }


    /**
     * Uploads a local file to remote.
     * @param metadata the metadata of the local file
     * @param file the path of the local file
     */
    suspend fun uploadFile(metadata: KwormFile, file: File) {
        when {
            file.length() <= 4.MiB ->
                uploadSmallFile(metadata, file)
            else ->
                uploadLargeFile(metadata, file)
        }
    }

    private suspend fun uploadSmallFile(metadata: KwormFile, file: File) {
        httpClient.put<Unit>(host = host, port = port, path = metadata.path) {
            contentType(ContentType.Application.OctetStream)
            parameter("method", "content")
            body = file.suspendReadBytes()
        }
        httpClient.put<Unit>(host = host, port = port, path = metadata.path) {
            contentType(ContentType.Application.Json)
            parameter("method", "metadata")
            body = metadata
        }
    }

    private suspend fun uploadLargeFile(metadata: KwormFile, file: File) {
        var totalRead = 0
        val fileLength = file.length()
        file.suspendForEachBlock(4.MiB) { buffer, read ->
            httpClient.put<Unit>(host = host, port = port, path = metadata.path) {
                contentType(ContentType.Application.OctetStream)
                contentRange(totalRead until read + totalRead, fileLength)
                parameter("method", "content-ranged")
                body = buffer
            }
            totalRead += read
        }
        httpClient.put<Unit>(host = host, port = port, path = metadata.path) {
            contentType(ContentType.Application.Json)
            parameter("method", "metadata")
            body = metadata
        }
    }

}