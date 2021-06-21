package top.anagke.kwormhole.config

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonModuleConfig {

    @Bean
    fun moduleKotlin(): Module {
        return KotlinModule()
    }

}