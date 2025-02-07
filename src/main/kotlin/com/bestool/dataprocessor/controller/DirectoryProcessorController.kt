package com.bestool.dataprocessor.controller


import com.bestool.dataprocessor.BuildConfig
import com.bestool.dataprocessor.service.DirectoryProcessorService
import com.bestool.dataprocessor.utils.Utils.Companion.ensureLogDirectoryExists
import com.bestool.dataprocessor.utils.Utils.Companion.listDirectoryTree
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.FileInputStream
import java.io.IOException


@RestController
class DirectoryProcessorController(private val directoryProcessorService: DirectoryProcessorService) {


    @GetMapping("/process-directory")
    fun processDirectory(): String {
        directoryProcessorService.processDirectoryAsync()
             return """{"status": "success", "currentStatus": "RUNNING"}"""
    }

    @GetMapping("/restore-files")
    fun restoreFiles(): ResponseEntity<String> {
        return try {
            val result = directoryProcessorService.restoreFiles()
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: ${e.message}")
        }
    }


    private val maxLogSizeForInlineView = 5 * 1024 * 1024 // 5 MB

    @GetMapping("/last-error")
    fun getLastError(@RequestParam(required = false) lines: Int?): ResponseEntity<*> {
        return try {
            val logDirectory = ensureLogDirectoryExists(BuildConfig.LOG_PATH)
            if (logDirectory.exists() && logDirectory.isDirectory) {
                val lastModifiedFile = logDirectory.listFiles { file ->
                    file.isFile && file.name.startsWith("application.") // Filtra solo los logs relevantes
                }?.maxByOrNull { it.lastModified() }

                lastModifiedFile?.let { file ->
                    if (lines == null || lines <= 0 && file.length() > maxLogSizeForInlineView) {
                        // Si el archivo es demasiado grande, ofrecer descarga
                        val resource = InputStreamResource(FileInputStream(file))
                        val headers = HttpHeaders().apply {
                            add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${file.name}")
                        }
                        return ResponseEntity.ok()
                            .headers(headers)
                            .contentLength(file.length())
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource)
                    } else {
                        // Si el archivo es pequeño, mostrar contenido inline
                        val content = file.useLines { allLines ->
                            allLines.toList().takeLast(lines).joinToString("\n")
                        }

                        ResponseEntity.ok(content)
                    }
                } ?: ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No se encontraron archivos de log en la carpeta especificada")
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La carpeta de logs no existe o no es un directorio válido")
            }
        } catch (e: IOException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al buscar el archivo: ${e.message}")
        }
    }

    @GetMapping("/tree")
    fun getDirectoryTree(): ResponseEntity<String> {
        return try {
            val tree = listDirectoryTree(directoryProcessorService.directory)
            ResponseEntity.ok(tree)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: ${e.message}")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: ${e.message}")
        }
    }

    @GetMapping("/catalogos")
    fun getCatalogs(): String {
        directoryProcessorService.catalogs()
        return """{"status": "success", "currentStatus": "CATALOGS RUNNING"}"""
    }


    @GetMapping("/facturas")
    fun getBills(): String {
        directoryProcessorService.processBills()
        return """{"status": "success", "currentStatus": "BILLS RUNNING"}"""
    }


    @GetMapping("/cargos")
    fun getCharges(): String {
        directoryProcessorService.processCharges()
        return """{"status": "success", "currentStatus": "CHARGES RUNNING"}"""
    }

    @Operation(summary = "Actualizar programación del cron", description = "Actualiza la programación del cron utilizando todos los parámetros.")
    @PostMapping("/update-schedule")
    fun updateSchedule(
        @RequestParam
        @Schema(description = "Segundos (0-59)", example = "0", minimum = "0", maximum = "59")
        seconds: Int,

        @RequestParam
        @Schema(description = "Minutos (0-59)", example = "30", minimum = "0", maximum = "59")
        minutes: Int,

        @RequestParam
        @Schema(description = "Horas (0-23)", example = "14", minimum = "0", maximum = "23")
        hours: Int,

        @RequestParam
        @Schema(description = "Día del mes (1-31 o '*')", example = "*")
        dayOfMonth: String,

        @RequestParam
        @Schema(description = "Mes (1-12 o '*')", example = "*")
        month: String,

        @RequestParam
        @Schema(description = "Día de la semana (0-6 o '*', donde 0 = Domingo)", example = "*")
        dayOfWeek: String
    ): ResponseEntity<String> {
        return try {
            directoryProcessorService.updateCronSchedule(seconds, minutes, hours, dayOfMonth, month, dayOfWeek)
            ResponseEntity.ok("Programación actualizada: ${directoryProcessorService.getCronExpression()}")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body("Error: ${e.message}")
        }
    }

    @Operation(summary = "Consultar programación actual", description = "Devuelve la programación actual en un formato legible.")
    @GetMapping("/current-schedule")
    fun getCurrentSchedule(): ResponseEntity<String> {
        return ResponseEntity.ok(directoryProcessorService.getCronExpression())
    }


    @GetMapping("/tree-logs")
    fun getDirectoryTreeLogs(): ResponseEntity<String> {
        return try {
            val logDirectory = ensureLogDirectoryExists(BuildConfig.LOG_PATH)
            val tree = listDirectoryTree(logDirectory)
            ResponseEntity.ok(tree)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: ${e.message}")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: ${e.message}")
        }
    }

}
