package top.anagke.kwormhole.store

interface Store {

    fun list(): List<Metadata>


    fun getMetadata(path: String): Metadata

    fun getContent(path: String): Content

    fun exists(path: String): Boolean


    fun store(metadata: Metadata, content: Content)

    fun delete(metadata: Metadata)


    fun apply(patch: Metadata, store: Store) {
        val path = patch.path
        if (patch.hash != null) {
            this.store(patch, store.getContent(path))
        } else {
            this.delete(patch)
        }
    }

}