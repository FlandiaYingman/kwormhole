package top.anagke.kwormhole.service

import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.MemoryFileContent
import top.anagke.kwormhole.dao.ContentEntity
import top.anagke.kwormhole.dao.ContentRepository
import top.anagke.kwormhole.dao.RecordEntity
import top.anagke.kwormhole.dao.RecordRepository

@Service
class KFRService(
    private val recordRepo: RecordRepository,
    private val contentRepo: ContentRepository
) {
    //TODO: Transaction!

    fun all(): List<FileRecord> {
        return recordRepo.findAll().map { it.toRecord() }
    }

    fun head(path: String): FileRecord? {
        return recordRepo.findById(path).orElseGet { null }?.toRecord()
    }

    fun get(path: String): Pair<FileRecord, FileContent>? {
        val record = recordRepo.findById(path).orElseGet { null }?.toRecord()
        val content = contentRepo.findById(path).orElseGet { null }?.content?.binaryStream?.use { it.readBytes() }
        if ((record == null) != (content == null)) {
            TODO("The state of repos aren't same, could be fixed by enabling transaction")
        }
        return if (record == null && content == null) {
            null
        } else {
            record!! to MemoryFileContent(content!!)
        }
    }

    fun put(path: String, record: FileRecord, content: ByteArray) {
        if (path != record.path) {
            TODO("Throw an exception")
        }
        recordRepo.save(RecordEntity(record))
        contentRepo.save(ContentEntity(path, BlobProxy.generateProxy(content)))
    }

    operator fun contains(path: String): Boolean {
        val recordExistence = recordRepo.existsById(path)
        val contentExistence = contentRepo.existsById(path)
        if (recordExistence != contentExistence)
            TODO("The state of repos aren't same, could be fixed by enabling transaction")
        return recordExistence && contentExistence
    }

}