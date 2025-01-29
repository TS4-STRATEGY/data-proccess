package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.bestool.dataprocessor.repository.BTDetalleLlamadasRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class TransactionalService(
    private val llamadasRepository: BTDetalleLlamadasRepository,
) {

    private val logger = LoggerFactory.getLogger(TransactionalService::class.java)


    @Transactional
    fun guardarLoteLlamadas(
        batch: List<BTDetalleLlamadas>,
        fileName: String,
    ) {
        if (batch.isEmpty()) {
            logger.info("No hay registros nuevos para insertar.")
            return
        }
        try {
            llamadasRepository.saveAll(batch)
            logger.info("Lote del archivo $fileName insertado correctamente.")
        } catch (_: DataIntegrityViolationException) {
            logger.error("Conflicto de integridad en lote $fileName...")
        } catch (_: Exception) {
            logger.error("Error guardando lote del archivo $fileName")
        }
    }

}

