package top.anagke.kwormhole.model.store

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import top.anagke.kwormhole.model.Metadata
import top.anagke.kwormhole.model.Metadata.Companion.deleted

@Component
internal class MetadataStore {

    @Autowired
    private lateinit var repo: MetadataRepo


    fun list(): List<Metadata> {
        return repo.findAll().map { it.toKwormFile() }
    }

    fun getMetadata(path: String): Metadata {
        require(exists(path)) { "The path is required to exist" }
        return repo.findById(path).get().toKwormFile()
    }

    fun putMetadata(path: String, metadata: Metadata) {
        repo.save(metadata.toEntity())
    }

    fun deleteMetadata(path: String) {
        repo.save(deleted(repo.findById(path).get().toKwormFile()).toEntity())
    }

    fun exists(path: String): Boolean {
        return repo.existsById(path)
    }


    private fun MetadataEntity.toKwormFile(): Metadata {
        return Metadata(path, if (hashNull) null else hash, time)
    }

    private fun Metadata.toEntity(): MetadataEntity {
        return MetadataEntity(path, hash ?: 0, hash == null, time)
    }

}