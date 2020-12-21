package top.anagke.kwormhole

import top.anagke.kwormhole.view.KwormholeView
import tornadofx.*
import java.nio.file.Path
import java.nio.file.Paths

class Kwormhole : App(KwormholeView::class) {

    override val configPath: Path = Paths.get("./kwormhole.properties")

    override fun init() {
        config.set("syncDir" to listOf(""))
        config.save()
    }

}

fun main() {
    launch<Kwormhole>()
}