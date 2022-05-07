package ru.rofleksey.sunnyway.rest.component

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ShutdownManager(private val appContext: ApplicationContext) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(ShutdownManager::class.java)
    }

    fun shutdown(returnCode: Int) {
        log.error("Shutting down...")
        SpringApplication.exit(appContext, ExitCodeGenerator { returnCode })
    }
}