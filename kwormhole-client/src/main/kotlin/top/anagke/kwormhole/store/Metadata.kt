package top.anagke.kwormhole.store

import java.time.Clock
import java.time.Instant

/**
 * A metadata represents info of a file that stored in Kwormhole.
 * @property path The file path for indicating each different file.
 * @property hash The hash of the file. If null, the file is deleted.
 * @property time The last indexed time of this file as epoch millis.
 */
data class Metadata(
    val path: String,
    val hash: Long?,
    val time: Long
) {

    companion object {
        val utcEpochMillis get() = Instant.now(Clock.systemUTC()).toEpochMilli()

        fun dummy(path: String): Metadata {
            return Metadata(path, 0, Long.MIN_VALUE)
        }

        infix fun Metadata.isNewerThan(other: Metadata): Boolean {
            return this > other
        }

        infix fun Metadata.isOlderThan(other: Metadata): Boolean {
            return this < other
        }

        fun Metadata.isDeleted(): Boolean {
            return hash == null
        }
    }

    operator fun compareTo(other: Metadata): Int {
        return time.compareTo(other.time)
    }

}