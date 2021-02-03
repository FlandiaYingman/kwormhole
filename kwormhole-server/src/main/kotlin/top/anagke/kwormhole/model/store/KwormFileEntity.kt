package top.anagke.kwormhole.model.store

import javax.persistence.Entity
import javax.persistence.Id

@Entity
internal data class KwormFileEntity(
    @Id
    val path: String,
    val hash: Long,
    val hashNull: Boolean,
    val time: Long
) {
    constructor() : this("", 0, false, 0)
}