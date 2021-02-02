package top.anagke.kwormhole.sync

import top.anagke.kwormhole.store.Metadata
import top.anagke.kwormhole.store.Store

class KwormSync(
    private val localStore: Store,
    private val remoteStore: Store
) {

    fun sync() {
        val localFiles = localStore.list().associateBy { it.path }.toMutableMap()
        val remoteFiles = remoteStore.list().associateBy { it.path }.toMutableMap()
        val localPatches = mutableListOf<Metadata>()
        val remotePatches = mutableListOf<Metadata>()
        for (key in (localFiles.keys + remoteFiles.keys)) {
            val localFile = localFiles[key] ?: Metadata.dummy(key)
            val remoteFile = remoteFiles[key] ?: Metadata.dummy(key)
            when {
                localFile > remoteFile -> remotePatches += localFile
                localFile > remoteFile -> localPatches += remoteFile
            }
        }
        localPatches.forEach { localStore.apply(it, remoteStore) }
        remotePatches.forEach { remoteStore.apply(it, localStore) }
    }

}