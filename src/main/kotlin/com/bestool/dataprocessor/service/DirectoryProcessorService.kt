package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.BuildConfig
import com.bestool.dataprocessor.DynamicSQLInspector
import com.bestool.dataprocessor.dto.FacturaDTO
import com.bestool.dataprocessor.dto.ProgresoProceso
import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.bestool.dataprocessor.entity.RegistroFactura
import com.bestool.dataprocessor.repository.BTDetalleLlamadasRepository
import com.bestool.dataprocessor.repository.RegistroFacturaRepository
import com.bestool.dataprocessor.utils.TransactionContext
import com.bestool.dataprocessor.utils.UtilCaster.Companion.crearDetalleLlamadasEntity
import com.bestool.dataprocessor.utils.Utils.Companion.moveFilesToRoot
import com.bestool.dataprocessor.utils.Utils.Companion.moveToProcessed
import com.bestool.dataprocessor.utils.Utils.Companion.saveProgress
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskRejectedException
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionTimedOutException
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import javax.persistence.RollbackException
import javax.validation.ConstraintViolationException

@Service
class DirectoryProcessorService(
    private val transactionalService: TransactionalService,
    private val catalogosService: CatalogosService,
    private val llamadasRepository: BTDetalleLlamadasRepository,
    private val registroFacturaRepository: RegistroFacturaRepository,
    private val cargosService: CargosService,
    private val facturasService: FacturasService,

    ) {
    private var mainPath = BuildConfig.MAIN_PATH
    var directory = File(mainPath)
    private var processedDirectory = File(mainPath, "/processed")
    private var failedDirectory = File(mainPath, "/failed")
    private var logger = LoggerFactory.getLogger(DirectoryProcessorService::class.java)

    private val isEnabled: Boolean = BuildConfig.SCHEDULE_ENABLE


    @PostConstruct
    fun initializeDirectories() {
        try {
            // Validar directorio principal
            val directory = File(mainPath)
            if (!directory.exists() || !directory.isDirectory) {
                directory.mkdirs()
                logger.info("El directorio principal no existe o no es válido, pero ha sido creado: $mainPath")
            }

            // Inicializar processedDirectory
            processedDirectory = File(directory, "processed").apply {
                if (!exists()) {
                    mkdirs()
                    logger.info("Directorio para procesar creado: $absolutePath")
                }
            }

            // Inicializar failedDirectory
            failedDirectory = File(directory, "failed").apply {
                if (!exists()) {
                    mkdirs()
                    logger.info("Directorio para fallas creado: $absolutePath")
                }
            }

        } catch (e: Exception) {
            logger.error("Error inicializando directorios: ${e.message}", e)
            throw IllegalStateException("No se pudieron inicializar los directorios", e)
        }
    }


    // Expresión cron dinámica (por defecto: una vez al día a las 11 PM)
    @Volatile
    private var cronExpression: String = "0 0 23 * * *"

    // Método para actualizar la programación con todos los parámetros
    fun updateCronSchedule(
        seconds: Int,
        minutes: Int,
        hours: Int,
        dayOfMonth: String,
        month: String,
        dayOfWeek: String
    ) {
        require(seconds in 0..59) { "Segundos debe estar entre 0 y 59." }
        require(minutes in 0..59) { "Minutos debe estar entre 0 y 59." }
        require(hours in 0..23) { "Horas debe estar entre 0 y 23." }
        require(dayOfMonth.matches(Regex("\\*|[1-9]|[12][0-9]|3[01]"))) { "Día del mes debe ser un número entre 1 y 31 o '*'." }
        require(month.matches(Regex("\\*|[1-9]|1[0-2]"))) { "Mes debe ser un número entre 1 y 12 o '*'." }
        require(dayOfWeek.matches(Regex("\\*|[0-6]"))) { "Día de la semana debe ser un número entre 0 y 6 o '*'." }

        // Construir la expresión cron
        cronExpression = "$seconds $minutes $hours $dayOfMonth $month $dayOfWeek"
        logger.info("Nueva programación configurada: $cronExpression")
    }

    // Obtener la expresión cron en formato legible
    fun getCronExpression(): String {
        val parts = cronExpression.split(" ")
        val seconds = parts[0]
        val minutes = parts[1]
        val hours = parts[2]
        val dayOfMonth = parts[3]
        val month = parts[4]
        val dayOfWeek = parts[5]

        return """
            La tarea está configurada para ejecutarse:
            - Segundos: $seconds
            - Minutos: $minutes
            - Horas: $hours
            - Días del mes: $dayOfMonth
            - Meses: $month
            - Días de la semana: $dayOfWeek
        """.trimIndent()
    }

    // Ejecutar la tarea programada
    @Async
    @Scheduled(cron = "#{@directoryProcessorService.getCronExpression()}")
    fun processDirectoryAsync() {
        if (!isEnabled) {
            logger.info("Scheduler is disabled")
            return
        }
        initializeDirectories()
        try {
            catalogosService.process(directory, processedDirectory, failedDirectory)
            facturasService.process(directory, processedDirectory, failedDirectory)
            cargosService.process(directory, processedDirectory, failedDirectory)
            catalogosService.loadCache()
            logger.info("4. PROCESANDO DETALLE DE LLAMADAS: ")
            val details = directory.walkTopDown()
                .filter { it.isFile && it.extension.equals("ll", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }
                .toList()
                .sortedWith(compareBy<File> { it.length() } // Ordenar por tamaño primero // Luego, los que tienen 'partX' al final
                    .thenBy { it.name })
            logger.info("4. VERIFICANDO INTEGRIDAD: ")
            verificarFacturasConArchivos(details)
            details.forEach { file ->
                try {
                    processLLBatchFile(file) // Cambiamos para usar batch
                } catch (ex: Exception) {
                    logger.error("Error al procesar el archivo LL: ${file.name}", ex)
                    moveToProcessed(file, failedDirectory)
                    throw ex
                }
            }
            logger.info("4. DATOS DE LLAMADA PROCESADOS")
        } catch (ex: Exception) {
            logger.error("Error durante el procesamiento: ", ex)
            throw ex
        }

    }


    fun verificarFacturasConArchivos(directorio: List<File>) {

        val facturas = llamadasRepository.findFacturasWithCountAndLastDate().map {
            FacturaDTO(
                numFactura = it["BDL_NUM_FACTURA"].toString(),
                ultimaFecha = it["ULTIMA_FECHA"].toString(),
                cantidadRegistros = it["CANTIDAD_REGISTROS"].toString().toLong()
            )
        }


        facturas.forEach { factura ->
            val archivo = directorio.firstOrNull { file ->
                file.name.contains(factura.numFactura, ignoreCase = true)
            }

            val progress = saveProgress(
                ProgresoProceso(
                    factura = factura.numFactura,
                    status = "DOWNLOAD",
                    totalLinesInBase = factura.cantidadRegistros,
                ), registroFacturaRepository
            )


            if (progress != null && archivo != null) {
                if (progress.totalLinesFile == factura.cantidadRegistros) {

                    moveToProcessed(archivo, processedDirectory)

                    logger.info("Factura ${factura.numFactura}: Archivos movidos a procesados.")
                    saveProgress(
                        ProgresoProceso(
                            archivo = archivo.name,
                            factura = factura.numFactura,
                            status = "COMPLETED"
                        ), registroFacturaRepository
                    )
                } else if (progress.numeroLinea == progress.totalLinesInBase) {
                    saveProgress(
                        ProgresoProceso(
                            factura = factura.numFactura,
                            status = "COMPLETED"
                        ), registroFacturaRepository
                    )
                } else {
                    moveToProcessed(archivo, failedDirectory)
                    saveProgress(
                        ProgresoProceso(
                            archivo = archivo.name,
                            factura = factura.numFactura,
                            status = "ERROR",
                            numeroLinea = factura.cantidadRegistros
                        ), registroFacturaRepository
                    )
                }
            }
        }


    }


    private fun processLLBatchFile(file: File) {
        logger.info("Procesando archivo LL en batch: ${file.name} (${file.length()} bytes)")
        val batchSize = 500 // Tamaño del lote
        val allSuccess = AtomicBoolean(true)
        var lastProcessedLine: Long = 0

        // Leer el progreso guardado
        val progresoPrevio: RegistroFactura? = try {
            registroFacturaRepository.findByArchivo(file.name)
        } catch (e: Exception) {
            logger.warn("Error consultando progreso en base de datos: ${e.message}", e)
            null
        }


        // Si hay progreso previo, tomar la línea de inicio
        if (progresoPrevio != null) {
            lastProcessedLine = progresoPrevio.numeroLinea
            logger.info("Reiniciando desde la línea ${lastProcessedLine + 1} para la factura ${progresoPrevio.archivo}")
            if (progresoPrevio.status.equals("COMPLETED", ignoreCase = true)) {
                return
            }
        }


        var numFactura = ""

        try {
            file.bufferedReader().use { reader ->
                val batch = mutableListOf<BTDetalleLlamadas>()
                reader.useLines { lines ->
                    lines.drop(lastProcessedLine.toInt()).forEachIndexed { _, line ->
                        try {
                            val values = line.replace("||", "|VACÍO|").split("|")

                            val entity = crearDetalleLlamadasEntity(
                                values,
                                catalogosService.localidadesCache,
                                catalogosService.catPoblacionRepository,
                                catalogosService.modalidadesCache,
                                catalogosService.catTipoLlamadaRepository
                            )

                            if (entity != null) {
                                batch.add(entity)
                                numFactura = entity.numFactura
                            }

                            // Guardar el lote cuando alcance el tamaño definido
                            if (batch.size >= batchSize) {
                                guardarLoteFiltrado(batch, file.name)
                                saveProgress(
                                    ProgresoProceso(
                                        factura = numFactura,
                                        archivo = file.name,
                                        status = "PROGRESS",
                                        numeroLinea = batch.size.toLong(),
                                    ), registroFacturaRepository
                                )
                                batch.clear()
                            }
                        } catch (ex: Exception) {
                            allSuccess.set(false)
                            logger.error("Error procesando línea: $line", ex)
                            throw ex // Opcional: detener el proceso completamente
                        }
                    }

                    // Guardar el lote restante
                    if (batch.isNotEmpty()) {
                        guardarLoteFiltrado(batch, file.name)
                        saveProgress(
                            ProgresoProceso(
                                factura = numFactura,
                                archivo = file.name,
                                status = "PROGRESS",
                                numeroLinea = batch.size.toLong(),
                            ), registroFacturaRepository
                        )
                    }
                }
            }

            if (allSuccess.get()) {
                moveToProcessed(file, processedDirectory)
                saveProgress(
                    ProgresoProceso(
                        factura = numFactura,
                        archivo = file.name,
                        status = "COMPLETED",
                    ), registroFacturaRepository
                )
                logger.info("Archivo LL procesado exitosamente: ${file.name}")
            } else {
                moveToProcessed(file, failedDirectory)
                saveProgress(
                    ProgresoProceso(
                        factura = numFactura,
                        archivo = file.name,
                        status = "ERROR"
                    ), registroFacturaRepository
                )
                logger.warn("Archivo procesado con errores: ${file.name}")
            }
        } catch (ex: Exception) {
            logger.error("Error procesando archivo LL: ${ex.message}", ex)
            moveToProcessed(file, failedDirectory)
            throw ex
        }
    }

    //Guardar el batch filtrado
    private fun guardarLoteFiltrado(
        batch: List<BTDetalleLlamadas>,
        fileName: String
    ) {
        if (batch.isNotEmpty()) {
            try {
                transactionalService.guardarLoteLlamadas(batch, fileName)
                logger.info("Se guardaron ${batch.size} registros del lote actual.")
            } catch (ex: ConstraintViolationException) {
                logger.error("Violación de restricciones al guardar el archivo: $fileName", ex)
                ex.constraintViolations.forEach {
                    logger.error("Campo: ${it.propertyPath} - Valor inválido: ${it.invalidValue} - Mensaje: ${it.message}")
                }
                throw ex  // <-- NECESARIO para que la transacción se marque como fallida
            } catch (ex: RollbackException) {
                logger.error("Se hizo rollback al procesar el archivo: $fileName", ex)
                var cause = ex.cause
                while (cause != null) {
                    logger.error("Causa anidada: ${cause::class.java.name} - ${cause.message}")
                    cause = cause.cause
                }
                throw ex
            } catch (ex: TransactionTimedOutException) {
                logger.error("La transacción expiró procesando: $fileName", ex)
                throw ex
            } catch (ex: TaskRejectedException) {
                logger.error("Tarea rechazada para el archivo: $fileName. Reintentando...", ex)
                throw ex
            } catch (ex: Exception) {
                val lastSQL = DynamicSQLInspector.getLastSQL()
                logger.error("Error general procesando el archivo: $fileName", ex)
                logger.error("Último SQL ejecutado antes del error:\n$lastSQL")
                TransactionContext.setException(ex)
                throw ex
            }
        } else {
            logger.info("No hay registros nuevos para guardar.")
        }
    }


    fun restoreFiles(): String {
        return try {
            if (processedDirectory.exists()) {
                moveFilesToRoot(processedDirectory)
                processedDirectory.deleteRecursively()
                logger.info("Directorio 'processed' restaurado y eliminado.")
            }

            if (failedDirectory.exists()) {
                moveFilesToRoot(failedDirectory)
                failedDirectory.deleteRecursively()
                logger.info("Directorio 'failed' restaurado y eliminado.")
            }

            "Archivos restaurados correctamente. Los directorios 'processed' y 'failed' han sido eliminados."
        } catch (e: Exception) {
            logger.error("Error al restaurar los archivos: ${e.message}", e)
            throw RuntimeException("Error al restaurar los archivos: ${e.message}")
        }
    }

    @Async
    fun catalogs() {
        catalogosService.process(directory, processedDirectory, failedDirectory)
    }

    @Async
    fun processCharges() {
        cargosService.process(directory, processedDirectory, failedDirectory)
    }

    @Async
    fun processBills() {
        facturasService.process(directory, processedDirectory, failedDirectory)
    }

    fun getDirectoryStatus(): Map<String, Int> {
        val totalDirs = directory.listFiles { file -> file.isDirectory }?.toList() ?: emptyList()
        val processed = processedDirectory.listFiles()?.size ?: 0
        val failed = failedDirectory.listFiles()?.size ?: 0

        // Los pendientes son los directorios que no están en processed ni failed
        val processedNames = processedDirectory.list()?.toSet() ?: emptySet()
        val failedNames = failedDirectory.list()?.toSet() ?: emptySet()
        val pending = totalDirs.count { it.name !in processedNames && it.name !in failedNames }

        return mapOf(
            "processed" to processed,
            "failed" to failed,
            "pending" to pending
        )
    }


}
