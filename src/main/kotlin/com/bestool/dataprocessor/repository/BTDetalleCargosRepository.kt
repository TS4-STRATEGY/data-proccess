package com.bestool.dataprocessor.repository

import com.bestool.dataprocessor.entity.BTDetalleCargos
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BTDetalleCargosRepository : JpaRepository<BTDetalleCargos, Long> {
    @Query(
        "SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END " +
                "FROM BTDetalleCargos b " +
                "WHERE b.identificadorUnico = :idUnique "
    )
    fun existsByIdentificadorUnico(idUnique: String): Boolean
}