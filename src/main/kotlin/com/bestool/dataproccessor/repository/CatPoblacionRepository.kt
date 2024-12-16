package com.bestool.dataproccessor.repository

import com.bestool.dataproccessor.entity.CatPoblacion
import com.bestool.dataproccessor.entity.CatTipoLlamada
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CatPoblacionRepository : JpaRepository<CatPoblacion, Long> {
    fun findByDescripcion(descripcion: String): CatPoblacion?
}

@Repository
interface CatTipoLlamadaRepository : JpaRepository<CatTipoLlamada, Long> {
    fun findByDescripcion(descripcion: String): CatTipoLlamada?
}
