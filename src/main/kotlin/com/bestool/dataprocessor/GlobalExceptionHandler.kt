package com.bestool.dataprocessor

import oracle.jdbc.OracleDatabaseException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.sql.SQLException

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger("OracleErrorLogger")

    private fun logLastSQLContext() {
        val lastSql = DynamicSQLInspector.getLastSQL()
        if (!lastSql.isNullOrBlank()) {
            log.error("📝 Último SQL ejecutado antes del error: $lastSql")
        } else {
            log.warn("⚠️ No se pudo recuperar el último SQL.")
        }
    }

    @ExceptionHandler(SQLException::class)
    fun handleSQLException(ex: SQLException): ResponseEntity<String> {
        if (ex.message?.contains("ORA-") == true) {
            log.error("❌ ORACLE SQL EXCEPTION: ${ex.message}", ex)
            logLastSQLContext()
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error en base de datos")
    }

    @ExceptionHandler(OracleDatabaseException::class)
    fun handleOracleException(ex: OracleDatabaseException): ResponseEntity<String> {
        log.error("❌ ORACLE DATABASE EXCEPTION: ${ex.message}", ex)
        logLastSQLContext()
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error específico de Oracle")
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<String> {
        if (ex.message?.contains("ORA-") == true) {
            log.error("❌ EXCEPCIÓN CON ORA DETECTADA: ${ex.message}", ex)
            logLastSQLContext()
        } else {
            log.error("🚨 Excepción general capturada: ${ex.message}", ex)
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Ocurrió un error inesperado")
    }
}
