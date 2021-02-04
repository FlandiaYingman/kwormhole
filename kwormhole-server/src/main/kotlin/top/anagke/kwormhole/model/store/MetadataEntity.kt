package top.anagke.kwormhole.model.store

import javax.persistence.Entity
import javax.persistence.Id

@Entity
internal data class MetadataEntity(
    @Id
    val path: String,
    val hash: Long,
    val hashNull: Boolean,
    val time: Long
) {
    constructor() : this("", 0, false, 0)
}