package top.anagke.kwormhole.model.store

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import top.anagke.kwormhole.model.Metadata
import top.anagke.kwormhole.model.Metadata.Companion.deleted

@Component
internal class MetadataStore {

    @Autowired
    private lateinit var repo: MetadataRepo


    fun getMetadata(path: String): Metadata {
        requireExists(path)
        return repo.findById(path).get().toKwormFile()
    }

    fun putMetadata(path: String, metadata: Metadata) {
        checkEquality(path, metadata)
        repo.save(metadata.toEntity())
    }

    fun deleteMetadata(path: String) {
        requireExists(path)
        repo.save(deleted(repo.findById(path).get().toKwormFile()).toEntity())
    }

    fun exists(path: String): Boolean {
        return repo.existsById(path)
    }

    fun notExists(path: String): Boolean {
        return !exists(path)
    }

    fun count(): Int {
        return repo.count().toInt()
    }

    fun list(): List<Metadata> {
        return repo.findAll().map { it.toKwormFile() }
    }


    private fun checkEquality(path: String, metadata: Metadata) {
        if (path != metadata.path) {
            throw IllegalArgumentException("The path '$path' and the metadata.path '${metadata.path}' are not equal.")
        }
    }

    private fun requireExists(path: String) {
        if (this.notExists(path)) {
            throw NoSuchElementException("The metadata with path '$path' doesn't exist.")
        }
    }


    private fun MetadataEntity.toKwormFile(): Metadata {
        return Metadata(path, if (hashNull) null else hash, time)
    }

    private fun Metadata.toEntity(): MetadataEntity {
        return MetadataEntity(path, hash ?: 0, hash == null, time)
    }

}