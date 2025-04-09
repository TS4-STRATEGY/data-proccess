package com.bestool.dataprocessor.utils


import com.bestool.dataprocessor.dto.ProgresoProceso
import com.bestool.dataprocessor.entity.RegistroFactura
import com.bestool.dataprocessor.repository.RegistroFacturaRepository
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

open class Utils {

    companion object {

        private val logger = LoggerFactory.getLogger(Utils::class.java)

        val monthMap = mapOf(
            "Jan" to "01", "Ene" to "01",
            "Feb" to "02",
            "Mar" to "03",
            "Apr" to "04", "Abr" to "04",
            "May" to "05",
            "Jun" to "06",
            "Jul" to "07",
            "Aug" to "08", "Ago" to "08",
            "Sep" to "09",
            "Oct" to "10",
            "Nov" to "11",
            "Dec" to "12", "Dic" to "12"
        )

        fun parseDateBill(dateString: String?): Date? {
            if (!dateString.isNullOrEmpty()) {
                try {
                    val parsedDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).parse(dateString)
                    return parsedDate
                } catch (_: ParseException) {
                    logger.warn("Failed to parse date with format: ${dateString.toPattern()}")
                }
            }
            return null
        }

        fun parseDate(dateString: String?): Date? {
            if (dateString.isNullOrEmpty()) {
                logger.error("Empty or null date string provided")
                return null
            }

            val normalizedDate = normalizeAndValidateDate(dateString)

            if (!normalizedDate.isNullOrEmpty()) {
                try {
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    return formatter.parse(normalizedDate)// "yyyy-MM-dd"
                } catch (_: ParseException) {
                    logger.error("Failed to parse normalized date: $normalizedDate")
                }
            }
            return null
        }


