package com.bestool.dataproccessor.repository

import com.bestool.dataproccessor.entity.BTDetalleLlamadas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface BTDetalleLlamadasRepository : JpaRepository<BTDetalleLlamadas, Long> {
    @Query(
        "SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END FROM BTDetalleLlamadas l WHERE " +
                "l.numFactura = :numFactura AND " +
                "(l.operador = :operador OR (:operador IS NULL AND l.operador IS NULL)) AND " +
                "(l.numOrigen = :numOrigen OR (:numOrigen IS NULL AND l.numOrigen IS NULL)) AND " +
                "(l.numDestino = :numDestino OR (:numDestino IS NULL AND l.numDestino IS NULL)) AND " +
                "l.localidad = :localidad AND " +
                "l.fechaLlamada = :fechaLlamada AND " +
                "(l.horaLlamada = :horaLlamada OR (:horaLlamada IS NULL AND l.horaLlamada IS NULL)) AND " +
                "(l.duracion = :duracion OR (:duracion IS NULL AND l.duracion IS NULL)) AND " +
                "(l.costo = :costo OR (:costo IS NULL AND l.costo IS NULL)) AND " +
                "(l.cargoAdicional = :cargoAdicional OR (:cargoAdicional IS NULL AND l.cargoAdicional IS NULL)) AND " +
                "(l.tipoCargo = :tipoCargo OR (:tipoCargo IS NULL AND l.tipoCargo IS NULL)) AND " +
                "l.modalidad = :modalidad AND " +
                "(l.clasificacion = :clasificacion OR (:clasificacion IS NULL AND l.clasificacion IS NULL))"
    )
    fun existsByAllFields(
        @Param("numFactura") numFactura: String,
        @Param("operador") operador: String?,
        @Param("numOrigen") numOrigen: String?,
        @Param("numDestino") numDestino: String?,
        @Param("localidad") localidad: String,
        @Param("fechaLlamada") fechaLlamada: Date, // Verifica si es java.util.Date o java.sql.Date
        @Param("horaLlamada") horaLlamada: String?,
        @Param("duracion") duracion: Int?,
        @Param("costo") costo: Double?,
        @Param("cargoAdicional") cargoAdicional: Double?,
        @Param("tipoCargo") tipoCargo: String?,
        @Param("modalidad") modalidad: String,
        @Param("clasificacion") clasificacion: String?
    ): Boolean
}