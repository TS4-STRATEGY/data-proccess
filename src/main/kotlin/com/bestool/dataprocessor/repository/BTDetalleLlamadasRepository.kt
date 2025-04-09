package com.bestool.dataprocessor.repository

import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BTDetalleLlamadasRepository : JpaRepository<BTDetalleLlamadas, Long> {

    @Query(
        value = """
        SELECT 
            BDL_NUM_FACTURA, 
            MAX(BDL_FECHA_CREACION) AS ULTIMA_FECHA, 
            COUNT(*) AS CANTIDAD_REGISTROS
        FROM BT_DETALLE_LLAMADAS
        WHERE BDL_FECHA_CREACION >= ADD_MONTHS(TRUNC(SYSDATE), -1)
        GROUP BY BDL_NUM_FACTURA
        ORDER BY ULTIMA_FECHA DESC
    """,
        nativeQuery = true
    )
    fun findFacturasWithCountAndLastDate(): List<Map<String, Any>>


}