        private fun normalizeAndValidateDate(dateString: String?): String? {
            if (dateString.isNullOrEmpty()) {
                logger.error("Date string is null or empty")
                return null
            }

            try {
                // Limpiar y normalizar la fecha
                val cleanedDate = dateString.replace(".", "-")
                    .replace("/", "-")
                    .replace(" ", "-")
                    .trim()

                val datePatternStrict = Regex("^\\d{4}-\\d{2}-\\d{2}\$")
                val datePatternWithMonth =
                    Regex("(?i)\\d{4}-(Jan|Ene|Feb|Mar|Abr|Apr|May|Jun|Jul|Ago|Aug|Sep|Oct|Nov|Dic|Dec)-\\d{2}")

                // Caso 1: Fecha en formato estricto yyyy-MM-dd
                if (datePatternStrict.matches(cleanedDate)) {
                    val year = cleanedDate.substring(0, 4).toInt()
                    if (year in -4713..9999)
                        return cleanedDate
                    logger.error("Year out of range: $year in date string: $cleanedDate")
                    return null
                }

                // Caso 2: Fecha con nombres de mes yyyy-MMM-dd
                if (datePatternWithMonth.matches(cleanedDate)) {
                    val parts = cleanedDate.split("-")
                    val year = parts[0].toIntOrNull()
                    val month = parts[1].lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }
                    val day = parts[2]

                    // Validar y convertir el mes
                    val numericMonth = monthMap[month]
                    if (year != null && numericMonth != null && year in -4713..9999) {
                        return "$year-$numericMonth-$day"
                    } else {
                        logger.error("Invalid month or year: $cleanedDate")
                        return null
                    }
                }
                logger.error("Date string does not match expected formats: $cleanedDate")
                return null

            } catch (e: Exception) {
                logger.error("Error during date normalization: ${e.message}")
                return null
            }
        }

        fun moveToProcessed(file: File, processedDirectory: File) {
            try {
                if (file.exists()) {
                    val destination = File(processedDirectory, file.name)
                    Files.move(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    logger.info("Archivo movido: ${file.name}")
                } else {
                    logger.info("El archivo no existe: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                logger.error("Error moviendo archivo ${file.name}: ${e.message}")
                throw e
            }
        }

        fun removeDuplicates(files: List<File>): List<File> {
            val uniqueFilesMap = mutableMapOf<String, File>()

            files.forEach { file ->
                val key = "${file.nameWithoutExtension}-${file.length()}" // Clave única: nombre base y tamaño
                if (!uniqueFilesMap.containsKey(key)) {
                    uniqueFilesMap[key] = file // Agregar el primer archivo encontrado
                } else {
                    // Si ya existe en el mapa, es un duplicado
                    file.delete()
                    logger.info("Duplicado eliminado: ${file.path}")
                }
            }

            return uniqueFilesMap.values.toList() // Retornar solo archivos únicos
        }


        fun parseDoubleOrDefault(value: String, defaultValue: Double, fileName: String, lineNumber: String): Double {
            return try {
                value.toDouble()
            } catch (_: NumberFormatException) {
                logger.error(
                    "Invalid number format in file: $fileName, line: $lineNumber, value: $value. Using default value: $defaultValue"
                )
                defaultValue
            }
        }


        fun moveFilesToRoot(directory: File) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val targetFile = File(directory.parentFile, file.name)
                    if (!targetFile.exists()) {
                        file.copyTo(targetFile, overwrite = true)
                        logger.info("Archivo restaurado: ${file.name} a la raíz del directorio.")
                    }
                    file.delete()
                }
            }
        }


        fun ensureLogDirectoryExists(logDirectory: String): File {
            val dir = File(logDirectory)
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    logger.info("Directorio de logs creado: $logDirectory")
                }
            }
            return dir
        }

        fun buildTree(file: File, builder: StringBuilder, prefix: String) {
            builder.append(prefix).append(if (file.isDirectory) "[DIR] " else "[FILE] ").append(file.name).append("\n")

            if (file.isDirectory) {
                val children: Array<File> = file.listFiles()?.sortedBy { it.name }?.toTypedArray() ?: emptyArray()
                for (i in children.indices) {
                    val child = children[i]
                    val isLast = i == children.size - 1
                    val newPrefix = prefix + if (isLast) "+-- " else "|-- "
                    buildTree(child, builder, newPrefix)
                }
            }
        }


        fun listDirectoryTree(directory: File?): String {
            val builder = StringBuilder()
            if (directory != null) {
                if (!directory.exists() || !directory.isDirectory) {
                    throw IllegalArgumentException("El directorio especificado no existe o no es válido: $directory")
                }
                buildTree(directory, builder, "")
            }
            return builder.toString()
        }


        fun saveProgress(
            progreso: ProgresoProceso,
            registroFacturaRepository: RegistroFacturaRepository
        ): ProgresoProceso? {
            return try {
                val factura = progreso.factura ?: run {
                    val parts = progreso.archivo?.split("-") ?: emptyList()
                    parts.getOrNull(1).orEmpty() + parts.getOrNull(2).orEmpty().replace(".ll", "")
                }

                val registroExistente = registroFacturaRepository.findByFactura(factura)


                val nuevoRegistro = if (registroExistente != null) {
                    when (progreso.status) {
                        "COUNT" -> {
                            registroExistente.apply {
                                this.factura = factura
                                this.status = progreso.status
                                this.archivo = valorArchivoSeguro(registroExistente.archivo, progreso.archivo)
                                this.numeroLinea = 0
                                this.totalLinesFile =
                                    progreso.totalLinesFile.takeIf { it != null && it > 0 } ?: totalLinesFile
                                this.totalLinesInBase = 0
                            }
                        }

                        "DOWNLOAD" -> {
                            registroExistente.apply {
                                this.factura = factura
                                this.status = if (progreso.totalLinesInBase == totalLinesFile) "COMPLETED" else "ERROR"
                                this.archivo = valorArchivoSeguro(registroExistente.archivo, progreso.archivo)
                                this.numeroLinea = progreso.totalLinesInBase ?: 0
                                this.totalLinesFile = registroExistente.totalLinesFile.takeIf { it > 0 } ?: (progreso.totalLinesFile ?: 0)
                                this.totalLinesInBase = progreso.totalLinesInBase ?: totalLinesInBase
                            }
                        }

                        "PROGRESS" -> {
                            registroExistente.apply {
                                this.factura = factura
                                this.status = progreso.status
                                this.archivo = valorArchivoSeguro(registroExistente.archivo, progreso.archivo)
                                this.numeroLinea += progreso.numeroLinea ?: 0
                                this.totalLinesFile =
                                    progreso.totalLinesFile.takeIf { it != null && it > 0 } ?: totalLinesFile
                                this.totalLinesInBase = progreso.totalLinesInBase ?: totalLinesInBase
                            }
                        }

                        else -> {
                            registroExistente.apply {
                                this.factura = factura
                                this.archivo = valorArchivoSeguro(registroExistente.archivo, progreso.archivo)
                                this.status = progreso.status
                                this.numeroLinea = registroExistente.numeroLinea.takeIf { it > 0 } ?: (progreso.numeroLinea ?: numeroLinea)
                                this.totalLinesFile = registroExistente.totalLinesFile.takeIf { it > 0 } ?: (progreso.totalLinesFile ?: totalLinesFile)
                                this.totalLinesInBase = registroExistente.totalLinesInBase.takeIf { it > 0 } ?: (progreso.totalLinesInBase ?: totalLinesInBase)
                            }
                        }
                    }
                } else {
                    RegistroFactura(
                        factura = factura,
                        archivo = progreso.archivo ?: "desconocido.ll",
                        status = progreso.status,
                        numeroLinea = progreso.numeroLinea ?: 0,
                        totalLinesFile = progreso.totalLinesFile ?: 0,
                        totalLinesInBase = progreso.totalLinesInBase ?: 0
                    )
                }

                registroFacturaRepository.save(nuevoRegistro)

                logger.info("Progreso guardado en DB: $nuevoRegistro")
                progreso.copy(factura = factura)
            } catch (e: Exception) {
                logger.error("Error guardando progreso en DB: ${e.message}")
                null
            }
        }

        fun valorArchivoSeguro(vararg opciones: String?): String {
            return opciones.firstOrNull { !it.isNullOrBlank() } ?: "desconocido.ll"
        }

    }


}