package com.bestool.dataproccessor.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "BT_DETALLE_CARGOS")
data class BTDetalleCargos(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cargo")
    @SequenceGenerator(name = "seq_cargo", sequenceName = "SEQ_BT_DETALLE_CARGOS", allocationSize = 1)
    @Column(name = "BDC_ID")
    val id: Long = 0,

    @Column(name = "BDC_NUM_FACTURA")
    val numFactura: String = "",

    @Column(name = "BDC_OPERADOR")
    val operador: String? = null,

    @Column(name = "BDC_TIPO_CARGO")
    val tipoCargo: String? = null,

    @Column(name = "BDC_MONTO")
    val monto: Double? = null,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "BDC_FECHA_REGISTRO")
    val fechaRegistro: Date? = null,

    @Column(name = "BDC_ACTIVO")
    val activo: Int = 1,

    @Column(name = "BDC_IDENTIFICADOR_UNICO", unique = true)
    val identificadorUnico: String = ""
) {
    // Constructor sin parámetros requerido por Hibernate
    constructor() : this(
        numFactura = "",
        operador = null,
        tipoCargo = null,
        monto = null,
        fechaRegistro = null,
        activo = 1
    )
}