package top.anagke.kwormhole

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KwormholeApp {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<KwormholeApp>(*args)
        }
    }

}
