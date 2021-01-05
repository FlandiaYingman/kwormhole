package top.anagke.kwormhole.store

import org.jetbrains.exposed.sql.Table

object KwormFileTable : Table("file_metadata") {
    val path = varchar("path", 255)
    val hash = long("hash")
    val time = long("time")

    override val primaryKey = PrimaryKey(path)
}