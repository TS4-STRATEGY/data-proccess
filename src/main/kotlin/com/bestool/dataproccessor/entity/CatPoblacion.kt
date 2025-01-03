package com.bestool.dataproccessor.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "BT_CAT_POBLACIONES")
data class CatPoblacion(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_poblacion")
    @SequenceGenerator(name = "seq_poblacion", sequenceName = "SEQ_BT_CAT_POBLACIONES", allocationSize = 1)
    @Column(name = "BCP_ID")
    val id: Long = 0,

    @Column(name = "BCP_DESCRIPCION", unique = true, nullable = false)
    val descripcion: String,

    @Column(name = "BCP_NIVEL", nullable = false)
    val nivel: Int = 1,

    @Column(name = "BCP_FECHA_CREACION", nullable = false)
    val fechaCreacion: Date = Date(),

    @Column(name = "BCP_ACTIVO", nullable = false)
    val activo: Int = 1
){
    // Constructor por defecto requerido por Hibernate
    constructor() : this(
        id = 0,
        descripcion = "",
        nivel = 1,
        fechaCreacion = Date(),
        activo = 1
    )
}