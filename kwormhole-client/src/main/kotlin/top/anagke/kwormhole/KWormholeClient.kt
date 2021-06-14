package top.anagke.kwormhole

import top.anagke.kwormhole.model.local.KfrDatabase
import top.anagke.kwormhole.model.local.KfrService
import top.anagke.kwormhole.model.local.LocalModel
import top.anagke.kwormhole.model.remote.RemoteModel
import top.anagke.kwormhole.sync.Synchronizer
import java.io.Closeable
import java.io.File

class KWormholeClient
private constructor(
    root: String,
    database: String,
    serverHost: String,
    serverPort: Int,
) : Closeable {

    companion object {
        fun open(
            root: String,
            database: String,
            serverHost: String,
            serverPort: Int,
        ): KWormholeClient {
            return KWormholeClient(root, database, serverHost, serverPort).apply {
                uploader = Synchronizer(localModel, remoteModel)
                downloader = Synchronizer(remoteModel, localModel)
                localModel.open()
                remoteModel.open()
            }
        }
    }

    private val localModel: LocalModel = LocalModel(KfrService(File(root), KfrDatabase(File(database))))
    private val remoteModel: RemoteModel = RemoteModel(serverHost, serverPort)

    private var uploader: Synchronizer? = null
    private var downloader: Synchronizer? = null

    override fun close() {
        localModel.close()
        remoteModel.close()
        uploader?.close()
        downloader?.close()
    }

}