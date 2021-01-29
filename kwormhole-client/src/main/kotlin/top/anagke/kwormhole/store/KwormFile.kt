package top.anagke.kwormhole.store

import java.time.Clock
import java.time.Instant

data class KwormFile(
    val path: String,
    val status: Status,
    val hash: Long,
    val time: Long = utcTimeMillis
) {

    enum class Status {
        CREATED, DELETED;
    }

    companion object {
        val utcTimeMillis get() = Instant.now(Clock.systemUTC()).toEpochMilli()
    }

}