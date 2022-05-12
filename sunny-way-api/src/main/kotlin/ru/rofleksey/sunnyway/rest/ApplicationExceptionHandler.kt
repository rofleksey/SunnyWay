package ru.rofleksey.sunnyway.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.rofleksey.sunnyway.nav.NavigationException
import ru.rofleksey.sunnyway.rest.dao.ErrorResponse

@RestControllerAdvice
open class ApplicationExceptionHandler {
    @ExceptionHandler(NavigationException::class)
    fun handleNavigationException(e: NavigationException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(HttpStatus.BAD_REQUEST, e.localizedMessage)
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }
}