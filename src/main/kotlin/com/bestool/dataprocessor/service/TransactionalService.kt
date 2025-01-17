package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.dto.ProgresoProceso
import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.bestool.dataprocessor.repository.BTDetalleLlamadasRepository
import com.bestool.dataprocessor.utils.Utils.Companion.saveProgress
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.transaction.Transactional
import kotlin.collections.forEach

@Service
class TransactionalService(
    private val llamadasRepository: BTDetalleLlamadasRepository,
) {

    private val logger = LoggerFactory.getLogger(TransactionalService::class.java)

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional
    fun guardarLoteLlamadas(
        batch: List<BTDetalleLlamadas>,
        fileName: String,
        numFactura: String,
    ) {
        if (batch.isEmpty()) {
            logger.info("No hay registros nuevos para insertar.")
            return
        }
        try {
            llamadasRepository.saveAll(batch)
            logger.info("Lote del archivo $fileName insertado correctamente.")
        } catch (_: DataIntegrityViolationException) {
            logger.warn("Conflicto de integridad en lote $fileName Procesando individualmente...")
            batch.forEach { guardarRegistroIndividual(it,fileName,numFactura) }
        } catch (_: Exception) {
            logger.error("Error guardando lote del archivo $fileName")
        }
    }

    private fun guardarRegistroIndividual(
        record: BTDetalleLlamadas,
        fileName: String,
        numFactura: String,
    ) {
        try {
            val existente = llamadasRepository.findByAllFields(
                record.numFactura, record.operador, record.numOrigen, record.numDestino,
                record.localidad, record.horaLlamada, record.duracion, record.costo,
                record.cargoAdicional, record.tipoCargo, record.modalidad, record.clasificacion
            )
            if (existente == null) {
                logger.info("Guardando nuevo registro con numFactura: $record")
                entityManager.merge(record)
            } else {
                logger.warn("El registro ya existe.")
            }

        } catch (_: Exception) {
            logger.error("Error procesando registro individual: ${record}")
        }
    }
}

