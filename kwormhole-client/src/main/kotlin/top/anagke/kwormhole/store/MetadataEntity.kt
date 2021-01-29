package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MetadataEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MetadataEntity>(MetadataTable) {
        fun MetadataEntity.toObj(): Metadata {
            return Metadata(path, length, hash, time)
        }

        fun MetadataEntity.fromObj(metadata: Metadata) {
            path = metadata.path
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