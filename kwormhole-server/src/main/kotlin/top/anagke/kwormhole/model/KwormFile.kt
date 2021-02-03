package top.anagke.kwormhole.model

import java.time.Clock
import java.time.Instant

data class KwormFile(
    val path: String,
    val hash: Long?,
    val time: Long
) {

    companion object {
        val utcTimeMillis get() = Instant.now(Clock.systemUTC()).toEpochMilli()

        fun deleted(kwormFile: KwormFile): KwormFile {
            return KwormFile(kwormFile.path, kwormFile.hash, kwormFile.time)
        }
    }

}