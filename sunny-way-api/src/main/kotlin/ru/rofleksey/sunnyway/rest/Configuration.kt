package ru.rofleksey.sunnyway.rest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Configuration
@AutoConfigureAfter(DispatcherServletAutoConfiguration::class)
open class Configuration {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(Configuration::class.java)
    }

    @Bean
    open fun indexRouter(@Value("file:ui/build/index.html") indexHtml: Resource): RouterFunction<ServerResponse> {
        return route(GET("/")) { request ->
            ok().contentType(MediaType.TEXT_HTML).bodyValue(indexHtml)
        }
    }

    @Bean
    open fun fileResourceRouter(): RouterFunction<ServerResponse> {
        return RouterFunctions.resources("/**", FileSystemResource("ui/build/"))
    }
}