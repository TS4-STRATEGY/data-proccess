package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.bestool.dataprocessor.repository.BTDetalleLlamadasRepository
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
    fun guardarLoteLlamadas(batch: List<BTDetalleLlamadas>, fileName: String) {
        if (batch.isEmpty()) {
            logger.info("No hay registros nuevos para insertar.")
            return
        }
        try {
            llamadasRepository.saveAll(batch)
            logger.info("Lote del archivo $fileName insertado correctamente.")
        } catch (_: DataIntegrityViolationException) {
            logger.warn("Conflicto de integridad en lote $fileName Procesando individualmente...")
            batch.forEach { guardarRegistroIndividual(it) }
        } catch (_: Exception) {
            logger.error("Error guardando lote del archivo $fileName")

        }
    }

    private fun guardarRegistroIndividual(record: BTDetalleLlamadas) {
        try {
            entityManager.persist(record)
        } catch (_: Exception) {
            logger.error("Error procesando registro individual: ${record}")
        }
    }
}

