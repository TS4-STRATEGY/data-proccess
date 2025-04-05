package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.repository.BTDetalleFacturaRepository
import com.bestool.dataprocessor.utils.UtilCaster.Companion.processFactura
import com.bestool.dataprocessor.utils.Utils.Companion.moveToProcessed
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class FacturasService(private val facturaRepository: BTDetalleFacturaRepository) {
    private val logger = LoggerFactory.getLogger(FacturasService::class.java)

    fun process(directory: File, processedDirectory: File, failedDirectory: File) {
        logger.info("2. PROCESANDO FACTURAS: ")
        val bills =
            directory.walkTopDown().filter { it.isFile && it.extension.equals("crt", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }.toList()
                .sortedBy { it.length() }

        bills.forEach { file ->
            processCRTFile(file, processedDirectory, failedDirectory)
        }
        logger.info("2. FACTURAS PROCESADAS")
    }

    private fun processCRTFile(file: File, processedDirectory: File, failedDirectory: File) {
        logger.info("Procesando archivo grande: ${file.name} (${file.length()} bytes)")

        file.bufferedReader().use { reader ->
            if (file.extension.equals("crt", ignoreCase = true)) {
                val records = mutableListOf<StringBuilder>() // Lista para almacenar registros
                var currentRecord = StringBuilder()

                // Leer línea por línea y agrupar registros
                reader.forEachLine { line ->
                    if (line.startsWith("OFF-")) {
                        // Si detectamos un nuevo registro, guardamos el anterior
                        if (currentRecord.isNotEmpty()) {
                            records.add(currentRecord)
                        }
                        currentRecord = StringBuilder(line) // Nuevo registro
                    } else {
                        // Agregar la línea al registro actual
                        currentRecord.append(" ").append(line.trim())
                    }
                }

                // Agregar el último registro
                if (currentRecord.isNotEmpty()) {
                    records.add(currentRecord)
                }

                // Procesar todos los registros y determinar el estado global
                val allSuccessful = records.all { record ->
                    processFactura(record.toString().replace("||", "|VACÍO|").split("|"), facturaRepository)
                }
                // Mover el archivo basado en el estado global
                if (allSuccessful) {
                    moveToProcessed(file, processedDirectory)
                } else {
                    moveToProcessed(file, failedDirectory)
                }
            }
        }

    }


}