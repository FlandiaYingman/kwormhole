package top.anagke.kwormhole.dao

import org.springframework.data.jpa.repository.JpaRepository
import top.anagke.kwormhole.Kfr
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

    constructor(record: Kfr) : this(record.path, record.size, record.time, record.hash)

    fun toRecord() = Kfr(path, time, size, hash)

}

interface RecordRepository : JpaRepository<RecordEntity, String> {

    fun findAllByPathStartingWith(starting: String): List<RecordEntity>

    fun findByTimeBefore(before: Double): List<RecordEntity>

    fun findByTimeAfter(after: Double): List<RecordEntity>

}