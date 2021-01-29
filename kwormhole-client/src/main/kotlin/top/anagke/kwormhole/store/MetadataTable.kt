package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.id.IntIdTable

object MetadataTable : IntIdTable("file_metadata") {
    val path = varchar("path", 255)
    val length = long("length")
    val hash = long("hash")
    val time = long("time")
}