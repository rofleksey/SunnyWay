package ru.rofleksey.sunnyway.rest.dao

import org.springframework.http.HttpStatus

data class ErrorResponse(val status: HttpStatus, val message: String)