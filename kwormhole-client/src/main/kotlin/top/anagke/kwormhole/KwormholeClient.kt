package top.anagke.kwormhole

import com.xenomachina.argparser.ArgParser
import top.anagke.kwormhole.model.LocalModel
import top.anagke.kwormhole.model.RecordDatabase
import top.anagke.kwormhole.model.RemoteModel
import top.anagke.kwormhole.sync.Synchronizer
import java.io.File
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val parsedArgs = ArgParser(args).parseInto(::Args)

    val database = RecordDatabase(File("./kwormhole.db"))
    val localModel = LocalModel(File(parsedArgs.root), database)
    val remoteModel = RemoteModel(parsedArgs.serverHost, parsedArgs.serverPort)

    val uploader = Synchronizer(localModel, remoteModel)
    val downloader = Synchronizer(remoteModel, localModel)

    return
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