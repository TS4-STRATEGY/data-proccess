package com.bestool.dataprocessor.entity

import javax.persistence.*
import java.util.*

@Entity
@Table(name = "BT_CAT_TIPOS_LLAMADAS")
data class CatTipoLlamada(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BT_CAT_TIPOS_LLAMADAS")
    @SequenceGenerator(name = "SEQ_BT_CAT_TIPOS_LLAMADAS", sequenceName = "SEQ_BT_CAT_TIPOS_LLAMADAS", allocationSize = 1)
    @Column(name = "BCTL_ID", insertable = false, updatable = false)
    val id: Long = 0,

    @Column(name = "BCTL_DESCRIPCION", unique = true, nullable = false)
    val descripcion: String,

    @Column(name = "BCTL_NIVEL", nullable = false)
    val nivel: Int = 1,

    @Column(name = "BCTL_FECHA_CREACION", nullable = false)
    val fechaCreacion: Date = Date(),

    @Column(name = "BCTL_ACTIVO", nullable = false)
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