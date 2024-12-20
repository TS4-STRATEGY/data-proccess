package com.bestool.dataproccessor.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "BT_DETALLE_LLAMADAS")
data class BTDetalleLlamadas(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_detalle")
    @SequenceGenerator(name = "seq_detalle", sequenceName = "SEQ_BT_DETALLE_LLAMADAS", allocationSize = 1)
    @Column(name = "BDL_ID") val id: Long = 0,
    @Column(name = "BDL_NUM_FACTURA") val numFactura: String,
    @Column(name = "BDL_OPERADOR") val operador: String?,
    @Column(name = "BDL_NUM_ORIGEN") val numOrigen: String?,
    @Column(name = "BDL_NUM_DESTINO") val numDestino: String?,
    @Column(name = "BDL_LOCALIDAD") val localidad: String?,
    @Column(name = "BDL_FECHA_LLAMADA") val fechaLlamada: Date?,
    @Column(name = "BDL_HORA_LLAMADA") val horaLlamada: String?,
    @Column(name = "BDL_DURACION") val duracion: Int?,
    @Column(name = "BDL_COSTO") val costo: Double?,
    @Column(name = "BDL_CARGO_ADICIONAL") val cargoAdicional: Double?,
    @Column(name = "BDL_TIPO_CARGO") val tipoCargo: String?,
    @Column(name = "BDL_MODALIDAD") val modalidad: String?,
    @Column(name = "BDL_CLASIFICACION") val clasificacion: String?,
    @Column(name = "BDL_FECHA_CREACION") val fechaCreacion: Date?,
    @Column(name = "BDL_ACTIVO") val activo: Int,
    @Column(name = "BDL_ID_CENTRO_COSTOS") val idCentroCostos: Int?,
    @Version
    @Column(name = "VERSION")
    var version: Long? = null

) {
    constructor() : this(
        id = 0,
        numFactura = "",
        operador = null,
        numOrigen = null,
        numDestino = null,
        localidad = null,
        fechaLlamada = null,
        horaLlamada = null,
        duracion = null,
        costo = null,
        cargoAdicional = null,
        tipoCargo = null,
        modalidad = null,
        clasificacion = null,
        fechaCreacion = null,
        activo = 1,
        idCentroCostos = null
    )
}