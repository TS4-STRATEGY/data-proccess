package com.bestool.dataprocessor.mapper

import com.bestool.dataprocessor.dto.BatisTDetalleLlamadas
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface DetalleLlamadasMapper {
    fun insertBatch(@Param("list") llamadas: List<BatisTDetalleLlamadas>)
    @Select("SELECT SEQ_BT_DETALLE_LLAMADAS.NEXTVAL FROM DUAL")
    fun getNextDetalleLlamadaId(): Long
}