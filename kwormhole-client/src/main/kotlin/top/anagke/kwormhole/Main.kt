package top.anagke.kwormhole

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.net.InetAddress
import java.nio.file.Path

fun main(args: Array<String>): Unit = mainBody {
    ArgParser(args)
        .parseInto(::Args)
        .run {
            val root = root
            val database = database

            val serverHost = host
            val serverPort = port

            KWormholeClient.open(root, database, serverHost, serverPort)
        }
}

private class Args(parser: ArgParser) {

    val database: Path by parser.storing(
        "-d", "--database",
        help = "sync database"
    ) { Path.of(this) }
        .default(Path.of("./kw_client.db"))

    val host by parser.storing(
        "-H", "--host",
        help = "server's host"
    )
        .default("localhost")
        .addValidator { InetAddress.getByName(value) }

    val port: Int by parser.storing(
        "-P", "--port",
        help = "server's port"
    ) { toInt() }
        .default(8080)
        .addValidator { check(value in 1 until 65535) { "invalid port $value" } }

    val root: Path by parser.positional(
        "ROOT",
        help = "sync root"
    ) { Path.of(this) }

}