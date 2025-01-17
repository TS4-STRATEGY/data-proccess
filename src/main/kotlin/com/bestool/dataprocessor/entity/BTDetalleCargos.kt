package com.bestool.dataprocessor.entity

import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "BT_DETALLE_CARGOS", schema = "QA_BESTOOLS_OWNER")
data class BTDetalleCargos(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "QA_BESTOOLS_OWNER.SEQ_BT_DETALLE_CARGOS")
    @SequenceGenerator(name = "QA_BESTOOLS_OWNER.SEQ_BT_DETALLE_CARGOS", sequenceName = "QA_BESTOOLS_OWNER.SEQ_BT_DETALLE_CARGOS", allocationSize = 1)
    @Column(name = "BDC_ID", insertable = false, updatable = false)
    val id: Long = 0,

    @Column(name = "BDC_NUM_FACTURA")
    val numFactura: String = "",

    @Column(name = "BDC_OPERADOR")
    val operador: String? = null,

    @Column(name = "BDC_TIPO_CARGO")
    val tipoCargo: String? = null,

    @Column(name = "BDC_MONTO", columnDefinition = "NUMBER")
    val monto: BigDecimal? = null,

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