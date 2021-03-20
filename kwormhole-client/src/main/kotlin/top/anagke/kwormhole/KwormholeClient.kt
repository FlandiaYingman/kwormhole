package top.anagke.kwormhole

import com.xenomachina.argparser.ArgParser
import top.anagke.kwormhole.net.KwormClient
import top.anagke.kwormhole.store.LocalStore
import top.anagke.kwormhole.store.RemoteStore
import top.anagke.kwormhole.sync.KwormSync
import java.io.File

fun main(argv: Array<String>) {
    val args = ArgParser(argv).parseInto(::KwormholeClientArgs)
    val localStore = LocalStore(File(args.root))
    val remoteStore = RemoteStore(KwormClient(args.serverHost, args.serverPort))
    val sync = KwormSync(localStore, remoteStore)
    while (!Thread.interrupted()) {
        localStore.index()
        for (m in localStore.listAll()) {
            println("Indexed: $m")
        }

        sync.sync()
        Thread.sleep(3000)
    }
}