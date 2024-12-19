package com.bestool.dataproccessor.repository

import com.bestool.dataproccessor.entity.BTDetalleFactura
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface BTDetalleFacturaRepository : JpaRepository<BTDetalleFactura, Long> {
    @Query(
        """
    SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END 
    FROM BTDetalleFactura f 
    WHERE f.numFactura = :numFactura 
    AND (f.referencia = :referencia OR (f.referencia IS NULL AND :referencia IS NULL))
"""
    )
    fun existsByFactura(numFactura: String, referencia: String?): Boolean
}