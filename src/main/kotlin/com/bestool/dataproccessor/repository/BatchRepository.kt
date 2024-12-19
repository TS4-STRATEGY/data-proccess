package com.bestool.dataproccessor.repository

import com.bestool.dataproccessor.entity.BTDetalleLlamadas
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository

@Repository
class BatchRepository(
    @PersistenceContext private val entityManager: EntityManager
) {

    @Transactional
    fun saveAllInBatch(entities: List<BTDetalleLlamadas>, batchSize: Int = 50) {
        for ((index, entity) in entities.withIndex()) {
            entityManager.merge(entity) // Usar merge en lugar de persist
            if (index > 0 && index % batchSize == 0) {
                entityManager.flush() // Enviar el lote a la base de datos
                entityManager.clear() // Limpiar el contexto de persistencia
            }
        }
        entityManager.flush() // Enviar el lote final
        entityManager.clear() // Limpiar el contexto final
    }
}