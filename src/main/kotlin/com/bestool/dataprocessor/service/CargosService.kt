package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.entity.BTDetalleCargos
import com.bestool.dataprocessor.repository.BTDetalleCargosRepository
import com.bestool.dataprocessor.utils.Utils.Companion.moveToProcessed
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.Date

@Service
class CargosService(private val cargosRepository: BTDetalleCargosRepository) {
    private val logger = LoggerFactory.getLogger(CargosService::class.java)

    fun process(directory: File, processedDirectory: File, failedDirectory: File) {
        logger.info("3. PROCESANDO CARGOS: ")
        val charges =
            directory.walkTopDown().filter { it.isFile && it.extension.equals("crg", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }.toList()
                .sortedBy { it.length() }

        charges.forEach { file ->
            processCRGFile(file,processedDirectory,failedDirectory)
        }
        logger.info("3. CARGOS PROCESADOS")
    }

    private fun processCRGFile(file: File, processedDirectory: File, failedDirectory: File) {
        logger.info("Procesando archivo: ${file.name} (${file.length()} bytes)")

        file.bufferedReader().useLines { lines ->
            val linesList = lines.toList() // Convertimos la secuencia a lista
            if (linesList.isEmpty()) {
                logger.error("El archivo ${file.name} está vacío y no se puede procesar.")
                moveToProcessed(file, failedDirectory)
                return
            }
            val allSuccessful = linesList.mapIndexed { index, line ->
                processCargos(
                    line.replace("||", "|VACÍO|").split("|"), file.name, index + 1
                )
            }.reduce { acc, result -> acc && result }
            if (allSuccessful) {
                moveToProcessed(file, processedDirectory)
            } else {
                moveToProcessed(file, failedDirectory)
            }

        }
    }

    fun processCargos(data: List<String>, fileName: String, lineNumber: Int): Boolean {
        return try {
            val numFactura = data[0]
            val operador = data[1]
            val tipoCargo = data[2]
            val monto = data[3].toBigDecimal()

            val idUnique = "$fileName:$lineNumber:$numFactura"

            val cargo = BTDetalleCargos(
                numFactura = numFactura,
                operador = operador,
                tipoCargo = tipoCargo,
                monto = monto,
                fechaRegistro = Date(),
                activo = 1,
                identificadorUnico = idUnique
            )
            // Guardar en la base de datos
            if (!cargosRepository.existsByIdentificadorUnico(idUnique)) {
                cargosRepository.save(cargo)
            }
            true
        } catch (e: Exception) {
            logger.error("Error procesando cargo en archivo $fileName línea $lineNumber: ${data.joinToString("|")}", e)
            false
        }
    }


}