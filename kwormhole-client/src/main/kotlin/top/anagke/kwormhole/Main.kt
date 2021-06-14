package top.anagke.kwormhole

import com.xenomachina.argparser.ArgParser

fun main(args: Array<String>) {
    val parsedArgs = ArgParser(args).parseInto(::Args)
    val root = parsedArgs.root
    val database = parsedArgs.database
    val serverHost = parsedArgs.serverHost
    val serverPort = parsedArgs.serverPort

    KWormholeClient.open(root, database, serverHost, serverPort)
}

private class Args(parser: ArgParser) {

    val root by parser.storing(
        "-r", "--root",
        help = "The root directory of KWormhole Client."
    )

    val database by parser.storing(
        "-d", "--database",
        help = "The database location of KWormhole Client."
    )

    private val server by parser.storing(
        "-s", "--server",
        help = "The address (address:port) of KWormhole Server"
    )

    val serverHost by lazy {
        server.split(":")[0]
    }

    val serverPort by lazy {
        server.split(":")[1].toInt()
    }


    override fun toString(): String {
        return "Args(root='$root', database='$database', serverHost='$serverHost', serverPort=$serverPort)"
    }

}