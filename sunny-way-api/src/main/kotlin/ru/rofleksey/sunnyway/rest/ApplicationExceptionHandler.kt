package ru.rofleksey.sunnyway.rest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import ru.rofleksey.sunnyway.nav.NavigationException
import ru.rofleksey.sunnyway.rest.dao.ErrorResponse

@ControllerAdvice
open class ApplicationExceptionHandler : ResponseEntityExceptionHandler() {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(ApplicationExceptionHandler::class.java)
    }

    // TODO: handle other errors (e.g. invalid algorithm)
    @ExceptionHandler(value = [NavigationException::class])
    fun handleNavigationException(e: NavigationException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(HttpStatus.BAD_REQUEST, e.localizedMessage)
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }
}