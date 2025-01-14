package com.bestool.dataprocessor.controller



import com.bestool.dataprocessor.service.DirectoryProcessorService
import com.bestool.dataprocessor.utils.Utils.Companion.ensureLogDirectoryExists
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.IOException


@RestController
class DirectoryProcessorController(private val directoryProcessorService: DirectoryProcessorService) {

    val logDirectory = ensureLogDirectoryExists("/u01/oracle/apps/bestool/logs/")

    @GetMapping("/process-directory")
    fun processDirectory(): String {
        // Inicia el proceso en segundo plano
        directoryProcessorService.processDirectoryAsync()
        val status = directoryProcessorService.readOrInitializeStatus()
        return """{"status": "success", "currentStatus": "$status"}"""
    }


    @GetMapping("/status")
    fun getStatus(): String {
        return directoryProcessorService.getStatus()
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


    @GetMapping("/last-error")
    fun getLastError(@RequestParam(required = false) lines: Int?): String {
        // Cambia la ruta a la carpeta de logs
        return try {
            if (logDirectory.exists() && logDirectory.isDirectory) {
                val lastModifiedFile = logDirectory.listFiles { file ->
                    file.isFile && file.name.startsWith("application.") // Filtra solo los logs relevantes
                }?.maxByOrNull { it.lastModified() }

                lastModifiedFile?.let { file ->
                    if (lines == null || lines <= 0) {
                        file.readText()
                    } else {
                        // Devuelve las últimas 'lines' líneas
                        file.useLines { allLines ->
                            allLines.toList().takeLast(lines).joinToString("\n")
                        }
                    }
                } ?: "No se encontraron archivos de log en la carpeta especificada"
            } else {
                "La carpeta de logs no existe o no es un directorio válido"
            }
        } catch (e: IOException) {
            "Error al buscar el archivo: ${e.message}"
        }
    }

    @GetMapping("/tree")
    fun getDirectoryTree(): ResponseEntity<String> {
        return try {
            val tree = directoryProcessorService.listDirectoryTree()
            ResponseEntity.ok(tree)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: ${e.message}")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: ${e.message}")
        }
    }


    @GetMapping("/tree-logs")
    fun getDirectoryTreeLogs(): ResponseEntity<String> {
        return try {
            val tree = directoryProcessorService.listDirectoryTree(logDirectory.path)
            ResponseEntity.ok(tree)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: ${e.message}")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: ${e.message}")
        }
    }

}
