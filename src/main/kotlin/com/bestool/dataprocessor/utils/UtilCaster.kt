package com.bestool.dataprocessor.utils

import com.bestool.dataprocessor.entity.BTDetalleFactura
import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.bestool.dataprocessor.entity.CatPoblacion
import com.bestool.dataprocessor.entity.CatTipoLlamada
import com.bestool.dataprocessor.repository.BTDetalleFacturaRepository
import com.bestool.dataprocessor.repository.CatPoblacionRepository
import com.bestool.dataprocessor.repository.CatTipoLlamadaRepository
import com.bestool.dataprocessor.utils.Utils.Companion.moveToProcessed
import com.bestool.dataprocessor.utils.Utils.Companion.parseDate
import com.bestool.dataprocessor.utils.Utils.Companion.parseDateBill
import com.bestool.dataprocessor.utils.Utils.Companion.parseDoubleOrDefault
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class UtilCaster {

    companion object {
        private var logger = LoggerFactory.getLogger(UtilCaster::class.java)

        fun crearDetalleLlamadasEntity(
            values: List<String>,
            localidadesCache: ConcurrentHashMap<String, CatPoblacion>,
            catPoblacionRepository: CatPoblacionRepository,
            modalidadesCache: ConcurrentHashMap<String, CatTipoLlamada>,
            catTipoLlamadaRepository: CatTipoLlamadaRepository
        ): BTDetalleLlamadas? {
            return try {

                val localidadDescripcion = values.getOrNull(4)?.uppercase() ?: "VACÍO"
                val modalidadDescripcion = values.getOrNull(11)?.uppercase() ?: "VACÍO"

                val poblacion = obtenerOInsertarEnCache(
                    cache = localidadesCache,
                    descripcion = localidadDescripcion.ifBlank { "VACÍO" },
                    buscarEnBD = { catPoblacionRepository.findByDescripcion(it) },
                    insertarEnBD = {
                        catPoblacionRepository.save(
                            CatPoblacion(
                                descripcion = localidadDescripcion.ifBlank { "VACÍO" },
                                nivel = 1,
                                activo = 1,
                                fechaCreacion = Date()
                            )
                        )
                    }
                )

                val tipoLlamada = obtenerOInsertarEnCache(
                    cache = modalidadesCache,
                    descripcion = modalidadDescripcion.ifBlank { "VACÍO" },
                    buscarEnBD = { catTipoLlamadaRepository.findByDescripcion(it) },
                    insertarEnBD = {
                        catTipoLlamadaRepository.save(
                            CatTipoLlamada(
                                descripcion = modalidadDescripcion.ifBlank { "VACÍO" },
                                nivel = 1,
                                activo = 1,
                                fechaCreacion = Date()
                            )
                        )
                    }
                )


                val parsedDate = parseDate(values.getOrNull(5)?.trim())
                val cost = parseDoubleOrDefault(values.getOrNull(8) ?: "0.0", 0.0, "fileName", "lineNumber")


                BTDetalleLlamadas(
                    numFactura = values[0],
                    operador = values.getOrNull(1),
                    numOrigen = values.getOrNull(2),
                    numDestino = values.getOrNull(3),
                    localidad = poblacion.id,
                    fechaLlamada = parsedDate,
                    horaLlamada = values.getOrNull(6),
                    duracion = values.getOrNull(7)?.toDoubleOrNull(),
                    costo = cost,
                    cargoAdicional = values.getOrNull(9)?.toDoubleOrNull(),
                    tipoCargo = values.getOrNull(10),
                    modalidad = tipoLlamada.id,
                    clasificacion = values.getOrNull(12),
                    fechaCreacion = Date(),
                    activo = 1,
                    idCentroCostos = values.getOrNull(15)?.toIntOrNull()
                )

            } catch (e: Exception) {
                logger.error("Error creando entidad para los valores: $values", e)
                null
            }
        }


        fun <T> obtenerOInsertarEnCache(
            cache: MutableMap<String, T>,
            descripcion: String,
            buscarEnBD: (String) -> T?, // Función lambda para buscar en la base de datos
            insertarEnBD: () -> T       // Función lambda para insertar en la base de datos
        ): T {
            return cache[descripcion] ?: synchronized(cache) { // Sincronización específica por caché
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
                            // Manejo de violaciones de integridad
                            if (ex.cause is SQLIntegrityConstraintViolationException) {
                                logger.warn("Registro '$descripcion' ya existe. Intentando recuperar de la base de datos.")
                                val recuperado = buscarEnBD(descripcion)
                                recuperado ?: throw ex // Si no se recupera, lanza la excepción original
                            } else {
                                logger.error("Error al insertar '$descripcion': ${ex.message}", ex)
                                throw ex
                            }
                        } catch (ex: Exception) {
                            logger.error("Error inesperado al manejar '$descripcion': ${ex.message}", ex)
                            throw ex
                        }
                    }
                }
            }
        }

        fun processFactura(values: List<String>, facturaRepository: BTDetalleFacturaRepository): Boolean {
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
                    activo = 1
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


        fun processZipFile(zipFile: File, processedDirectory: File, failedDirectory: File) {
            val zipParentDirectory = zipFile.parentFile // Obtiene el directorio donde se encuentra el ZIP

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry?

                try {
                    while (zis.nextEntry.also { entry = it } != null) {
                        val currentEntry = entry!! // Crear una copia inmutable de la entrada

                        if (currentEntry.isDirectory) {
                            // Crear subdirectorios si el ZIP contiene carpetas
                            val dir = File(zipParentDirectory, currentEntry.name)
                            if (!dir.exists()) dir.mkdirs()
                            logger.info("Directorio creado: ${dir.absolutePath}")
                        } else {
                            // Crear archivo dentro del mismo directorio del ZIP
                            val outputFile = File(zipParentDirectory, currentEntry.name)
                            outputFile.parentFile.mkdirs() // Asegúrate de que los directorios existan

                            logger.info("Extrayendo archivo dentro del ZIP: ${currentEntry.name} a ${outputFile.absolutePath}")

                            try {
                                // Escribir el contenido del archivo ZIP en el archivo físico
                                FileOutputStream(outputFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                if (outputFile.extension.equals("zip", ignoreCase = true)) {
                                    logger.info("Archivo ZIP detectado dentro del ZIP: ${outputFile.name}")
                                    processZipFile(outputFile, processedDirectory, failedDirectory)
                                }

                                logger.info("Archivo extraído exitosamente: ${outputFile.absolutePath}")
                            } catch (e: IOException) {
                                logger.error("Error al escribir archivo descomprimido: ${outputFile.name}", e)
                            }
                        }
                        zis.closeEntry() // Cierra la entrada después de procesarla
                    }

                    moveToProcessed(zipFile, processedDirectory)
                } catch (e: IOException) {
                    logger.error("Error al procesar el archivo ZIP: ${zipFile.name}", e)
                    // Si el procesamiento del ZIP falla, moverlo a fallidos
                    moveToProcessed(zipFile, failedDirectory)
                }
            }
        }




    }
}