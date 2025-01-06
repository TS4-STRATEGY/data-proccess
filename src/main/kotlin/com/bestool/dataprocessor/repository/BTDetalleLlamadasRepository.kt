package com.bestool.dataprocessor.repository

import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Date

interface BTDetalleLlamadasRepository : JpaRepository<BTDetalleLlamadas, Long> {
    @Query("""
    SELECT l FROM BTDetalleLlamadas l
    WHERE CONCAT(
        l.numFactura, '|', l.operador, '|', l.numOrigen, '|', l.numDestino, '|', 
        l.localidad, '|', l.fechaLlamada, '|', l.horaLlamada, '|', l.duracion, '|', 
        l.costo, '|', l.cargoAdicional, '|', l.tipoCargo, '|', l.modalidad, '|', l.clasificacion
    ) IN :clavesUnicas
""")
    fun findByUniqueKeys(@Param("clavesUnicas") clavesUnicas: List<String>): List<BTDetalleLlamadas>


    @Query("""
        SELECT COUNT(d) > 0 
        FROM BTDetalleLlamadas d 
        WHERE d.numFactura = :numFactura 
          AND d.operador = :operador 
          AND d.numOrigen = :numOrigen 
          AND d.numDestino = :numDestino 
          AND d.localidad = :localidad 
          AND d.fechaLlamada = :fechaLlamada 
          AND d.horaLlamada = :horaLlamada 
          AND d.duracion = :duracion 
          AND d.costo = :costo 
          AND d.cargoAdicional = :cargoAdicional 
          AND d.tipoCargo = :tipoCargo 
          AND d.modalidad = :modalidad 
          AND d.clasificacion = :clasificacion
    """)
    fun existsByUniqueConstraint(
        @Param("numFactura") numFactura: String,
        @Param("operador") operador: String?,
        @Param("numOrigen") numOrigen: String?,
        @Param("numDestino") numDestino: String?,
        @Param("localidad") localidad: String?,
        @Param("fechaLlamada") fechaLlamada: Date?,
        @Param("horaLlamada") horaLlamada: String?,
        @Param("duracion") duracion: Int?,
        @Param("costo") costo: Double?,
        @Param("cargoAdicional") cargoAdicional: Double?,
        @Param("tipoCargo") tipoCargo: String?,
        @Param("modalidad") modalidad: String?,
        @Param("clasificacion") clasificacion: String?
    ): Boolean

    @Query("""
        SELECT l
        FROM BTDetalleLlamadas l
        WHERE (l.numFactura = :numFactura AND l.operador = :operador AND 
               l.numOrigen = :numOrigen AND l.numDestino = :numDestino AND 
               l.localidad = :localidad AND l.fechaLlamada = :fechaLlamada AND 
               l.horaLlamada = :horaLlamada AND l.duracion = :duracion AND 
               l.costo = :costo AND l.cargoAdicional = :cargoAdicional AND 
               l.tipoCargo = :tipoCargo AND l.modalidad = :modalidad AND 
               l.clasificacion = :clasificacion)
    """)
    fun findExistingRecords(
        @Param("numFactura") numFactura: String,
        @Param("operador") operador: String?,
        @Param("numOrigen") numOrigen: String?,
        @Param("numDestino") numDestino: String?,
        @Param("localidad") localidad: String?,
        @Param("fechaLlamada") fechaLlamada: Date?,
        @Param("horaLlamada") horaLlamada: String?,
        @Param("duracion") duracion: Int?,
        @Param("costo") costo: Double?,
        @Param("cargoAdicional") cargoAdicional: Double?,
        @Param("tipoCargo") tipoCargo: String?,
        @Param("modalidad") modalidad: String?,
        @Param("clasificacion") clasificacion: String?
    ): List<BTDetalleLlamadas>}