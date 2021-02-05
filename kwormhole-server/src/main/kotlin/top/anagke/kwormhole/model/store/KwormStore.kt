package top.anagke.kwormhole.model.store

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import top.anagke.kwormhole.model.Content
import top.anagke.kwormhole.model.Metadata
import top.anagke.kwormhole.model.Metadata.Companion.isAbsent
import top.anagke.kwormhole.model.Metadata.Companion.isPresent


@Component
class KwormStore {

    @Autowired
    private lateinit var metadataStore: MetadataStore

    @Autowired
    private lateinit var contentStore: ContentStore


    @Synchronized
    fun list(): List<Metadata> {
        return metadataStore.list()
    }

    @Synchronized
    fun contains(path: String): Boolean {
        return metadataStore.exists(path)
    }

    @Synchronized
    fun getMetadata(path: String): Metadata {
        return metadataStore.getMetadata(path)
    }

    @Synchronized
    fun getContent(path: String): Content {
        return contentStore.getContent(path)
    }

    @Synchronized
    fun put(path: String, metadata: Metadata, content: Content) {
        require(path == metadata.path) { "The path and the path of metadata are required to be same" }
        require(metadata.isPresent()) { "The metadata is required to be present" }
        metadataStore.putMetadata(path, metadata)
        contentStore.putContent(path, content)
    }

    @Synchronized
    fun delete(path: String, metadata: Metadata) {
        require(path == metadata.path) { "The path and the path of metadata are required to be same" }
        require(metadata.isAbsent()) { "The metadata is required to be absent" }
        metadataStore.putMetadata(path, metadata)
        contentStore.deleteContent(path)
    }

}