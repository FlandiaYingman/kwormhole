package top.anagke.kwormhole.dao

import org.hibernate.Hibernate
import org.springframework.data.jpa.repository.JpaRepository
import java.sql.Blob
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class KfrFileEntity(
    @Id
    val path: String,
    @Lob
    val content: Blob,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as KfrFileEntity

        return path == other.path
    }

    override fun hashCode(): Int {
        return 1160945792
    }

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(path = $path)"
    }

}

interface KfrFileRepository : JpaRepository<KfrFileEntity, String>