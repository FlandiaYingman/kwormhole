package top.anagke.kwormhole.model

import org.springframework.data.jpa.repository.JpaRepository
import java.sql.Blob
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class ContentEntity(
    @Id
    val path: String,
    @Lob
    val content: Blob,
)

interface ContentRepository : JpaRepository<ContentEntity, String>