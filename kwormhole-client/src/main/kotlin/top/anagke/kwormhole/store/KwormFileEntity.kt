package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class KwormFileEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<KwormFileEntity>(KwormFileTable)

    var path by KwormFileTable.path
    var status by KwormFileTable.status
    var hash by KwormFileTable.hash
    var time by KwormFileTable.time
}