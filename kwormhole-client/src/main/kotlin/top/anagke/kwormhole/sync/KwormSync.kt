package top.anagke.kwormhole.sync

import top.anagke.kwormhole.store.Metadata.Companion.isDeleted
import top.anagke.kwormhole.store.Metadata.Companion.isNewerThan
import top.anagke.kwormhole.store.Metadata.Companion.isOlderThan
import top.anagke.kwormhole.store.Store

class KwormSync(
    private val localStore: Store,
    private val remoteStore: Store
) {

    fun sync() {
        val localFiles = localStore.listAll().associateBy { it.path }.toMutableMap()
        val remoteFiles = remoteStore.listAll().associateBy { it.path }.toMutableMap()
        val localPatches = mutableListOf<Metadata>()
        val remotePatches = mutableListOf<Metadata>()
        for (key in (localFiles.keys + remoteFiles.keys)) {
            val localFile = localFiles[key] ?: Metadata.dummy(key)
            val remoteFile = remoteFiles[key] ?: Metadata.dummy(key)
            if (localFile.isDeleted() && remoteFile.isDeleted()) continue
            when {
                localFile isNewerThan remoteFile -> remotePatches += localFile
                localFile isOlderThan remoteFile -> localPatches += remoteFile
            }
        }
        localPatches.forEach { localStore.apply(it, remoteStore) }
        remotePatches.forEach { remoteStore.apply(it, localStore) }
    }

}