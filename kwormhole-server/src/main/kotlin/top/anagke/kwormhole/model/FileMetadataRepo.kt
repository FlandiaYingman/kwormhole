package top.anagke.kwormhole.model

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import top.anagke.kwormhole.util.urlPathToRepoFile
import top.anagke.kwormhole.util.urlPathToRepoFolder

// TODO: NOT TESTED 2020/12/16
interface FileMetadataRepo : JpaRepository<FileMetadataEntity, Int> {

    fun findByPath(path: String): FileMetadata?

    fun findAllByPathStartsWith(path: String, pageable: Pageable): List<FileMetadata>

    fun existsByPath(path: String): Boolean

    fun existsByPathStartingWith(path: String): Boolean


    fun getFileMetadata(path: String) =
        findByPath(path.urlPathToRepoFile())

    fun getFolderMetadata(path: String, start: Int, limit: Int) =
        findAllByPathStartsWith(path.urlPathToRepoFolder(), PageRequest.of(start, limit))

    fun existsFile(path: String) =
        existsByPath(path.urlPathToRepoFile())

    fun existsFolder(path: String) =
        existsByPathStartingWith(path.urlPathToRepoFolder())


    fun putFile(path: String, metadata: FileMetadata) =
        this.save(FileMetadataEntity(path, metadata.updateTime))

}