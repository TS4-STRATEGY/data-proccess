package com.bestool.dataproccessor.utils


import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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

        private val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
        )

        fun parseDateWithFallback(dateString: String?, lastValidDate: Date?): Date? {
            return parseDate(dateString) ?: run {
                logger.warn("Using fallback date: $lastValidDate for invalid input: $dateString")
                lastValidDate
            }
        }

        private fun parseDate(dateString: String?): Date? {
            if (dateString.isNullOrEmpty()) {
                logger.error("Empty or null date string provided")
                return null
            }

            val normalizedDate = normalizeAndValidateDate(dateString)

            if (normalizedDate != null) {
                logger.debug("Attempting to parse normalized date: $normalizedDate")

                // Validar formato explícitamente antes de parsear
                val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
                if (!datePattern.matches(normalizedDate)) {
                    logger.error("Date does not match expected format yyyy-MM-dd: $normalizedDate")
                    return null
                }

                try {
                    return dateFormats[0].parse(normalizedDate) // "yyyy-MM-dd"
                } catch (e: ParseException) {
                    logger.error("Failed to parse normalized date: $normalizedDate", e)
                }
            }

            logger.error("Unparseable date: $dateString. No valid format found.")
            return null
        }

        fun formatToSqlDate(date: java.util.Date?): java.sql.Date? {
            return date?.let { java.sql.Date(it.time) }
        }

            private fun normalizeAndValidateDate(dateString: String?): String? {
                if (dateString.isNullOrEmpty()) {
                    logger.error("Date string is null or empty")
                    return null
                }

                // Reemplazar puntos con guiones para normalizar el formato
                var cleanedDate = dateString.replace(".", "-")
                    .trim().replace(" ", "-")
                    .trim().replace("/", "-")

                val datePattern2 =
                    Regex("(?i)\\d{4}-(Jan|Ene|Feb|Mar|Abr|Apr|May|Jun|Jul|Ago|Aug|Sep|Oct|Nov|Dic|Dec)-\\d{2}\n")
                val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")

                if (!datePattern.matches(cleanedDate)) {
                    if (datePattern2.matches(cleanedDate)) {
                        val parts = cleanedDate.split("-")
                        val year = parts[0]
                        val month = parts[1].capitalize() // Asegura la capitalización correcta
                        val day = parts[2]

                        // Convertir el mes a su valor numérico
                        val numericMonth = monthMap[month]

                        if (numericMonth != null) {
                            cleanedDate = "$year-$numericMonth-$day" // Retorna la fecha convertida
                        }
                    } else {
                        logger.error("Date string is not in the expected format yyyy-MMM-dd: $cleanedDate")
                    }
                }


                // Validar año
                val year = cleanedDate.substring(0, 4).toIntOrNull()
                if (year == null || year !in -4713..9999) {
                    logger.error("Year out of range: $year in date string: $cleanedDate")
                    return null
                }

                return cleanedDate
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