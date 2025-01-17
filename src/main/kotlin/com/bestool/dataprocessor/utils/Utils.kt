package com.bestool.dataprocessor.utils


import com.bestool.dataprocessor.dto.ProgresoProceso
import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

open class Utils {

    companion object {

        private val logger = LoggerFactory.getLogger(Utils::class.java)
        val progresoFile = File("progreso.json")


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
                } catch (e: ParseException) {
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
                } catch (e: ParseException) {
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
            } catch (e: NumberFormatException) {
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

        fun splitFileBySizeAndLines(file: File, maxSizeInMB: Int): List<File> {
            val maxSizeInBytes = maxSizeInMB * 1024 * 1024 // Convertir MB a bytes
            val parts = mutableListOf<File>()

            try {
                var partNumber = 1
                var currentSize = 0L
                var currentPart: File? = null
                var writer: PrintWriter? = null

                file.useLines { lines ->
                    lines.forEach { line ->
                        val lineSize = line.toByteArray().size

                        // Crear un nuevo archivo si el tamaño acumulado supera el límite
                        if (currentSize + lineSize > maxSizeInBytes) {
                            writer?.close() // Cerrar el archivo anterior
                            currentPart =
                                File("${file.parent}/${file.nameWithoutExtension}_part$partNumber.${file.extension}")
                            writer = PrintWriter(currentPart)
                            parts.add(currentPart!!)
                            partNumber++
                            currentSize = 0L // Reiniciar el tamaño acumulado
                        }

                        // Escribir la línea en la parte actual
                        writer?.println(line)
                        currentSize += lineSize
                    }
                }
                writer?.close() // Asegúrate de cerrar el último archivo

                // Eliminar el archivo original si todas las partes se generaron correctamente
                if (parts.isNotEmpty()) {
                    if (file.delete()) {
                        logger.info("Archivo original eliminado con éxito: ${file.name}")
                    } else {
                        logger.error("No se pudo eliminar el archivo original: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error durante la partición del archivo: ${e.message}")
            }

            return parts
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

        fun calcularTotalLineas(
            archivosFactura: List<File>,
            archivosProcesados: List<File>,
            numFactura: String
        ): Long {
            val lineasFactura = archivosFactura.sumOf { archivo ->
                try {
                    archivo.useLines { it.count() }
                } catch (e: Exception) {
                    logger.error("Error leyendo archivo: ${archivo.name} -> ${e.message}")
                    0
                }
            }

            val lineasProcesadas = archivosProcesados
                .filter { it.name.contains(numFactura, ignoreCase = true) }
                .sumOf { archivo ->
                    try {
                        archivo.useLines { it.count() }
                    } catch (e: Exception) {
                        logger.error("Error leyendo archivo procesado: ${archivo.name} -> ${e.message}")
                        0
                    }
                }

            return lineasFactura.toLong() + lineasProcesadas.toLong()
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


        fun saveProgress(progreso: ProgresoProceso) {
            try {
                val mapper = jacksonObjectMapper()
                val progresoList: MutableList<ProgresoProceso> = if (progresoFile.exists()) {
                    mapper.readValue(progresoFile, object : TypeReference<MutableList<ProgresoProceso>>() {})
                } else {
                    mutableListOf()
                }

                // Actualizar o agregar progreso
                val index = progresoList.indexOfFirst { it.archivo == progreso.archivo }
                if (index != -1) {
                    progresoList[index] = progreso
                } else {
                    progresoList.add(progreso)
                }

                mapper.writeValue(progresoFile, progresoList)
                logger.info("Progreso guardado: $progreso")
            } catch (e: Exception) {
                logger.error("Error guardando progreso: ${e.message}")
            }
        }

        // Función para extraer el número de parte del nombre del archivo
        fun extractPartNumber(fileName: String): Int {
            val match = Regex("part(\\d+)", RegexOption.IGNORE_CASE).find(fileName)
            return match?.groups?.get(1)?.value?.toIntOrNull() ?: Int.MAX_VALUE
        }

        private fun compararRegistros(a: BTDetalleLlamadas?, b: BTDetalleLlamadas?): Boolean {
            return a != null && b != null &&
                    a.numFactura == b.numFactura &&
                    a.operador == b.operador &&
                    a.numOrigen == b.numOrigen &&
                    a.numDestino == b.numDestino &&
                    a.localidad == b.localidad &&
                    a.fechaLlamada == b.fechaLlamada &&
                    a.horaLlamada == b.horaLlamada &&
                    a.duracion == b.duracion &&
                    a.costo == b.costo &&
                    a.cargoAdicional == b.cargoAdicional &&
                    a.tipoCargo == b.tipoCargo &&
                    a.modalidad == b.modalidad &&
                    a.clasificacion == b.clasificacion
        }

    }

}