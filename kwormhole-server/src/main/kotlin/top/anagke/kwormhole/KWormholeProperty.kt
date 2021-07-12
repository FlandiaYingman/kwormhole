package top.anagke.kwormhole

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:kwormhole.properties")
class KWormholeProperty(
    @Value("\${kwormhole.root}") val root: String
)