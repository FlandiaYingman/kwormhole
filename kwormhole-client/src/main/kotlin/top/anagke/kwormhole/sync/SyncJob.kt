package top.anagke.kwormhole.sync

import top.anagke.kwormhole.store.KwormFile

data class SyncJob(
    val dest: Dest,
    val file: KwormFile
) {
    enum class Dest {
        LOCAL,
        REMOTE;
    }
}
