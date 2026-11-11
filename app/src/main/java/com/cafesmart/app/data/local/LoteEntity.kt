package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Agrupación lógica de sublotes por tipo y calidad. Versión de Room de data/model/Lote.kt.
// No guarda pesos ni totales: se calculan sumando sus sublotes al leer.

/**
 * - tipoCafe: nombre del enum TipoCafe ("VERDE", "SECO", "PASILLA", "TRILLADO").
 * - calidad: nombre del enum Calidad ("BUENA", "REGULAR", "MALA").
 * No puede haber dos lotes con el mismo (tipo_cafe, calidad): al registrar una compra se busca el
 * lote y, si no existe, se crea.
 */
@Entity(
    tableName = "lote",
    indices = [Index(value = ["tipo_cafe", "calidad"], unique = true)],
)
data class LoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "tipo_cafe") val tipoCafe: String,
    val calidad: String,
)
