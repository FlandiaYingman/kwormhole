package top.anagke.kwormhole.store

import java.time.Clock
import java.time.Instant

data class KwormFile(
    val path: String,
    val metadata: FileMetadata
) {

    companion object {
        val utcTimeMillis get() = Instant.now(Clock.systemUTC()).toEpochMilli()
    }

    val hash get() = metadata.hash
    val updateTime get() = metadata.updateTime
}