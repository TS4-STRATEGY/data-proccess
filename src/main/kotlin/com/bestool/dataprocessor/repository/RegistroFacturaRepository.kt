package com.bestool.dataprocessor.repository

import com.bestool.dataprocessor.entity.RegistroFactura
import org.springframework.data.jpa.repository.JpaRepository

interface RegistroFacturaRepository : JpaRepository<RegistroFactura, Long> {
    fun findByFactura(factura: String): RegistroFactura?
    fun findByArchivo(archivo: String): RegistroFactura?
}
