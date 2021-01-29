package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.id.IntIdTable

object KwormFileTable : IntIdTable("file_metadata") {
    val path = varchar("path", 255)
    val status = varchar("status", 16)
    val hash = long("hash")
    val time = long("time")
}