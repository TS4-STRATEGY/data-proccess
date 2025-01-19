package com.bestool.dataprocessor.dto

data class ProgresoProceso(
    var factura: String? = "",
    val archivo: String? = "",
    val status: String? = "",
    val numeroLinea: Long? = 0,
    val totalLinesFile: Long? = 0,
    val totalLinesInBase: Long? = 0
)