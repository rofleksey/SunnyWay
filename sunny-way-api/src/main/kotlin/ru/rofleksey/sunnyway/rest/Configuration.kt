package ru.rofleksey.sunnyway.rest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
@AutoConfigureAfter(DispatcherServletAutoConfiguration::class)
open class Configuration : WebMvcConfigurer {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(Configuration::class.java)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/data/**").addResourceLocations("file:data/")
        registry.addResourceHandler("/**").addResourceLocations("file:ui/build/")
        log.info("Added static resources handlers")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/", "index.html")
    }
}