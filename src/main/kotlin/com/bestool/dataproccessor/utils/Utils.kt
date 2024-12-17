package com.bestool.dataproccessor.utils


import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.ParseException
import java.util.*
import java.text.SimpleDateFormat

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

        fun parseDateWithFallback(dateString: String?, lastValidDate: Date?): Date? {
            try {
                return parseDate(dateString) ?: run {
                    logger.warn("Using fallback date: $lastValidDate for invalid input: $dateString")
                    lastValidDate
                }
            } catch (e: Exception) {
                logger.error("Unparseable date: $dateString No valid format found.")
                return null
            }

        }

        private fun parseDate(dateString: String?): Date? {
            if (dateString.isNullOrEmpty()) {
                logger.error("Empty or null date string provided")
                return null
            }

            val normalizedDate = normalizeAndValidateDate(dateString)

            if (!normalizedDate.isNullOrEmpty()) {
                try {
                    logger.debug("Attempting to parse normalized date: $normalizedDate")
                    // Validar formato explícitamente antes de parsear
                    val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
                    if (!datePattern.matches(normalizedDate)) {
                        logger.error("Date does not match expected format yyyy-MM-dd: $normalizedDate")
                        return null
                    }

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
                var cleanedDate = dateString.replace(".", "-")
                    .replace("/", "-")
                    .replace(" ", "-")
                    .trim()

                val datePatternStrict = Regex("^\\d{4}-\\d{2}-\\d{2}\$")
                val datePatternWithMonth = Regex("(?i)\\d{4}-(Jan|Ene|Feb|Mar|Abr|Apr|May|Jun|Jul|Ago|Aug|Sep|Oct|Nov|Dic|Dec)-\\d{2}")

                // Caso 1: Fecha en formato estricto yyyy-MM-dd
                if (datePatternStrict.matches(cleanedDate)) {
                    val year = cleanedDate.substring(0, 4).toInt()
                    if (year in -4713..9999) return cleanedDate
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
            val target = File(processedDirectory, file.name)
            Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            logger.info("Archivo movido a procesados: ${file.name}")
        }


        fun parseDoubleOrDefault(value: String, defaultValue: Double, fileName: String, lineNumber: String): Double {
            return try {
                value.toDouble()
            } catch (e: NumberFormatException) {
                logger.error(
                    "Invalid number format in file: $fileName, line: $lineNumber, value: $value. Using default value: $defaultValue",
                    e
                )
                defaultValue
            }
        }

        fun getProcessedFiles(processedFilesFile: File): Set<String> {
            if (!processedFilesFile.exists()) {
                processedFilesFile.createNewFile()
            }
            return processedFilesFile.readLines().toSet()
        }

        fun markFileAsProcessed(processedFilesFile: File, fileName: String) {
            processedFilesFile.appendText("$fileName\n")
        }

        fun detectarDelimitador(line: String): String? {
            val posiblesDelimitadores = listOf("|", ",", ";", "\t", " ")
            return posiblesDelimitadores.firstOrNull { line.contains(it) }
        }


    }
}