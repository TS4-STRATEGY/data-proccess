package com.bestool.dataproccessor.utils

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.sql.SQLIntegrityConstraintViolationException

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<String> {
        val cause = ex.cause
        return if (cause is SQLIntegrityConstraintViolationException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body("Error: El registro ya existe en la base de datos.")
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error de base de datos: ${ex.message}")
        }
    }
}