package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Unidad real del inventario. Versión de Room de data/model/Sublote.kt (la data class sigue
// validando rangos y reglas; aquí solo se describe la tabla y sus llaves foráneas).

/**
 * - loteId: obligatorio. El tipo y la calidad viven solo en el Lote.
 * - compraId / secadoId: el sublote nace de una compra o de un secado, nunca de ambos (la regla se
 *   valida en la data class Sublote). Con RESTRICT, ni el lote ni la compra se pueden borrar
 *   mientras tengan sublotes.
 * - secadoId: todavía SIN llave foránea porque la tabla de Secado no existe; quien agregue
 *   SecadoEntity le pone el ForeignKey y sube la versión de la base.
 * - fechaIngreso: milisegundos UTC (fechaAMilis / milisAFecha).
 * - costoKg: pesos enteros por kg.
 * - humedadPorcentaje y factorRendimiento: opcionales (null si no se registraron).
 */
@Entity(
    tableName = "sublote",
    foreignKeys = [
        ForeignKey(
            entity = LoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["lote_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CompraEntity::class,
            parentColumns = ["id"],
            childColumns = ["compra_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["lote_id"]),
        Index(value = ["compra_id"]),
        Index(value = ["secado_id"]),
    ],
)
data class SubloteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lote_id") val loteId: Long,
    @ColumnInfo(name = "compra_id") val compraId: Long? = null,
    @ColumnInfo(name = "secado_id") val secadoId: Long? = null,
    @ColumnInfo(name = "peso_inicial_kg") val pesoInicialKg: Double,
    @ColumnInfo(name = "peso_disponible_kg") val pesoDisponibleKg: Double,
    @ColumnInfo(name = "costo_kg") val costoKg: Long,
    @ColumnInfo(name = "fecha_ingreso") val fechaIngreso: Long,
    @ColumnInfo(name = "humedad_porcentaje") val humedadPorcentaje: Double? = null,
    @ColumnInfo(name = "factor_rendimiento") val factorRendimiento: Int? = null,
)
