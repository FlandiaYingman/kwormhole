package top.anagke.kwormhole

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.pattern.PathPatternParser

@Configuration
@EnableWebMvc
@PropertySource("classpath:kwormhole.properties")
class KwormholeConfig : WebMvcConfigurer {

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.setPatternParser(PathPatternParser.defaultInstance)
    }
}