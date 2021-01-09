package top.anagke.kwormhole.sync

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import top.anagke.kwormhole.net.KwormClient
import top.anagke.kwormhole.store.FileStore
import top.anagke.kwormhole.store.KwormFile

// TODO: NOT TESTED 2021/1/9
@ExperimentalCoroutinesApi
class FileSync(
    private val fileStore: FileStore,
    private val kwormClient: KwormClient,
) {

    suspend fun sync() = withContext(IO) {
        val recordFiles = listLocalFiles()
        val remoteFiles = listRemoteFiles()
        val filePathMap = mutableMapOf<String, Pair<KwormFile?, KwormFile?>>()
        recordFiles.forEach { filePathMap[it.path] = it to filePathMap[it.path]?.second }
        remoteFiles.forEach { filePathMap[it.path] = filePathMap[it.path]?.first to it }

        val syncTasks = filePathMap.values.mapNotNull { (local, remote) ->
            when {
                local == null && remote != null ->
                    SyncJob(SyncJob.Dest.LOCAL, remote)
                remote == null && local != null ->
                    SyncJob(SyncJob.Dest.REMOTE, local)
                remote == null || local == null ->
                    null // Make compiler auto-cast to nonnull type
                local.hash != remote.hash && local.time > remote.time ->
                    SyncJob(SyncJob.Dest.REMOTE, local)
                local.hash != remote.hash && local.time < remote.time ->
                    SyncJob(SyncJob.Dest.LOCAL, remote)
                else ->
                    null
            }
        }
        performSync(syncTasks)
    }

    private suspend fun listLocalFiles(): List<KwormFile> = withContext(IO) {
        return@withContext fileStore.list().toList()
    }

    private suspend fun listRemoteFiles(): List<KwormFile> = withContext(IO) {
        val limit = 1000
        val filesCount = kwormClient.countFiles()
        val jobList = List(filesCount / limit + 1) { async { kwormClient.listFiles(it * limit, limit) } }
        jobList.awaitAll()
        return@withContext jobList.map { it.getCompleted() }.flatten()
    }

    private suspend fun performSync(updates: List<SyncJob>) {
        for (update in updates) {
            when (update.dest) {
                SyncJob.Dest.LOCAL -> {
                    val kwormPath = update.file.path
                    val filePath = fileStore.resolve(kwormPath)
                    val downloadedFile = kwormClient.downloadFile(kwormPath, filePath)
                    fileStore.store(downloadedFile)
                }
                SyncJob.Dest.REMOTE -> {
                    val kwormPath = update.file.path
                    val filePath = fileStore.resolve(kwormPath)
                    val kwormFile = fileStore.find(kwormPath)
                    kwormClient.uploadFile(kwormFile!!, filePath)
                }
            }
        }
    }

}