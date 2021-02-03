package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object MetadataTable : IdTable<String>("file_metadata") {
    val path = varchar("path", 255)
    val hash = long("hash")
    val hashNull = bool("hashNull").default(false)
    val time = long("time")
    override val id: Column<EntityID<String>> = path.entityId()
    override val primaryKey get() = PrimaryKey(id)
}