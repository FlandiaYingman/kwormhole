package top.anagke.kwormhole.model.store

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import top.anagke.kwormhole.model.KwormFile
import java.io.File

@Component
class KwormStore {

    companion object {
        val DEFAULT_CONTENT_LOCATION = File("./store")
    }


    @Autowired private lateinit var metadataStore: MetadataStore

    private val contentStore: ContentStore = ContentStore(DEFAULT_CONTENT_LOCATION)


    private val tempMetadataMap = HashMap<String, KwormFile>()

    private val tempContentMap = HashMap<String, ByteArray>()


    @Synchronized
    fun count(): Long {
        return metadataStore.count().toLong()
    }

    @Synchronized
    fun list(): List<KwormFile> {
        return metadataStore.list()
    }

    @Synchronized
    fun contains(path: String): Boolean {
        return metadataStore.exists(path)
    }

    @Synchronized
    fun getMetadata(path: String): KwormFile {
        return metadataStore.getMetadata(path)
    }

    @Synchronized
    fun getContent(path: String): ByteArray {
        return contentStore.getContent(path)
    }

    @Synchronized
    fun putMetadata(path: String, metadata: KwormFile) {
        if (path in tempContentMap) {
            storeExisting(path, metadata, tempContentMap[path]!!)
            tempContentMap.remove(path)
            return
        }
        tempMetadataMap[path] = metadata
    }

    @Synchronized
    fun putContent(path: String, content: ByteArray) {
        if (path in tempMetadataMap) {
            storeExisting(path, tempMetadataMap[path]!!, content)
            tempMetadataMap.remove(path)
            return
        }
        tempContentMap[path] = content
    }

    @Synchronized
    fun delete(path: String) {
        metadataStore.deleteMetadata(path)
        contentStore.deleteContent(path)
    }


    private fun storeExisting(path: String, metadata: KwormFile, content: ByteArray) {
        metadataStore.putMetadata(path, metadata)
        contentStore.putContent(path, content)
    }

}