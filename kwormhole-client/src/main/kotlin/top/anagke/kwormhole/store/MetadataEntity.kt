package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MetadataEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, MetadataEntity>(MetadataTable) {
        fun MetadataEntity.toObj(): Metadata {
            return Metadata(path, length, hash, time)
        }

        fun MetadataEntity.fromObj(metadata: Metadata) {
            if (path != metadata.path) throw IllegalArgumentException("Paths are not the same.")
            length = metadata.length
            hash = metadata.hash
            time = metadata.time
        }
    }

    var path by MetadataTable.path
    var length by MetadataTable.length
    var hash by MetadataTable.hash
    var time by MetadataTable.time
}