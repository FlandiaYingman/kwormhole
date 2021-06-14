package top.anagke.kwormhole.service

import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.dao.KfrEntity
import top.anagke.kwormhole.dao.KfrFileEntity
import top.anagke.kwormhole.dao.KfrFileRepository
import top.anagke.kwormhole.dao.KfrRepository

@Service
class KfrService(
    private val kfrRepo: KfrRepository,
    private val contentRepository: KfrFileRepository
) {
    //TODO: Transaction!

    fun all(): List<Kfr> {
        return kfrRepo.findAll().map { it.asKfr() }
    }

    fun head(path: String): Kfr? {
        return kfrRepo.findById(path).orElseGet { null }?.asKfr()
    }

    fun get(path: String): Pair<Kfr, ByteArray>? {
        val record = kfrRepo.findById(path).orElseGet { null }?.asKfr()
        val content = contentRepository.findById(path).orElseGet { null }?.content?.binaryStream?.use { it.readBytes() }
        if ((record == null) != (content == null)) {
            TODO("The state of repos aren't same, could be fixed by enabling transaction")
        }
        return if (record == null && content == null) {
            null
        } else {
            record!! to content!!
        }
    }

    fun put(path: String, record: Kfr, content: ByteArray) {
        if (path != record.path) {
            TODO("Throw an exception")
        }
        kfrRepo.save(KfrEntity(record))
        contentRepository.save(KfrFileEntity(path, BlobProxy.generateProxy(content)))
    }

    operator fun contains(path: String): Boolean {
        val recordExistence = kfrRepo.existsById(path)
        val contentExistence = contentRepository.existsById(path)
        if (recordExistence != contentExistence)
            TODO("The state of repos aren't same, could be fixed by enabling transaction")
        return recordExistence && contentExistence
    }

}