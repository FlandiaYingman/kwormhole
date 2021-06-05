package top.anagke.kwormhole

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
class KwormholeApp {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<KwormholeApp>(*args)
        }
    }


    private val log = mu.KotlinLogging.logger {}

}
