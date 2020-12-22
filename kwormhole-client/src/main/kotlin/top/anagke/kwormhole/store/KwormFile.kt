package top.anagke.kwormhole.store

data class KwormFile(
    val path: String,
    val metadata: FileMetadata
) {
    val hash get() = metadata.hash
    val updateTime get() = metadata.updateTime
}