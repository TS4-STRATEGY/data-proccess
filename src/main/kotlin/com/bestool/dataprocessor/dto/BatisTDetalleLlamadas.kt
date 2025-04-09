package com.bestool.dataprocessor.dto

import java.util.*

data class BatisTDetalleLlamadas(
    val numFactura: String,
    val operador: String?,
    val numOrigen: String?,
    val numDestino: String?,
    val localidad: Long,
    val fechaLlamada: Date?,
    val horaLlamada: String?,
    val duracion: Double?,
    val costo: Double?,
    val cargoAdicional: Double?,
    val tipoCargo: String?,
    val modalidad: Long,
    val clasificacion: String?,
    val fechaCreacion: Date?,
    val activo: Int,
    val idCentroCostos: Int?,
    val version: Long = 0,
    val id: Long?=0
)
