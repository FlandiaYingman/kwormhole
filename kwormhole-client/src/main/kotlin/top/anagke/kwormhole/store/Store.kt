package top.anagke.kwormhole.store

import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord

interface Store {

    fun listAll(): List<FileRecord>


    fun getRecord(path: String): FileRecord

    fun getContent(path: String): FileContent

    fun contains(path: String): Boolean


    fun store(metadata: FileRecord, content: FileContent)


    fun delete(path: String)


    fun apply(patch: Metadata, store: Store) {
        val path = patch.path
        if (patch.hash != null) {
            this.store(patch, store.getContent(path))
        } else {
            this.delete(patch)
        }
    }

}