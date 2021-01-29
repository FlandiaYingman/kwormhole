package top.anagke.kwormhole.store

import java.time.Clock
import java.time.Instant

data class Metadata(
    val path: String,
    val length: Long,
    val hash: Long,
    val time: Long = utcTimeMillis
) {

    companion object {
        val utcTimeMillis get() = Instant.now(Clock.systemUTC()).toEpochMilli()

        fun dummy(path: String): Metadata {
            return Metadata(path, 0, 0, Long.MIN_VALUE)
        }
    }

    operator fun compareTo(other: Metadata): Int {
        return time.compareTo(other.time)
    }

}