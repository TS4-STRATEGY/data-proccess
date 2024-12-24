package com.bestool.dataproccessor.service

import com.bestool.dataproccessor.entity.*
import com.bestool.dataproccessor.repository.*
import com.bestool.dataproccessor.utils.Utils.Companion.detectarDelimitador
import com.bestool.dataproccessor.utils.Utils.Companion.getProcessedFiles
import com.bestool.dataproccessor.utils.Utils.Companion.markFileAsProcessed
import com.bestool.dataproccessor.utils.Utils.Companion.moveToProcessed
import com.bestool.dataproccessor.utils.Utils.Companion.parseDate
import com.bestool.dataproccessor.utils.Utils.Companion.parseDateBill
import com.bestool.dataproccessor.utils.Utils.Companion.parseDoubleOrDefault
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Service
class DirectoryProcessorService(
    @Value("\${directory.input.path}") private val directoryPath: String,
    @Value("\${directory.processed.path}") private val processedPath: String,
    @Value("\${directory.failed.path}") private val failedPath: String,
    private val catPoblacionRepository: CatPoblacionRepository,
    private val catTipoLlamadaRepository: CatTipoLlamadaRepository,
    private val facturaRepository: BTDetalleFacturaRepository,
    private val cargosRepository: BTDetalleCargosRepository,
    private val llamadasRepository: BTDetalleLlamadasRepository,
    private val batchRepository: BatchRepository,

    ) {
    private lateinit var processedDirectory: File
    private lateinit var failedDirectory: File
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

        if (currentStatus == "INICIADO") {
            logger.info("Ya hay un proceso en ejecución.")
            return
        }

        updateStatus("INICIADO")

        try {

            val directory = File(directoryPath)

            if (!directory.exists() || !directory.isDirectory) {
                logger.error("El directorio no existe o no es válido: $directoryPath")
                return
            }

            processedDirectory = File(processedPath)
            if (!processedDirectory.exists()) processedDirectory.mkdirs()


            failedDirectory = File(failedPath)
            if (!failedDirectory.exists()) failedDirectory.mkdirs()

            getProcessedFiles(processedFilesFile)

            logger.info("DESCOMPRIMIENDO DATA: ")
            val zips = directory.walkTopDown()
                .filter { it.isFile && it.extension.equals("zip", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }
                .toList().sortedBy { it.length() }

            zips.forEach { file ->
                processZipFile(file)
            }

            logger.info("PROCESANDO CATALOGOS: ")
            val data = directory.walkTopDown()
                .filter { it.isFile && it.extension.equals("ll", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }
                .toList()
                .sortedBy { it.length() }

            removeDuplicates(data).forEach { file ->
                try {
                    processLLFile(file) // Cambiamos el método para usar batch
                } catch (e: Exception) {
                    logger.error("Error al procesar el archivo LL: ${file.name}", e)
                    moveToProcessed(file, failedDirectory)
                }
            }


            logger.info("PROCESANDO FACTURAS: ")
            val bills = directory.walkTopDown()
                .filter { it.isFile && it.extension.equals("crt", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }
                .toList().sortedBy { it.length() }

            bills.forEach { file ->
                processCRTFile(file)
            }

            logger.info("PROCESANDO CARGOS: ")
            val charges = directory.walkTopDown()
                .filter { it.isFile && it.extension.equals("crg", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }
                .toList().sortedBy { it.length() }

            charges.forEach { file ->
                processCRGFile(file)
            }

            logger.info("PROCESANDO DETALLE DE LLAMADAS: ")
            val details = directory.walkTopDown()
                .filter { it.isFile && it.extension.equals("ll", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }
                .toList()
                .sortedBy { it.length() }

            removeDuplicates(details).forEach { file ->
                try {
                    processLLBatchFile(file) // Cambiamos el método para usar batch
                } catch (e: Exception) {
                    logger.error("Error al procesar el archivo LL: ${file.name}", e)
                    moveToProcessed(file, failedDirectory)
                }
            }



            updateStatus("COMPLETADO")
        } catch (e: Exception) {
            updateStatus("ERROR: ${e.message}")
            logger.error("Error durante el procesamiento: ", e)
        }
    }


    private fun removeDuplicates(files: List<File>): List<File> {
        val uniqueFilesMap = mutableMapOf<String, File>()

        files.forEach { file ->
            val key = "${file.nameWithoutExtension}-${file.length()}" // Clave única: nombre base y tamaño
            if (!uniqueFilesMap.containsKey(key)) {
                uniqueFilesMap[key] = file // Agregar el primer archivo encontrado
            } else {
                // Si ya existe en el mapa, es un duplicado
                moveToProcessed(file, processedDirectory)
                markFileAsProcessed(processedFilesFile, file.name)
                logger.info("Duplicado movido a procesados: ${file.path}")
            }
        }

        return uniqueFilesMap.values.toList() // Retornar solo archivos únicos
    }

    private fun processZipFile(zipFile: File) {
        val zipParentDirectory = zipFile.parentFile // Obtiene el directorio donde se encuentra el ZIP

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry?

            try {
                while (zis.nextEntry.also { entry = it } != null) {
                    if (entry!!.isDirectory) {
                        // Crear subdirectorios si el ZIP contiene carpetas
                        val dir = File(zipParentDirectory, entry!!.name)
                        if (!dir.exists()) dir.mkdirs()
                        logger.info("Directorio creado: ${dir.absolutePath}")
                    } else {
                        // Crear archivo dentro del mismo directorio del ZIP
                        val outputFile = File(zipParentDirectory, entry!!.name)
                        outputFile.parentFile.mkdirs() // Asegúrate de que los directorios existan

                        logger.info("Extrayendo archivo dentro del ZIP: ${entry!!.name} a ${outputFile.absolutePath}")

                        try {
                            // Escribir el contenido del archivo ZIP en el archivo físico
                            FileOutputStream(outputFile).use { fos ->
                                zis.copyTo(fos)
                            }

                            logger.info("Archivo extraído exitosamente: ${outputFile.absolutePath}")
                        } catch (e: IOException) {
                            logger.error("Error al escribir archivo descomprimido: ${outputFile.name}", e)
                        }
                    }
                    zis.closeEntry() // Cierra la entrada después de procesarla
                }

                // Si todo el ZIP fue procesado correctamente, muévelo a procesados
                moveToProcessed(zipFile, processedDirectory)
                markFileAsProcessed(processedFilesFile, zipFile.name)

            } catch (e: IOException) {
                logger.error("Error al procesar el archivo ZIP: ${zipFile.name}", e)
                // Si el procesamiento del ZIP falla, moverlo a fallidos
                moveToProcessed(zipFile, failedDirectory)
            }
        }
    }


    private fun processCRTFile(file: File) {
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
                    processFactura(record.toString().split("|"))
                }

                // Mover el archivo basado en el estado global
                if (allSuccessful) {
                    moveToProcessed(file, processedDirectory)
                } else {
                    moveToProcessed(file, failedDirectory)
                }

                // Marcar el archivo como procesado
                markFileAsProcessed(processedFilesFile, file.name)
            }
        }

    }

    private fun processCRGFile(file: File) {
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
                    line.split("|"),
                    file.name,
                    index + 1
                )
            }.reduce { acc, result -> acc && result }
            // Mover el archivo basado en el estado global
            if (allSuccessful) {
                moveToProcessed(file, processedDirectory)
            } else {
                moveToProcessed(file, failedDirectory)
            }

            // Marcar el archivo como procesado
            markFileAsProcessed(processedFilesFile, file.name)
        }

    }

    private fun processLLFile(file: File) {
        logger.info("Procesando catalogos de : ${file.name} (${file.length()} bytes)")
        file.bufferedReader().useLines { lines ->
            var allSuccessful = true

            lines.forEach { line ->
                if (!processCatalog(line.split("|"))) {
                    allSuccessful = false
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


    private fun processFactura(values: List<String>): Boolean {
        if (values.size < 43) {
            logger.warn("Registro inválido: se esperaban al menos 43 campos, se recibieron ${values.size}. Valores: $values")
            return false
        }

        return try {
            val numFactura = values[0].takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("El campo numFactura es obligatorio y está vacío")

            val entity = BTDetalleFactura(
                numFactura = numFactura,
                referencia = values.getOrNull(24),
                operador = values.getOrNull(2),
                fechaEmision = values.getOrNull(3)?.let { parseDateBill(it) },
                fechaVencimiento = values.getOrNull(4)?.let { parseDateBill(it) },
                fechaCorte = values.getOrNull(5)?.let { parseDateBill(it) },
                moneda = values.getOrNull(6),
                tipoMoneda = values.getOrNull(7),
                iva = values.getOrNull(8)?.toDoubleOrNull() ?: 0.0,
                subtotal = values.getOrNull(9)?.toDoubleOrNull() ?: 0.0,
                impuestos = values.getOrNull(10)?.toDoubleOrNull() ?: 0.0,
                total = values.getOrNull(11)?.toDoubleOrNull() ?: 0.0,
                totalLetra = values.getOrNull(12),
                saldoAnterior = values.getOrNull(13)?.toDoubleOrNull(),
                descuento = values.getOrNull(14)?.toDoubleOrNull() ?: 0.0,
                otrosCargos = values.getOrNull(15)?.toDoubleOrNull() ?: 0.0,
                subtotal2 = values.getOrNull(9)?.toDoubleOrNull() ?: 0.0,
                impuestos2 = values.getOrNull(10)?.toDoubleOrNull(),
                total2 = values.getOrNull(11)?.toDoubleOrNull(),
                totalFinal = values.getOrNull(20)?.toDoubleOrNull(),
                nombreCliente = values.getOrNull(21),
                descripcionCliente = values.getOrNull(22),
                sucursal = values.getOrNull(23),
                numeroCuenta = values.getOrNull(24),
                rfc = values.getOrNull(25),
                referenciaAdicional = "",
                nombreClienteAdicional = values.getOrNull(26),
                domicilio = values.getOrNull(27),
                ubicacion = values.getOrNull(30),
                localidad = values.getOrNull(31),
                estado = values.getOrNull(32),
                municipio = values.getOrNull(33),
                codigoPostal = values.getOrNull(34),
                pais = values.getOrNull(35),
                domicilioFiscal = values.getOrNull(36),
                ubicacionFiscal = values.getOrNull(37),
                localidadFiscal = values.getOrNull(38),
                estadoFiscal = values.getOrNull(39),
                municipioFiscal = values.getOrNull(40),
                codigoPostalFiscal = values.getOrNull(41),
                numFacturacion = values.getOrNull(41) ?: "N/A",
                paisFiscal = values.getOrNull(35),
                notas = "",
                fechaCreacion = Date(),
                activo = 0
            )


            if (logger.isDebugEnabled)
                logger.info("BT_DETALLE_FACTURA: $entity")
            if (!facturaRepository.existsByFactura(numFactura, entity.referencia)) {

                facturaRepository.save(entity)
            } else {
                if (logger.isDebugEnabled)
                    logger.info("Factura duplicada: ${entity.numFactura}")
            }

            return true
        } catch (e: IllegalArgumentException) {
            logger.error("Error de validación: ${e.message}")
            false
        } catch (e: Exception) {
            logger.error("Error procesando factura: ${e.message}", e)
            false
        }
    }

    fun processCargos(data: List<String>, fileName: String, lineNumber: Int): Boolean {
        return try {
            val numFactura = data[0]
            val operador = data[1]
            val tipoCargo = data[2]
            val monto = data[3].toDouble()

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


    private fun <T> obtenerOInsertarEnCache(
        cache: MutableMap<String, T>,
        descripcion: String,
        buscarEnBD: (String) -> T?, // Función lambda para buscar en la base de datos
        insertarEnBD: () -> T       // Función lambda para insertar en la base de datos
    ): T {
        return cache[descripcion] ?: synchronized(this) {
            cache[descripcion] ?: run {
                val existente = buscarEnBD(descripcion)
                if (existente != null) {
                    cache[descripcion] = existente
                    existente
                } else {
                    try {
                        val nuevaEntidad = insertarEnBD()
                        cache[descripcion] = nuevaEntidad
                        nuevaEntidad
                    } catch (ex: DataIntegrityViolationException) {
                        if (ex.cause is SQLIntegrityConstraintViolationException) {
                            logger.warn("Registro '$descripcion' ya existe. Recuperando de la base de datos.")
                            val recuperado = buscarEnBD(descripcion)
                            recuperado ?: throw ex // Si no se recupera, lanza la excepción
                        } else throw ex
                    }
                }
            }
        }
    }


    private fun processCatalog(values: List<String>): Boolean {
        try {
            if (values.size < 13) {
                logger.warn("Invalid record format for BT_DETALLE_LLAMADAS: $values")
                return false
            }

            val localidadDescripcion = values.getOrNull(4)?.uppercase() ?: "DESCONOCIDA"
            val modalidadDescripcion = values.getOrNull(11)?.uppercase() ?: return false

            // Obtener o insertar localidad en caché
            obtenerOInsertarEnCache(
                cache = localidadesCache,
                descripcion = localidadDescripcion.ifBlank { "DESCONOCIDA" },
                buscarEnBD = { catPoblacionRepository.findByDescripcion(it) }, // Buscar en base de datos
                insertarEnBD = {
                    catPoblacionRepository.save(
                        CatPoblacion(
                            descripcion = localidadDescripcion.ifBlank { "DESCONOCIDA" },
                            nivel = 1,
                            activo = 1,
                            fechaCreacion = Date()
                        )
                    )
                }
            )

            // Obtener o insertar modalidad en caché
            obtenerOInsertarEnCache(
                cache = modalidadesCache,
                descripcion = modalidadDescripcion.ifBlank { "DESCONOCIDA" },
                buscarEnBD = { catTipoLlamadaRepository.findByDescripcion(it) }, // Buscar en base de datos
                insertarEnBD = {
                    catTipoLlamadaRepository.save(
                        CatTipoLlamada(
                            descripcion = modalidadDescripcion.ifBlank { "DESCONOCIDA" },
                            nivel = 1,
                            activo = 1,
                            fechaCreacion = Date()
                        )
                    )
                }
            )
            return true
        } catch (ex: Exception) {
            logger.error("Error processing record: $values", ex)
            return false
        }
    }

    private fun processLLBatchFile(file: File) {
        logger.info("Procesando archivo LL en batch: ${file.name} (${file.length()} bytes)")

        val batchSize = 1000 // Tamaño del lote
        val batch = mutableListOf<BTDetalleLlamadas>()

        var allSuccess = true
        file.bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                try {
                    val values = line.split("|")
                    val entity = crearDetalleLlamadasEntity(values) // Método para crear la entidad
                    if (entity != null) {
                        batch.add(entity)
                    }

                    // Procesar el lote cuando alcance el tamaño definido
                    if (batch.size >= batchSize) {
                        guardarLoteLlamadas(batch, file.name)
                        batch.clear() // Limpiar el lote después de guardar
                    }
                } catch (e: Exception) {
                    allSuccess = false
                    logger.error("Error procesando registro en línea ${index + 1}: $line", e)
                }
            }

            // Procesar cualquier registro restante en el último lote
            if (batch.isNotEmpty()) {
                guardarLoteLlamadas(batch, file.name)
            }
        }

        // Si el procesamiento fue exitoso, mover el archivo a procesados
        if (allSuccess) {
            moveToProcessed(file, processedDirectory)
            markFileAsProcessed(processedFilesFile, file.name)
            logger.info("Archivo LL procesado exitosamente: ${file.name}")
        } else {
            moveToProcessed(file, failedDirectory)
            markFileAsProcessed(processedFilesFile, file.name)
            logger.info("Archivo fallido: ${file.name}")
        }

    }

    private fun guardarLoteLlamadas(batch: List<BTDetalleLlamadas>, fileName: String) {
        try {
            llamadasRepository.saveAll(batch) // Guardar todas las entidades en un solo batch
            logger.info("Lote de ${batch.size} registros guardado exitosamente.")
        } catch (e: Exception) {
            logger.error("Error guardando lote del archivo $fileName: ${e.message}", e)
            // Procesar cada registro individualmente si ocurre un error en el lote
            batch.forEach { record ->
                try {
                    val existe = llamadasRepository.existsById(record.id)
                    if (!existe) {
                        logger.error("La entidad no existe en la base de datos")
                        return
                    }
                    llamadasRepository.save(record)
                } catch (ex: ObjectOptimisticLockingFailureException) {
                    logger.warn("Conflicto detectado. Reintentando...")
                    val entidadActualizada =
                        llamadasRepository.findById(record.id).orElseThrow { EntityNotFoundException() }
                    llamadasRepository.save(entidadActualizada)
                } catch (ex: Exception) {
                    logger.error("Error guardando registro individual: ${record.numFactura}", ex)
                }
            }
        }
    }


    private fun crearDetalleLlamadasEntity(values: List<String>): BTDetalleLlamadas? {
        return try {
            val localidadDescripcion = values.getOrNull(4)?.uppercase() ?: "DESCONOCIDA"
            val modalidadDescripcion = values.getOrNull(11)?.uppercase() ?: "DESCONOCIDA"

            val poblacion = localidadesCache[localidadDescripcion] ?: return null
            val tipoLlamada = modalidadesCache[modalidadDescripcion] ?: return null

            val parsedDate = parseDate(values.getOrNull(5)?.trim())
            val cost = parseDoubleOrDefault(values.getOrNull(8) ?: "0.0", 0.0, "fileName", "lineNumber")

            BTDetalleLlamadas(
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
        } catch (e: Exception) {
            logger.error("Error creando entidad para los valores: $values", e)
            null
        }
    }


}
