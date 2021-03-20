package top.anagke.kwormhole.sync

import java.time.Instant

object Synchronizer {

    val utcEpochMillis get() = Instant.now().toEpochMilli()

}