package com.bestool.dataprocessor.repository

import com.bestool.dataprocessor.entity.BTDetalleLlamadas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BTDetalleLlamadasRepository : JpaRepository<BTDetalleLlamadas, Long> {


    @Query(
        "SELECT l FROM BTDetalleLlamadas l WHERE " +
                "l.numFactura = :numFactura AND " +
                "(l.operador = :operador OR (:operador IS NULL AND l.operador IS NULL)) AND " +
                "(l.numOrigen = :numOrigen OR (:numOrigen IS NULL AND l.numOrigen IS NULL)) AND " +
                "(l.numDestino = :numDestino OR (:numDestino IS NULL AND l.numDestino IS NULL)) AND " +
                "l.localidad = :localidad AND " +
                "(l.horaLlamada = :horaLlamada OR (:horaLlamada IS NULL AND l.horaLlamada IS NULL)) AND " +
                "(l.duracion = :duracion OR (:duracion IS NULL AND l.duracion IS NULL)) AND " +
                "(l.costo = :costo OR (:costo IS NULL AND l.costo IS NULL)) AND " +
                "(l.cargoAdicional = :cargoAdicional OR (:cargoAdicional IS NULL AND l.cargoAdicional IS NULL)) AND " +
                "(l.tipoCargo = :tipoCargo OR (:tipoCargo IS NULL AND l.tipoCargo IS NULL)) AND " +
                "l.modalidad = :modalidad AND " +
                "(l.clasificacion = :clasificacion OR (:clasificacion IS NULL AND l.clasificacion IS NULL))"
    )
    fun findByAllFields(
        @Param("numFactura") numFactura: String,
        @Param("operador") operador: String?,
        @Param("numOrigen") numOrigen: String?,
        @Param("numDestino") numDestino: String?,
        @Param("localidad") localidad: Long,
        @Param("horaLlamada") horaLlamada: String?,
        @Param("duracion") duracion: Int?,
        @Param("costo") costo: Double?,
        @Param("cargoAdicional") cargoAdicional: Double?,
        @Param("tipoCargo") tipoCargo: String?,
        @Param("modalidad") modalidad: Long,
        @Param("clasificacion") clasificacion: String?
    ): BTDetalleLlamadas?

    @Query(
        value = """
        SELECT 
            BDL_NUM_FACTURA, 
            MAX(BDL_FECHA_CREACION) AS ULTIMA_FECHA, 
            COUNT(*) AS CANTIDAD_REGISTROS
        FROM QA_BESTOOLS_OWNER.BT_DETALLE_LLAMADAS
        GROUP BY BDL_NUM_FACTURA
        ORDER BY ULTIMA_FECHA DESC
        """,
        nativeQuery = true
    )
    fun findFacturasWithCountAndLastDate(): List<Map<String, Any>>


    @Query(
        value = """
            SELECT *
            FROM (
                SELECT *
                FROM QA_BESTOOLS_OWNER.BT_DETALLE_LLAMADAS
                WHERE BDL_NUM_FACTURA = :numFactura
                ORDER BY BDL_FECHA_CREACION DESC
            )
            WHERE ROWNUM = 1
        """,
        nativeQuery = true
    )
    fun findLastByFactura(@Param("numFactura") numFactura: String): BTDetalleLlamadas?

    @Query(
        """
    SELECT CONCAT(l.numFactura, '|', l.numOrigen, '|', l.fechaLlamada, '|', l.horaLlamada, '|', l.duracion)
    FROM BTDetalleLlamadas l
    WHERE CONCAT(l.numFactura, '|', l.numOrigen, '|', l.fechaLlamada, '|', l.horaLlamada, '|', l.duracion) IN :batchKeys
    """
    )
    fun findExistingBatchKeys(@Param("batchKeys") batchKeys: List<String>): List<String>
}