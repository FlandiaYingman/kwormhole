package top.anagke.kwormhole.model.store

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import top.anagke.kwormhole.model.Content
import top.anagke.kwormhole.model.Metadata
import java.io.File

@Component
class KwormStore {

    companion object {
        val DEFAULT_CONTENT_LOCATION = File("./store")
    }


    @Autowired private lateinit var metadataStore: MetadataStore

    private val contentStore: ContentStore = ContentStore(DEFAULT_CONTENT_LOCATION)


    private val tempMetadataMap = HashMap<String, Metadata>()

    private val tempContentMap = HashMap<String, Content>()


    @Synchronized
    fun count(): Long {
        return metadataStore.count().toLong()
    }

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
    fun putMetadata(path: String, metadata: Metadata) {
        if (metadata.hash == null) {
            metadataStore.putMetadata(path, metadata)
            if (contentStore.exists(path)) {
                contentStore.deleteContent(path)
            }
        }
        if (path in tempContentMap) {
            storeExisting(path, metadata, tempContentMap[path]!!)
            tempContentMap.remove(path)
            return
        }
        tempMetadataMap[path] = metadata
    }

    @Synchronized
    fun putContent(path: String, content: Content) {
        if (path in tempMetadataMap) {
            storeExisting(path, tempMetadataMap[path]!!, content)
            tempMetadataMap.remove(path)
            return
        }
        tempContentMap[path] = content
    }


    private fun storeExisting(path: String, metadata: Metadata, content: Content) {
        metadataStore.putMetadata(path, metadata)
        contentStore.putContent(path, content)
    }

}