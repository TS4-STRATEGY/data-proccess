package com.bestool.dataprocessor.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException

@RestController
@RequestMapping("/upload")
class FileUploadController {
    @Value("\${bestools.main-path}")
    private lateinit var mainPath: String

    @PostMapping
    fun uploadFile(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<String> {
        return try {
            // Ruta donde se guardará el archivo
            val uploadDir = File(mainPath)
            if (!uploadDir.exists()) {
                uploadDir.mkdirs() // Crear el directorio si no existe
            }

            // Guarda el archivo
            val destinationFile = File(uploadDir, file.originalFilename ?: "uploaded-file")
            file.inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            ResponseEntity.ok("Archivo subido exitosamente: ${destinationFile.absolutePath}")
        } catch (e: IOException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al subir el archivo: ${e.message}")
        }
    }
}