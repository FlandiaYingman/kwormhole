package top.anagke.kwormhole.view

import tornadofx.*

class KwormholeView : View("My View") {
    override val root = borderpane {
        center = listview<String> {

        }
    }
}
