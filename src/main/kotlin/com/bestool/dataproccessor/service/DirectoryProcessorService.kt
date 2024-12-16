package com.bestool.dataproccessor.service

import com.bestool.dataproccessor.entity.*
import com.bestool.dataproccessor.repository.*
import com.bestool.dataproccessor.utils.Utils.Companion.detectarDelimitador
import com.bestool.dataproccessor.utils.Utils.Companion.getProcessedFiles
import com.bestool.dataproccessor.utils.Utils.Companion.markFileAsProcessed
import com.bestool.dataproccessor.utils.Utils.Companion.moveToProcessed
import com.bestool.dataproccessor.utils.Utils.Companion.parseDateWithFallback
import com.bestool.dataproccessor.utils.Utils.Companion.parseDoubleOrDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Service
class DirectoryProcessorService(
    @Value("\${directory.input.path}") private val directoryPath: String,
    @Value("\${directory.processed.path}") private val processedPath: String,
    private val catPoblacionRepository: CatPoblacionRepository,
    private val catTipoLlamadaRepository: CatTipoLlamadaRepository,
    private val facturaRepository: BTDetalleFacturaRepository,
    private val cargosRepository: BTDetalleCargosRepository,
    private val llamadasRepository: BTDetalleLlamadasRepository
    ) {

    private val logger = LoggerFactory.getLogger(DirectoryProcessorService::class.java)
    private val statusFile = File(directoryPath, "processing_status.txt")
    private val processedFilesFile = File("$processedPath/processed_files.txt")

    private var localidadesCache = ConcurrentHashMap<String, CatPoblacion>()
    private var modalidadesCache = ConcurrentHashMap<String, CatTipoLlamada>()


    init {
        statusFile.writeText("IDLE")
    }

    @Async
    fun precargarCatalogos() {
        try {
            localidadesCache.putAll(
                catPoblacionRepository.findAll()
                    .associateBy({ it.descripcion.uppercase(Locale.getDefault()) }, { it })
            )

            modalidadesCache.putAll(
                catTipoLlamadaRepository.findAll()
                    .associateBy({ it.descripcion.uppercase(Locale.getDefault()) }, { it })
            )

            logger.info("Localidades y modalidades precargadas en caché.")
        } catch (e: Exception) {
            logger.error("Error al precargar los catálogos en caché", e)
        }
    }

    @Async
    fun processDirectoryAsync() {
        precargarCatalogos()
        val currentStatus = readOrInitializeStatus()
        // Verificar si ya hay un proceso en ejecución
        if (currentStatus == "INICIADO") {
            logger.info("Ya hay un proceso en ejecución.")
            return
        }

        updateStatus("INICIADO")

        try {
            // Lógica de procesamiento aquí


            val processedFiles = getProcessedFiles(processedFilesFile)
            val directory = File(directoryPath)


            if (!directory.exists() || !directory.isDirectory) {
                logger.error("El directorio no existe o no es válido: $directoryPath")
                return
            }

            val processedDirectory = File(processedPath)
            if (!processedDirectory.exists()) {
                processedDirectory.mkdirs()
            }

            runBlocking {
                directory.walkTopDown()
                    .filter { it.isFile }
                    .filterNot { it.name in processedFiles }
                    .sortedBy { it.length() }
                    .map { file ->
                        async(Dispatchers.IO) {
                            try {
                                if (file.extension.equals("zip", ignoreCase = true)) {
                                    processZipFile(file)
                                } else {
                                    processLargeFile(file)
                                }
                                moveToProcessed(file, processedDirectory)
                                markFileAsProcessed(processedFilesFile,file.name)
                            } catch (e: Exception) {
                                logger.error("Error al procesar el archivo: ${file.name}", e)
                            }
                        }
                    }.forEach { it.await() }
            }

            updateStatus("COMPLETADO")


        } catch (e: Exception) {
            updateStatus("ERROR: ${e.message}")
            logger.error("Error durante el procesamiento: ", e)
        }
    }

    private fun processZipFile(zipFile: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry?
            try {
                while (zis.nextEntry.also { entry = it } != null) {
                    if (!entry!!.isDirectory) {
                        logger.info("Procesando archivo dentro del ZIP: ${entry!!.name}")
                         zis.bufferedReader().use { reader ->
                            processStream(reader)
                        }
                    }
                    zis.closeEntry() // Asegúrate de cerrar cada entrada después de procesarla
                }
            } catch (e: IOException) {
                logger.error("Error al procesar el archivo ZIP: ${zipFile.name}", e)
            }
        }
    }

    private fun processLargeFile(file: File) {
        logger.info("Procesando archivo grande: ${file.name} (${file.length()} bytes)")
        file.bufferedReader().use { reader ->
            processStream(reader)
        }
    }

    private fun processStream(reader: BufferedReader) {

        reader.forEachLine { line ->
            val delimitador = detectarDelimitador(line)
            if (delimitador != null) {
                val values = line.split(delimitador)
                val columnCount = values.size

              val tableName= when (columnCount) {
                    in 4..7 -> "BT_DETALLE_CARGOS"
                    in 13..17 -> "BT_DETALLE_LLAMADAS"
                    46 -> "BT_DETALLE_FACTURA"
                    else -> null
                }

                try {
                    when (tableName) {
                        "BT_DETALLE_FACTURA" -> {
                           processFactura(values)
                        }

                        "BT_DETALLE_CARGOS" -> {
                            processCargos(values)
                        }

                        "BT_DETALLE_LLAMADAS" -> {
                            processLlamadas(values)
                        }
                    }

                } catch (e: Exception) {
                    logger.error("Error processing record in values: ${values}", e)
                }
            }
        }
    }


    // Lee o inicializa el estado del archivo
    fun readOrInitializeStatus(): String {
        return if (statusFile.exists()) {
            statusFile.readText().ifBlank { "IDLE" }
        } else {
            updateStatus("IDLE")
            "IDLE"
        }
    }

    private fun updateStatus(status: String) {
        try {
            statusFile.writeText(status)
            logger.info("Estado actualizado a: $status")
        } catch (e: Exception) {
            logger.error("Error al actualizar el archivo de estado", e)
        }
    }

    fun getStatus(): String {
        return try {
            if (statusFile.exists()) {
                val status = statusFile.readText().ifBlank { "IDLE" }
                """{"status": "$status"}"""
            } else {
                """{"status": "NO_STATUS_FILE"}"""
            }
        } catch (e: Exception) {
            logger.error("Error al obtener el estado", e)
            """{"status": "ERROR", "message": "${e.message}"}"""
        }
    }


    private var lastValidDate: Date? = null

    private fun processFactura(values: List<String>): Boolean {
        if (values.size < 34) {
            logger.warn("Invalid record format for BT_DETALLE_FACTURA: $values")
            return false
        }

        try {
            val entity = BTDetalleFactura(
                numFactura = values[0],
                referencia = values.getOrNull(1),
                operador = values.getOrNull(2),
                fechaEmision = values.getOrNull(3)?.let { parseDateWithFallback(it, lastValidDate) },
                fechaVencimiento = values.getOrNull(4)?.let { parseDateWithFallback(it, lastValidDate) },
                fechaCorte = values.getOrNull(5)?.let { parseDateWithFallback(it, lastValidDate) },
                moneda = values.getOrNull(6),
                tipoMoneda = values.getOrNull(7),
                iva = values.getOrNull(8)?.toDoubleOrNull(),
                subtotal = values.getOrNull(9)?.toDoubleOrNull(),
                impuestos = values.getOrNull(10)?.toDoubleOrNull(),
                total = values.getOrNull(11)?.toDoubleOrNull(),
                totalLetra = values.getOrNull(12),
                saldoAnterior = values.getOrNull(13)?.toDoubleOrNull(),
                descuento = values.getOrNull(14)?.toDoubleOrNull(),
                otrosCargos = values.getOrNull(15)?.toDoubleOrNull(),
                subtotal2 = values.getOrNull(16)?.toDoubleOrNull(),
                impuestos2 = values.getOrNull(17)?.toDoubleOrNull(),
                total2 = values.getOrNull(18)?.toDoubleOrNull(),
                totalFinal = values.getOrNull(19)?.toDoubleOrNull(),
                nombreCliente = values.getOrNull(20),
                descripcionCliente = values.getOrNull(21),
                sucursal = values.getOrNull(22),
                numeroCuenta = values.getOrNull(23),
                rfc = values.getOrNull(24),
                referenciaAdicional = values.getOrNull(25),
                nombreClienteAdicional = values.getOrNull(26),
                domicilio = values.getOrNull(27),
                ubicacion = values.getOrNull(28),
                localidad = values.getOrNull(29),
                estado = values.getOrNull(30),
                municipio = values.getOrNull(31),
                codigoPostal = values.getOrNull(32),
                pais = values.getOrNull(33),
                domicilioFiscal = values.getOrNull(34),
                ubicacionFiscal = values.getOrNull(35),
                localidadFiscal = values.getOrNull(36),
                estadoFiscal = values.getOrNull(37),
                municipioFiscal = values.getOrNull(38),
                codigoPostalFiscal = values.getOrNull(39),
                numFacturacion = values.getOrNull(40),
                paisFiscal = values.getOrNull(41),
                notas = values.getOrNull(42),
                fechaCreacion = Date(),
                activo = 0
            )

            val isDuplicate = facturaRepository.existsByAllFields(
                entity.numFactura,
                entity.referencia,
                entity.operador,
                entity.fechaEmision,
                entity.fechaVencimiento,
                entity.fechaCorte,
                entity.moneda,
                entity.tipoMoneda,
                entity.iva,
                entity.subtotal,
                entity.impuestos,
                entity.total,
                entity.totalLetra,
                entity.saldoAnterior,
                entity.descuento,
                entity.otrosCargos,
                entity.subtotal2,
                entity.impuestos2,
                entity.total2,
                entity.totalFinal,
                entity.nombreCliente,
                entity.descripcionCliente,
                entity.sucursal,
                entity.numeroCuenta,
                entity.rfc,
                entity.referenciaAdicional,
                entity.nombreClienteAdicional,
                entity.domicilio,
                entity.ubicacion,
                entity.localidad,
                entity.estado,
                entity.municipio,
                entity.codigoPostal,
                entity.pais,
                entity.domicilioFiscal,
                entity.ubicacionFiscal,
                entity.localidadFiscal,
                entity.estadoFiscal,
                entity.municipioFiscal,
                entity.codigoPostalFiscal,
                entity.numFacturacion,
                entity.paisFiscal,
                entity.notas
            )

            return if (isDuplicate) {
                false
            } else {
                facturaRepository.save(entity)
                true
            }
        } catch (e: Exception) {
            // Manejar el error si el formato de la fecha no es válido
            //handleInvalidDate(fileName, lineNumber, values.getOrNull(5),lastValidDate)
            return false
        }
    }

    private fun processCargos(values: List<String>): Boolean {
        if (values.size < 4) {
            logger.warn("Invalid record format for BT_DETALLE_CARGOS: $values")
            return false
        }

        val entity = BTDetalleCargos(
            numFactura = values[0],
            operador = values.getOrNull(1),
            tipoCargo = values.getOrNull(2),
            monto = values.getOrNull(3)?.toDoubleOrNull(),
            fechaRegistro = Date(),
            activo = values.getOrNull(5)?.toIntOrNull() ?: 0
        )

        val isDuplicate = cargosRepository.existsByNumFacturaAndOperadorAndTipoCargoAndMonto(
            entity.numFactura, entity.operador, entity.tipoCargo, entity.monto
        )

        return if (isDuplicate) {
            false
        } else {
            cargosRepository.save(entity)
            true
        }
    }


    private fun processLlamadas(values: List<String>): Boolean {
        try {
            if (values.size < 13) {
                logger.warn("Invalid record format for BT_DETALLE_LLAMADAS: $values")
                return false
            }

            val localidad = values.getOrNull(4)?.uppercase() ?: "DESCONOCIDA"

// Verificar primero en caché

            val poblacion = localidadesCache[localidad]
                ?: synchronized(this) { // Sincronizar para evitar conflictos concurrentes
                    // Verificar de nuevo dentro del bloque sincronizado
                    localidadesCache[localidad] ?: run {
                        // Si no existe en caché, verificar directamente en la base de datos
                        val poblacionExistente = catPoblacionRepository.findByDescripcion(localidad)
                        if (poblacionExistente != null) {
                            localidadesCache[localidad] = poblacionExistente // Actualizar caché
                            poblacionExistente
                        } else {
                            // Insertar nueva localidad
                            val nuevaLocalidad = catPoblacionRepository.save(
                                CatPoblacion(
                                    descripcion = localidad.ifBlank { "DESCONOCIDA" },
                                    nivel = 1,
                                    activo = 1,
                                    fechaCreacion = Date()
                                )
                            )
                            localidadesCache[localidad] = nuevaLocalidad // Actualizar caché
                            nuevaLocalidad
                        }
                    }
                }

            val modalidad = values.getOrNull(11)?.uppercase() ?: return false

// Verificar primero en caché
            val tipoLlamada = modalidadesCache[modalidad]
                ?: synchronized(this) { // Sincronizar para evitar conflictos concurrentes
                    // Verificar de nuevo dentro del bloque sincronizado
                    modalidadesCache[modalidad] ?: run {
                        // Si no existe en caché, verificar directamente en la base de datos
                        val tipoLlamadaExistente = catTipoLlamadaRepository.findByDescripcion(modalidad)
                        if (tipoLlamadaExistente != null) {
                            modalidadesCache[modalidad] = tipoLlamadaExistente // Actualizar caché
                            tipoLlamadaExistente
                        } else {
                            // Insertar nueva modalidad
                            val nuevaModalidad = catTipoLlamadaRepository.save(
                                CatTipoLlamada(
                                    descripcion = modalidad,
                                    nivel = 1,
                                    activo = 1,
                                    fechaCreacion = Date()
                                )
                            )
                            modalidadesCache[modalidad] = nuevaModalidad // Actualizar caché
                            nuevaModalidad
                        }
                    }
                }


            val parsedDate = parseDateWithFallback(values.getOrNull(5)?.trim(), lastValidDate)

            if (parsedDate != null) {
                val cost = parseDoubleOrDefault(values.getOrNull(8) ?: "0.0", 0.0, "fileName", "lineNumber")
                val entity = BTDetalleLlamadas(
                    numFactura = values[0],
                    operador = values.getOrNull(1),
                    numOrigen = values.getOrNull(2),
                    numDestino = values.getOrNull(3),
                    localidad = poblacion.id.toString(),
                    fechaLlamada = parsedDate,
                    horaLlamada = values.getOrNull(6),
                    duracion = values.getOrNull(7)?.toIntOrNull(),
                    costo = cost,
                    cargoAdicional = values.getOrNull(9)?.toDoubleOrNull(),
                    tipoCargo = values.getOrNull(10),
                    modalidad = tipoLlamada.id.toString(),
                    clasificacion = values.getOrNull(12),
                    fechaCreacion = Date(),
                    activo = 0,
                    idCentroCostos = values.getOrNull(15)?.toIntOrNull()
                )

                llamadasRepository.save(entity)
                return true
            } else {
                return false
            }
        } catch (e: ConstraintViolationException) {
            logger.warn("Duplicate record detected: ${e.message}")
            return true
        } catch (e: Exception) {
            logger.error("Error processing record", e)
            return false
        }
    }

}
