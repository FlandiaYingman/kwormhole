package top.anagke.kwormhole.dao

import org.springframework.data.jpa.repository.JpaRepository
import top.anagke.kwormhole.FileRecord
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class RecordEntity(
    @Id
    val path: String,
    val size: Long,
    val time: Long,
    val hash: Long,
) {

    constructor(record: FileRecord) : this(record.path, record.size, record.time, record.hash)

    fun toRecord() = FileRecord(path, size, time, hash)

}

interface RecordRepository : JpaRepository<RecordEntity, String> {

    fun findAllByPathStartingWith(starting: String): List<RecordEntity>

    fun findByTimeBefore(before: Double): List<RecordEntity>

    fun findByTimeAfter(after: Double): List<RecordEntity>

}