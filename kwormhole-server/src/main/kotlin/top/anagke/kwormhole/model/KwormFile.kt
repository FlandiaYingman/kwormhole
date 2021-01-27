package top.anagke.kwormhole.model

import java.time.Clock
import java.time.Instant

data class KwormFile(
    val path: String,
    val hash: Long,
    val time: Long = utcTimeMillis
) {

    companion object {
        val utcTimeMillis get() = Instant.now(Clock.systemUTC()).toEpochMilli()
    }

}