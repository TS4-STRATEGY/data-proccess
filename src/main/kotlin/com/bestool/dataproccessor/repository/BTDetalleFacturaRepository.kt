package com.bestool.dataproccessor.repository

import com.bestool.dataproccessor.entity.BTDetalleFactura
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface BTDetalleFacturaRepository : JpaRepository<BTDetalleFactura, Long> {
    @Query(
        """
    SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END 
    FROM BTDetalleFactura f 
    WHERE f.numFactura = :numFactura 
    AND (f.referencia = :referencia OR (f.referencia IS NULL AND :referencia IS NULL))
    AND (f.operador = :operador OR (f.operador IS NULL AND :operador IS NULL))
    AND (f.fechaEmision = :fechaEmision OR (f.fechaEmision IS NULL AND :fechaEmision IS NULL))
    AND (f.fechaVencimiento = :fechaVencimiento OR (f.fechaVencimiento IS NULL AND :fechaVencimiento IS NULL))
    AND (f.fechaCorte = :fechaCorte OR (f.fechaCorte IS NULL AND :fechaCorte IS NULL))
    AND (f.moneda = :moneda OR (f.moneda IS NULL AND :moneda IS NULL))
    AND (f.tipoMoneda = :tipoMoneda OR (f.tipoMoneda IS NULL AND :tipoMoneda IS NULL))
    AND (f.iva = :iva OR (f.iva IS NULL AND :iva IS NULL))
    AND (f.subtotal = :subtotal OR (f.subtotal IS NULL AND :subtotal IS NULL))
    AND (f.impuestos = :impuestos OR (f.impuestos IS NULL AND :impuestos IS NULL))
    AND (f.total = :total OR (f.total IS NULL AND :total IS NULL))
    AND (f.totalLetra = :totalLetra OR (f.totalLetra IS NULL AND :totalLetra IS NULL))
    AND (f.saldoAnterior = :saldoAnterior OR (f.saldoAnterior IS NULL AND :saldoAnterior IS NULL))
    AND (f.descuento = :descuento OR (f.descuento IS NULL AND :descuento IS NULL))
    AND (f.otrosCargos = :otrosCargos OR (f.otrosCargos IS NULL AND :otrosCargos IS NULL))
    AND (f.subtotal2 = :subtotal2 OR (f.subtotal2 IS NULL AND :subtotal2 IS NULL))
    AND (f.impuestos2 = :impuestos2 OR (f.impuestos2 IS NULL AND :impuestos2 IS NULL))
    AND (f.total2 = :total2 OR (f.total2 IS NULL AND :total2 IS NULL))
    AND (f.totalFinal = :totalFinal OR (f.totalFinal IS NULL AND :totalFinal IS NULL))
    AND (f.nombreCliente = :nombreCliente OR (f.nombreCliente IS NULL AND :nombreCliente IS NULL))
    AND (f.descripcionCliente = :descripcionCliente OR (f.descripcionCliente IS NULL AND :descripcionCliente IS NULL))
    AND (f.sucursal = :sucursal OR (f.sucursal IS NULL AND :sucursal IS NULL))
    AND (f.numeroCuenta = :numeroCuenta OR (f.numeroCuenta IS NULL AND :numeroCuenta IS NULL))
    AND (f.rfc = :rfc OR (f.rfc IS NULL AND :rfc IS NULL))
    AND (f.referenciaAdicional = :referenciaAdicional OR (f.referenciaAdicional IS NULL AND :referenciaAdicional IS NULL))
    AND (f.nombreClienteAdicional = :nombreClienteAdicional OR (f.nombreClienteAdicional IS NULL AND :nombreClienteAdicional IS NULL))
    AND (f.domicilio = :domicilio OR (f.domicilio IS NULL AND :domicilio IS NULL))
    AND (f.ubicacion = :ubicacion OR (f.ubicacion IS NULL AND :ubicacion IS NULL))
    AND (f.localidad = :localidad OR (f.localidad IS NULL AND :localidad IS NULL))
    AND (f.estado = :estado OR (f.estado IS NULL AND :estado IS NULL))
    AND (f.municipio = :municipio OR (f.municipio IS NULL AND :municipio IS NULL))
    AND (f.codigoPostal = :codigoPostal OR (f.codigoPostal IS NULL AND :codigoPostal IS NULL))
    AND (f.pais = :pais OR (f.pais IS NULL AND :pais IS NULL))
    AND (f.domicilioFiscal = :domicilioFiscal OR (f.domicilioFiscal IS NULL AND :domicilioFiscal IS NULL))
    AND (f.ubicacionFiscal = :ubicacionFiscal OR (f.ubicacionFiscal IS NULL AND :ubicacionFiscal IS NULL))
    AND (f.localidadFiscal = :localidadFiscal OR (f.localidadFiscal IS NULL AND :localidadFiscal IS NULL))
    AND (f.estadoFiscal = :estadoFiscal OR (f.estadoFiscal IS NULL AND :estadoFiscal IS NULL))
    AND (f.municipioFiscal = :municipioFiscal OR (f.municipioFiscal IS NULL AND :municipioFiscal IS NULL))
    AND (f.codigoPostalFiscal = :codigoPostalFiscal OR (f.codigoPostalFiscal IS NULL AND :codigoPostalFiscal IS NULL))
    AND (f.numFacturacion = :numFacturacion OR (f.numFacturacion IS NULL AND :numFacturacion IS NULL))
    AND (f.paisFiscal = :paisFiscal OR (f.paisFiscal IS NULL AND :paisFiscal IS NULL))
    AND (f.notas = :notas OR (f.notas IS NULL AND :notas IS NULL))
"""
    )
    fun existsByAllFields(
        numFactura: String,
        referencia: String?,
        operador: String?,
        fechaEmision: Date?,
        fechaVencimiento: Date?,
        fechaCorte: Date?,
        moneda: String?,
        tipoMoneda: String?,
        iva: Double?,
        subtotal: Double?,
        impuestos: Double?,
        total: Double?,
        totalLetra: String?,
        saldoAnterior: Double?,
        descuento: Double?,
        otrosCargos: Double?,
        subtotal2: Double?,
        impuestos2: Double?,
        total2: Double?,
        totalFinal: Double?,
        nombreCliente: String?,
        descripcionCliente: String?,
        sucursal: String?,
        numeroCuenta: String?,
        rfc: String?,
        referenciaAdicional: String?,
        nombreClienteAdicional: String?,
        domicilio: String?,
        ubicacion: String?,
        localidad: String?,
        estado: String?,
        municipio: String?,
        codigoPostal: String?,
        pais: String?,
        domicilioFiscal: String?,
        ubicacionFiscal: String?,
        localidadFiscal: String?,
        estadoFiscal: String?,
        municipioFiscal: String?,
        codigoPostalFiscal: String?,
        numFacturacion: String?,
        paisFiscal: String?,
        notas: String?
    ): Boolean
}