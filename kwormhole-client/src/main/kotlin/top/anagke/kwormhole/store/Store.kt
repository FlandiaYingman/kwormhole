package top.anagke.kwormhole.store

interface Store {

    fun list(): List<Metadata>


    fun getMetadata(path: String): Metadata

    fun getContent(path: String): Content

    fun exists(path: String): Boolean

    fun notExists(path: String): Boolean = !exists(path)


    fun store(metadata: Metadata, content: Content)

    fun delete(path: String)


    fun apply(patch: Metadata, store: Store) {
        val path = patch.path
        if (patch.length >= 0) {
            this.store(patch, store.getContent(path))
        } else {
            this.delete(path)
        }
    }

}