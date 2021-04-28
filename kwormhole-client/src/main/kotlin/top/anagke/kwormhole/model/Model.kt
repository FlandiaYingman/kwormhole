package top.anagke.kwormhole.model

import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import java.io.Closeable
import java.util.concurrent.BlockingQueue

typealias FileContentProvider = () -> FileContent

interface Model : Closeable {

    val records: Map<String, FileRecord>

    val contents: Map<String, FileContentProvider>

    val changes: BlockingQueue<String>


    fun put(record: FileRecord, content: FileContent)

    override fun close()

}

