package top.anagke.kwormhole

import com.xenomachina.argparser.ArgParser
import top.anagke.kwormhole.model.local.LocalModel
import top.anagke.kwormhole.model.local.KfrDatabase
import top.anagke.kwormhole.model.remote.RemoteModel
import top.anagke.kwormhole.sync.Synchronizer
import java.io.Closeable
import java.io.File

fun main(args: Array<String>) {
    val parsedArgs = ArgParser(args).parseInto(::Args)

 //   KwormholeClient(parsedArgs.root,parsedArgs.database, parsedArgs.serverHost, parsedArgs.serverPort)
}

class KwormholeClient(
    root: String,
    database: String,
    serverHost: String,
    serverPort: Int
) : Closeable {

    private var localModel: LocalModel? = null
    private var remoteModel: RemoteModel? = null
    private var uploader: Synchronizer? = null
    private var downloader: Synchronizer? = null


    init {
        localModel = LocalModel(File(root), KfrDatabase(File(database)))
        remoteModel = RemoteModel(serverHost, serverPort)
        uploader = Synchronizer(localModel!!, remoteModel!!)
        downloader = Synchronizer(remoteModel!!, localModel!!)

        localModel!!.open()
        remoteModel!!.open()
    }

    override fun close() {
        localModel?.close()
        remoteModel?.close()
        uploader?.close()
        downloader?.close()
    }

}

private class Args(parser: ArgParser) {

    val root by parser.storing(
        "-r", "--root",
        help = "The root directory of Kwormhole Client."
    )

    val server by parser.storing(
        "-s", "--server",
        help = "The address of Kwormhole Server"
    )

    val serverHost by lazy {
        server.split(":")[0]
    }

    val serverPort by lazy {
        server.split(":")[1].toInt()
    }

    override fun toString(): String {
        return "Args(root='$root', serverHost='$serverHost', serverPort=$serverPort)"
    }

}