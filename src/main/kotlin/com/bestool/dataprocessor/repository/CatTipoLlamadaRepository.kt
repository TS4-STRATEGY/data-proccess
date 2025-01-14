package com.bestool.dataprocessor.repository

import com.bestool.dataprocessor.entity.CatTipoLlamada
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CatTipoLlamadaRepository : JpaRepository<CatTipoLlamada, Long> {
    fun findByDescripcion(descripcion: String): CatTipoLlamada?
}
