@file:Suppress("EXPERIMENTAL_API_USAGE")

package top.anagke.kwormhole.store

import kotlinx.coroutines.runBlocking
import top.anagke.kwormhole.DiskFileContent
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.net.KwormClient
import java.io.File

class RemoteStore(private val client: KwormClient) : Store {

    override fun listAll(): List<FileRecord> {
        return runBlocking { client.listFiles() }
    }

    override fun getRecord(path: String): FileRecord {
        return runBlocking { client.peekFile(path)!! }
    }

    override fun getContent(path: String): FileContent {
        val tmp = File.createTempFile("rs_", null)
        runBlocking { client.downloadFile(path, tmp) }
        return DiskFileContent(tmp)
    }

    override fun contains(path: String): Boolean {
        return runBlocking { client.peekFile(path) } != null
    }


    override fun store(metadata: FileRecord, content: FileContent) {
        runBlocking { client.uploadFile(metadata, content) }
    }

    override fun delete(path: String) {
        runBlocking { client.deleteFile(path) }
    }

}
