package top.anagke.kwormhole.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class FileMetadataEntity(
    val path: String = "",
    val time: Long = -1
) {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0

}