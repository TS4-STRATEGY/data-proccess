package com.bestool.dataprocessor.dto

data class ProgresoProceso(
    val factura: String,
    val archivo: String? = "",
    val status: String,
    val numeroLinea: Long? = 0,
    val totalLinesFile: Long? = 0
)