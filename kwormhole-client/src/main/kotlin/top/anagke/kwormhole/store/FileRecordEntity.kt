package top.anagke.kwormhole.store

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import top.anagke.kwormhole.FileRecord

class FileRecordEntity(id: EntityID<String>) : Entity<String>(id) {

    companion object : EntityClass<String, FileRecordEntity>(FileRecordTable) {

        fun FileRecordEntity.toObj(): FileRecord {
            return FileRecord(path, size, hash, time)
        }

        fun FileRecordEntity.fromObj(record: FileRecord) {
            require(this.path == record.path) { "The path ${this.path} and ${record.path} are required to be same." }
            size = record.size
            hash = record.hash
            time = record.time
        }

    }

    var path by FileRecordTable.path
    var size by FileRecordTable.size
    var hash by FileRecordTable.hash
    var time by FileRecordTable.time

}