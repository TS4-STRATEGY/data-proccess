package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.dto.ProgresoProceso
import com.bestool.dataprocessor.entity.CatPoblacion
import com.bestool.dataprocessor.entity.CatTipoLlamada
import com.bestool.dataprocessor.repository.CatPoblacionRepository
import com.bestool.dataprocessor.repository.CatTipoLlamadaRepository
import com.bestool.dataprocessor.repository.RegistroFacturaRepository
import com.bestool.dataprocessor.utils.UtilCaster.Companion.obtenerOInsertarEnCache
import com.bestool.dataprocessor.utils.Utils.Companion.moveToProcessed
import com.bestool.dataprocessor.utils.Utils.Companion.removeDuplicates
import com.bestool.dataprocessor.utils.Utils.Companion.saveProgress
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class CatalogosService(
    val catPoblacionRepository: CatPoblacionRepository,
    val catTipoLlamadaRepository: CatTipoLlamadaRepository,
    val registroFacturaRepository: RegistroFacturaRepository
) {
    private val logger = LoggerFactory.getLogger(CatalogosService::class.java)
    var localidadesCache = ConcurrentHashMap<String, CatPoblacion>()
    var modalidadesCache = ConcurrentHashMap<String, CatTipoLlamada>()

    fun loadCache() {
        try {
            localidadesCache.putAll(
                catPoblacionRepository.findAll().associateBy({ it.descripcion.uppercase(Locale.getDefault()) }, { it })
            )
            modalidadesCache.putAll(
                catTipoLlamadaRepository.findAll()
                    .associateBy({ it.descripcion.uppercase(Locale.getDefault()) }, { it })
            )
            logger.info("Localidades y modalidades precargadas en caché.")
        } catch (ex: Exception) {
            logger.error("Error al precargar los catálogos en caché", ex)
            throw ex
        }
    }

    fun process(directory: File, processedDirectory: File, failedDirectory: File) {
        loadCache()
        logger.info("1. PROCESANDO CATALOGOS: ")
        val data = directory.walkTopDown().filter { it.isFile && it.extension.equals("ll", ignoreCase = true) }
            .toList()
            .sortedBy { it.length() }

        removeDuplicates(data).forEach { file ->
            try {
                processLLFile(file) // Cambiamos para usar batch
            } catch (e: Exception) {
                logger.error("Error al procesar el archivo LL: ${file.name}", e)
                moveToProcessed(file, failedDirectory)
            }
        }
        logger.info("1. CATALOGOS PROCESADOS")
    }

    private fun processLLFile(file: File) {
        logger.info("Procesando catalogos de : ${file.name} (${file.length()} bytes)")
        var numFactura = ""
        var lineCount = 0 // Ahora puedes contar las líneas sin consumir la secuencia

        file.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val values = line.replace("||", "|VACÍO|").split("|")
                processCatalog(values)
                numFactura = values[0].toString()
                lineCount++
            }
        }

        saveProgress(
            ProgresoProceso(
                factura = numFactura,
                archivo = file.name,
                status = "COUNT",
                totalLinesFile = lineCount.toLong()
            ), registroFacturaRepository
        )
    }

    fun processCatalog(values: List<String>) {
        try {
            if (values.size < 13) {
                logger.warn("Invalid record format for BT_DETALLE_LLAMADAS: $values")
            }

            val localidadDescripcion = values.getOrNull(4)?.uppercase() ?: "VACÍO"
            val modalidadDescripcion = values.getOrNull(11)?.uppercase() ?: "VACÍO"

            // Obtener o insertar localidad en caché
            obtenerOInsertarEnCache(
                cache = localidadesCache,
                descripcion = localidadDescripcion.ifBlank { "VACÍO" },
                buscarEnBD = { catPoblacionRepository.findByDescripcion(it) }, // Buscar en base de datos
                insertarEnBD = {
                    catPoblacionRepository.save(
                        CatPoblacion(
                            descripcion = localidadDescripcion.ifBlank { "VACÍO" },
                            nivel = 1,
                            activo = 1,
                            fechaCreacion = Date()
                        )
                    )
                })

            // Obtener o insertar modalidad en caché
            obtenerOInsertarEnCache(
                cache = modalidadesCache,
                descripcion = modalidadDescripcion.ifBlank { "VACÍO" },
                buscarEnBD = { catTipoLlamadaRepository.findByDescripcion(it) }, // Buscar en base de datos
                insertarEnBD = {
                    catTipoLlamadaRepository.save(
                        CatTipoLlamada(
                            descripcion = modalidadDescripcion.ifBlank { "VACÍO" },
                            nivel = 1,
                            activo = 1,
                            fechaCreacion = Date()
                        )
                    )
                })

        } catch (ex: Exception) {
            logger.error("Error processing record: $values", ex)
            throw ex
        }
    }

}