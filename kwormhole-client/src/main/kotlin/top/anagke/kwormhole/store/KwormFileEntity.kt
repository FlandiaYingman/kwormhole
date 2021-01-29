package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.EntityID

class KwormFileEntity(id: EntityID<String>) : Entity<String>(id) {
    var path by KwormFileTable.path
    var status by KwormFileTable.status
    var hash by KwormFileTable.hash
    var time by KwormFileTable.time
}