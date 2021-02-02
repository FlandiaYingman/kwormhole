@file:Suppress("EXPERIMENTAL_API_USAGE")

package top.anagke.kwormhole.store

import kotlinx.coroutines.runBlocking
import top.anagke.kwormhole.net.KwormClient
import java.io.File

class RemoteStore(private val client: KwormClient) : Store {

    override fun list(): List<Metadata> {
        return runBlocking { client.listFiles() }
    }

    override fun getMetadata(path: String): Metadata {
        return runBlocking { client.peek(path)!! }
    }

    override fun getContent(path: String): Content {
        val tmp = File.createTempFile("rs_", null)
        runBlocking { client.downloadFile(path, tmp) }
        return FileContent(tmp)
    }

    override fun exists(path: String): Boolean {
        return runBlocking { client.peek(path) } != null
    }

    override fun store(metadata: Metadata, content: Content) {
        runBlocking { client.uploadFile(metadata, (content as FileContent).file) }
    }

    override fun delete(path: String) {
        runBlocking { client.deleteFile(path) }
    }

}
