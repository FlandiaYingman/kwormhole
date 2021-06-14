package top.anagke.kwormhole.dao

import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import top.anagke.kwormhole.Kfr
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class KfrEntity(
    @Id
    val path: String,
    val size: Long,
    val time: Long,
    val hash: Long,
) {

    constructor(kfr: Kfr) : this(kfr.path, kfr.size, kfr.time, kfr.hash)

    fun asKfr() = Kfr(path, time, size, hash)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as KfrEntity

        return path == other.path
    }

    override fun hashCode(): Int {
        return 1195306756
    }

    override fun toString(): String {
        return this::class.simpleName + "(path = $path , size = $size , time = $time , hash = $hash )"
    }

}

interface KfrRepository : JpaRepository<KfrEntity, String> {

    fun findAllByPathStartingWith(starting: String): List<KfrEntity>

    fun findByTimeBefore(before: Double): List<KfrEntity>

    fun findByTimeAfter(after: Double): List<KfrEntity>

}