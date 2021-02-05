package top.anagke.kwormhole.model

import java.time.Clock
import java.time.Instant

data class Metadata(
    val path: String,
    val hash: Long?,
    val time: Long
) {

    companion object {

        val utcTimeMillis get() = Instant.now(Clock.systemUTC()).toEpochMilli()

        fun deleted(metadata: Metadata): Metadata {
            return Metadata(metadata.path, metadata.hash, metadata.time)
        }

        fun Metadata.isPresent(): Boolean {
            return hash != null
        }

        fun Metadata.isAbsent(): Boolean {
            return hash == null
        }

    }

}