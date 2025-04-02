package com.bestool.dataprocessor.entity

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "BT_REGISTRO_FACTURAS")
data class RegistroFactura(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BT_REGISTRO_FACTURAS")
    @SequenceGenerator(name = "SEQ_BT_REGISTRO_FACTURAS", sequenceName = "SEQ_BT_REGISTRO_FACTURAS", allocationSize = 1)
    @Column(name = "BRF_ID_FACTURA_ARCHIVO")
    val id: Long? = null,

    @Column(name = "BRF_FACTURA", nullable = false)
    var factura: String?="",

    @Column(name = "BRF_ARCHIVO", nullable = false)
    var archivo: String?="",

    @Column(name = "BRF_STATUS", nullable = false)
    var status: String?="",

    @Column(name = "BRF_NUMERO_LINEA", nullable = false)
    var numeroLinea: Long = 0,

    @Column(name = "BRF_TOTAL_LINES_FILE", nullable = false)
    var totalLinesFile: Long = 0,

    @Column(name = "BRF_TOTAL_LINES_IN_BASE", nullable = false)
    var totalLinesInBase: Long = 0,

    @Column(name = "BRF_FECHA_CREACION", nullable = false)
    var fechaCreacion: LocalDateTime = LocalDateTime.now()
)
