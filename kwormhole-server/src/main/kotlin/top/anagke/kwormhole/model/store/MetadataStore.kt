package top.anagke.kwormhole.model.store

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import top.anagke.kwormhole.model.KwormFile
import top.anagke.kwormhole.model.KwormFile.Companion.deleted

@Component
internal class MetadataStore {

    @Autowired
    private lateinit var repo: KwormFileRepo


    fun getMetadata(path: String): KwormFile {
        requireExists(path)
        return repo.findById(path).get().toKwormFile()
    }

    fun putMetadata(path: String, metadata: KwormFile) {
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

    fun list(): List<KwormFile> {
        return repo.findAll().map { it.toKwormFile() }
    }


    private fun checkEquality(path: String, metadata: KwormFile) {
        if (path != metadata.path) {
            throw IllegalArgumentException("The path '$path' and the metadata.path '${metadata.path}' are not equal.")
        }
    }

    private fun requireExists(path: String) {
        if (this.notExists(path)) {
            throw NoSuchElementException("The metadata with path '$path' doesn't exist.")
        }
    }


    private fun KwormFileEntity.toKwormFile(): KwormFile {
        return KwormFile(path, if (hashNull) null else hash, time)
    }

    private fun KwormFile.toEntity(): KwormFileEntity {
        return KwormFileEntity(path, hash ?: 0, hash == null, time)
    }

}