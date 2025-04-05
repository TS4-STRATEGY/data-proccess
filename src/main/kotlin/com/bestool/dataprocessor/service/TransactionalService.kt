package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import com.bestool.dataprocessor.repository.BTDetalleLlamadasRepository
import com.bestool.dataprocessor.utils.TransactionContext
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
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

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                    override fun afterCompletion(status: Int) {
                        val statusText = when (status) {
                            TransactionSynchronization.STATUS_COMMITTED -> "COMMIT"
                            TransactionSynchronization.STATUS_ROLLED_BACK -> "ROLLBACK"
                            else -> "DESCONOCIDO"
                        }

                        logger.warn("[$fileName] -> La transacción finalizó con estado: $statusText")

                        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                            val ex = TransactionContext.getException()
                            if (ex != null) {
                                logger.error("[$fileName] -> Rollback causado por excepción:", ex)
                            } else {
                                logger.warn("[$fileName] -> Rollback sin excepción capturada.")
                            }
                        }

                        TransactionContext.clear()
                    }
                })
            } else {
                logger.warn("[$fileName] -> No hay sincronización activa para registrar el listener de transacción.")
            }

        } catch (ex: DataIntegrityViolationException) {
            logger.error("Conflicto de integridad en lote $fileName...", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Error guardando lote del archivo $fileName", ex)
            throw ex
        }
    }


}

