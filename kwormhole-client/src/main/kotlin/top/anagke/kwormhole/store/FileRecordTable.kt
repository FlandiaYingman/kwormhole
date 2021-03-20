package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object FileRecordTable : IdTable<String>("file_record") {
    val path = varchar("path", 1024)
    val size = long("size")
    val hash = long("hash")
    val time = long("time")
    override val id: Column<EntityID<String>> = path.entityId()
    override val primaryKey get() = PrimaryKey(id)
}