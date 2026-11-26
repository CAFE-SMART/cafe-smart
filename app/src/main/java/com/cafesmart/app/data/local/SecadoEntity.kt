package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Proceso de secado (café verde → café seco). Versión de Room de data/model/Secado.kt (la data class sigue
// validando las reglas; aquí solo se describen las tablas y sus llaves foráneas).

/**
 * - estado: el nombre del enum EstadoSecado como texto ("EN_CURSO", "FINALIZADO" o "CANCELADO"),
 *   igual que lote.tipo_cafe.
 * - fechaInicio y fechaFin: milisegundos UTC (fechaAMilis / milisAFecha), igual que sublote.fecha_ingreso.
 *   fechaFin es null mientras el secado está EN_CURSO (o si se canceló).
 * - factorRendimiento: opcional (null si no se registró); entre 80 y 120.
 * - La merma NO se guarda: se calcula (kg enviados − kg secos). Ver SecadoConDetalle.
 */
@Entity(tableName = "secado")
data class SecadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "fecha_inicio") val fechaInicio: Long,
    @ColumnInfo(name = "fecha_fin") val fechaFin: Long? = null,
    val estado: String,
    @ColumnInfo(name = "factor_rendimiento") val factorRendimiento: Int? = null,
)

/**
 * Tabla intermedia Secado <-> Sublote: cuántos kg de cada sublote verde se enviaron a secar.
 * La llave primaria es (secado_id, sublote_id): un sublote aparece una sola vez por secado.
 * Con RESTRICT no se puede borrar un secado ni un sublote que tengan envíos, así no se pierde el historial.
 * El índice de sublote_id permite buscar rápido a qué secados se envió un sublote.
 */
@Entity(
    tableName = "secado_sublote",
    primaryKeys = ["secado_id", "sublote_id"],
    foreignKeys = [
        ForeignKey(
            entity = SecadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["secado_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SubloteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sublote_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["sublote_id"])],
)
data class SecadoSubloteEntity(
    @ColumnInfo(name = "secado_id") val secadoId: Long,
    @ColumnInfo(name = "sublote_id") val subloteId: Long,
    @ColumnInfo(name = "peso_enviado_kg") val pesoEnviadoKg: Double,
)
