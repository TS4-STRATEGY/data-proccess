package com.bestool.dataprocessor.service

import com.bestool.dataprocessor.dto.BatisTDetalleLlamadas
import com.bestool.dataprocessor.mapper.DetalleLlamadasMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.sql.SQLException
import javax.transaction.Transactional

@Service
class TransactionalService(
    private val detalleLlamadasMapper: DetalleLlamadasMapper
) {

    private val logger = LoggerFactory.getLogger(TransactionalService::class.java)

    @Transactional
    fun guardarBatchMyBatis(batch: List<BatisTDetalleLlamadas>, fileName: String) {
        try {

            val nextId = detalleLlamadasMapper.getNextDetalleLlamadaId()
            var currentId = nextId

            val itemsConId = batch.map { item ->
                item.copy(id = currentId++ ) // asumiendo que tu DTO permite `.copy()`
            }
            detalleLlamadasMapper.insertBatch(itemsConId)

        } catch (ex: SQLException) {
            logger.error("❌ Error SQL al guardar con MyBatis: ${ex.message}")
            logger.error("Código de error: ${ex.errorCode}")
            if (ex.message?.contains("ORA-") == true) {
                logger.error("❌ ORACLE ERROR $fileName - ${ex.message}", ex)
            }
            throw ex
        } catch (ex: Exception) {
            logger.error("❌ Error general al guardar con MyBatis", ex)
            throw ex
        }
    }


}

