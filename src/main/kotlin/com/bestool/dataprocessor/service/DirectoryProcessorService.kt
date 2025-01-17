package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.dto.FacturaDTO
import com.bestool.dataprocessor.dto.ProgresoProceso
import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.bestool.dataprocessor.repository.BTDetalleLlamadasRepository
import com.bestool.dataprocessor.utils.UtilCaster.Companion.crearDetalleLlamadasEntity
import com.bestool.dataprocessor.utils.Utils
import com.bestool.dataprocessor.utils.Utils.Companion.moveFilesToRoot
import com.bestool.dataprocessor.utils.Utils.Companion.moveToProcessed
import com.bestool.dataprocessor.utils.Utils.Companion.saveProgress
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskRejectedException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

@Service
class DirectoryProcessorService(
    private val llamadasRepository: BTDetalleLlamadasRepository,
    private val transactionalService: TransactionalService,
    private val catalogosService: CatalogosService,
    private val cargosService: CargosService,
    private val facturasService: FacturasService,
    @Value("\${bestools.main-path}") private var mainPath: String
) {
    var directory = File(mainPath)
    private var processedDirectory = File(mainPath,"/processed")
    private var failedDirectory = File(mainPath,"/failed")
    private var logger = LoggerFactory.getLogger(DirectoryProcessorService::class.java)


    @PostConstruct
    fun initializeDirectories() {
        try {
            // Validar directorio principal
            val directory = File(mainPath)
            if (!directory.exists() || !directory.isDirectory) {
                logger.error("El directorio principal no existe o no es válido: $mainPath")
                throw IllegalStateException("El directorio principal no es válido: $mainPath")
            }

            // Inicializar processedDirectory
            processedDirectory = File(directory, "processed").apply {
                if (!exists()) {
                    mkdirs()
                    logger.info("Directorio para procesar creado: ${absolutePath}")
                }
            }

            // Inicializar failedDirectory
            failedDirectory = File(directory, "failed").apply {
                if (!exists()) {
                    mkdirs()
                    logger.info("Directorio para fallas creado: ${absolutePath}")
                }
            }

        } catch (e: Exception) {
            logger.error("Error inicializando directorios: ${e.message}", e)
            throw IllegalStateException("No se pudieron inicializar los directorios", e)
        }
    }


    @Async
    fun processDirectoryAsync() {
        try {
            catalogosService.loadCache()
            logger.info("PROCESANDO DETALLE DE LLAMADAS: ")
            val details = directory.walkTopDown()
                .filter { it.isFile && it.extension.equals("ll", ignoreCase = true) }
                .filterNot { it.parentFile.name.equals(processedDirectory.name, ignoreCase = true) }
                .toList()
                .sortedWith(compareBy<File> { it.length() } // Ordenar por tamaño primero
                    .thenBy {
                        if (it.name.contains(
                                "part",
                                ignoreCase = true
                            )
                        ) 1 else 0
                    } // Luego, los que tienen 'partX' al final
                    .thenBy { it.name })

            logger.info("VERIFICANDO INTEGRIDAD: ")
            verificarFacturasConArchivos(details)

            details.forEach { file ->
                try {
                    processLLBatchFile(file) // Cambiamos para usar batch
                } catch (e: Exception) {
                    logger.error("Error al procesar el archivo LL: ${file.name}", e)
                    moveToProcessed(file, failedDirectory)
                }
            }
            logger.info("DATOS DE LLAMADA PROCESADOS")
        } catch (e: Exception) {
            logger.error("Error durante el procesamiento: ", e)
        }

    }

    fun verificarFacturasConArchivos(directorio: List<File>) {
        val facturas = llamadasRepository.findFacturasWithCountAndLastDate().map {
            FacturaDTO(
                numFactura = it["BDL_NUM_FACTURA"].toString(),
                ultimaFecha = it["ULTIMA_FECHA"].toString(),
                cantidadRegistros = it["CANTIDAD_REGISTROS"].toString().toLong()
            )
        }.filter { factura ->
            directorio.any { file -> file.name.contains(factura.numFactura, ignoreCase = true) }
        }

        facturas.forEach { factura ->
            val archivosFactura = directorio.filter { file ->
                file.name.contains(factura.numFactura, ignoreCase = true)
            }

            val progress = saveProgress(
                ProgresoProceso(
                    factura = factura.numFactura,
                    status = "DOWNLOAD",
                    numeroLinea = factura.cantidadRegistros
                )
            )

            if (progress != null) {
                if (progress.totalLinesFile == factura.cantidadRegistros) {
                    archivosFactura.forEach { archivo ->
                        moveToProcessed(archivo, processedDirectory)
                    }
                    logger.info("Factura ${factura.numFactura}: Archivos movidos a procesados.")
                    saveProgress(
                        ProgresoProceso(
                            factura = factura.numFactura,
                            status = "COMPLETED"
                        )
                    )
                } else {
                    saveProgress(
                        ProgresoProceso(
                            factura = factura.numFactura,
                            status = "ERROR",
                            numeroLinea = factura.cantidadRegistros + 1
                        )
                    )
                }
            }
        }
    }


    private fun processLLBatchFile(file: File) {
        logger.info("Procesando archivo LL en batch: ${file.name} (${file.length()} bytes)")
        val batchSize = 500 // Tamaño del lote
        val allSuccess = AtomicBoolean(true)

        val mapper = jacksonObjectMapper()
        var lastProcessedLine: Long = 0

        // Leer el progreso guardado
        val progresoFile = Utils.progresoFile
        val progresoPrevio: ProgresoProceso? = try {
            if (progresoFile.exists()) {
                mapper.readValue<List<ProgresoProceso>>(
                    progresoFile,
                    object : TypeReference<List<ProgresoProceso>>() {})
                    .find { it.archivo == file.name } // Buscar progreso para este archivo
            } else {
                null // El archivo no existe
            }
        } catch (e: Exception) {
            logger.warn("Error leyendo archivo de progreso: ${e.message}", e)
            null // En caso de error, retornar null
        }

        // Si hay progreso previo, tomar la línea de inicio
        if (progresoPrevio != null) {
            lastProcessedLine = progresoPrevio.numeroLinea ?: 0
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

                    lines.drop(lastProcessedLine.toInt()).forEachIndexed { index, line ->
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
                                batch.clear()
                                saveProgress(
                                    ProgresoProceso(
                                        factura = progresoPrevio?.factura ?: "",
                                        archivo = file.name,
                                        status = "PROGRESS",
                                        numeroLinea = lastProcessedLine + index + 1,
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            allSuccess.set(false)
                            saveProgress(
                                ProgresoProceso(
                                    factura = numFactura,
                                    archivo = file.name,
                                    status = "ERROR",
                                    numeroLinea = index.toLong()
                                )
                            )
                            logger.error("Error procesando línea: $line", e)
                        }
                    }

                    // Guardar el lote restante
                    if (batch.isNotEmpty()) {
                        guardarLoteFiltrado(batch, file.name)
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
                    )
                )
                logger.info("Archivo LL procesado exitosamente: ${file.name}")
            } else {
                moveToProcessed(file, failedDirectory)
                logger.warn("Archivo procesado con errores: ${file.name}")
            }
        } catch (e: Exception) {
            logger.error("Error procesando archivo LL: ${e.message}", e)
            moveToProcessed(file, failedDirectory)
        }
    }

    //Guardar el batch filtrado
    private fun guardarLoteFiltrado(batch: List<BTDetalleLlamadas>, fileName: String) {
        if (batch.isNotEmpty()) {
            try {
                transactionalService.guardarLoteLlamadas(batch, fileName)
                logger.info("Se guardaron ${batch.size} registros del lote actual.")
            } catch (_: TaskRejectedException) {
                logger.error("Tarea rechazada para el archivo: $fileName. Reintentando...")
                // Reintentar después de un retraso (o usar una cola)
                Thread.sleep(3000)
                guardarLoteFiltrado(batch, fileName)
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


}
