package top.anagke.kwormhole

import com.xenomachina.argparser.ArgParser

class KwormholeClientArgs(parser: ArgParser) {

    val root by parser.storing(
        "-r", "--root",
        help = "The root directory of Kwormhole Client."
    )

    private val server by parser.storing(
        "-s", "--server",
        help = "The address of Kwormhole Server"
    )

    val serverHost by lazy {
        server.split(":")[0]
    }

    val serverPort by lazy {
        server.split(":")[1].toInt()
    }

}