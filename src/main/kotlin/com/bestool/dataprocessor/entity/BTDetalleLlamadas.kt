package com.bestool.dataprocessor.entity

import javax.persistence.*
import java.util.*

@Entity
@Table(name = "BT_DETALLE_LLAMADAS")
data class BTDetalleLlamadas(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BT_DETALLE_LLAMADAS")
    @SequenceGenerator(name = "SEQ_BT_DETALLE_LLAMADAS", sequenceName = "SEQ_BT_DETALLE_LLAMADAS", allocationSize = 500)
    @Column(name = "BDL_ID", insertable = false, updatable = false) val id: Long = 0,
    @Column(name = "BDL_NUM_FACTURA") val numFactura: String,
    @Column(name = "BDL_OPERADOR") val operador: String?,
    @Column(name = "BDL_NUM_ORIGEN") val numOrigen: String?,
    @Column(name = "BDL_NUM_DESTINO") val numDestino: String?,
    @Column(name = "BDL_LOCALIDAD") val localidad: Long,
    @Column(name = "BDL_FECHA_LLAMADA") val fechaLlamada: Date?,
    @Column(name = "BDL_HORA_LLAMADA") val horaLlamada: String?,
    @Column(name = "BDL_DURACION", columnDefinition = "NUMBER") val duracion: Double?,
    @Column(name = "BDL_COSTO", columnDefinition = "NUMBER") val costo: Double?,
    @Column(name = "BDL_CARGO_ADICIONAL", columnDefinition = "NUMBER") val cargoAdicional: Double?,
    @Column(name = "BDL_TIPO_CARGO") val tipoCargo: String?,
    @Column(name = "BDL_MODALIDAD") val modalidad: Long,
    @Column(name = "BDL_CLASIFICACION") val clasificacion: String?,
    @Column(name = "BDL_FECHA_CREACION") val fechaCreacion: Date?,
    @Column(name = "BDL_ACTIVO") val activo: Int,
    @Column(name = "BDL_ID_CENTRO_COSTOS") val idCentroCostos: Int?,
    @Version
    @Column(name = "VERSION")
    var version: Long = 0

) {
    constructor() : this(
        id = 0,
        numFactura = "",
        operador = null,
        numOrigen = null,
        numDestino = null,
        localidad = 0,
        fechaLlamada = null,
        horaLlamada = null,
        duracion = null,
        costo = null,
        cargoAdicional = null,
        tipoCargo = null,
        modalidad = 0,
        clasificacion = null,
        fechaCreacion = null,
        activo = 1,
        idCentroCostos = null
    )
}