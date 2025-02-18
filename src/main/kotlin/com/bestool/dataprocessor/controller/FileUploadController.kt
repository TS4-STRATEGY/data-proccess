package com.bestool.dataprocessor.controller

import com.bestool.dataprocessor.BuildConfig
import com.bestool.dataprocessor.service.DirectoryProcessorService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException

@RestController
class FileUploadController(
) {
    private val mainPath = BuildConfig.MAIN_PATH
    private var logger = LoggerFactory.getLogger(DirectoryProcessorService::class.java)

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    @Operation(summary = "Subir un archivo", description = "Subir un archivo con título y descripción")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Archivo subido exitosamente"),
            ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            ApiResponse(responseCode = "500", description = "Error interno del servidor")
        ]
    )
    fun uploadFile(
        @Parameter(
            description = "Archivo a subir",
            required = true
        )
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<String> {
        logger.info("File: ${file.originalFilename}")
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body("No se recibió un archivo válido en la solicitud")
        }
        return try {
            // Verifica y crea el directorio de destino
            val uploadDir = File(mainPath)
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No se pudo crear el directorio de destino")
            }

            // Guarda el archivo
            val destinationFile = File(uploadDir, file.originalFilename ?: "uploaded-file")
            file.inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Devuelve el éxito
            ResponseEntity.ok("Archivo subido exitosamente a: ${destinationFile.absolutePath}")
        } catch (e: IOException) {
            // Maneja errores de IO
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al subir el archivo: ${e.message}")
        } catch (e: Exception) {
            // Maneja errores generales
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error inesperado: ${e.message}")
        }
    }

}
