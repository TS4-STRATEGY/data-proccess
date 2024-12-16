package com.bestool.dataproccessor.repository

import com.bestool.dataproccessor.entity.BTDetalleCargos
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BTDetalleCargosRepository : JpaRepository<BTDetalleCargos, Long> {
    @Query(
        "SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END " +
                "FROM BTDetalleCargos b " +
                "WHERE b.numFactura = :numFactura " +
                "AND (:operador IS NULL OR b.operador = :operador) " +
                "AND (:tipoCargo IS NULL OR b.tipoCargo = :tipoCargo) " +
                "AND (:monto IS NULL OR b.monto = :monto)"
    )
    fun existsByNumFacturaAndOperadorAndTipoCargoAndMonto(
        numFactura: String,
        operador: String?,
        tipoCargo: String?,
        monto: Double?
    ): Boolean
}