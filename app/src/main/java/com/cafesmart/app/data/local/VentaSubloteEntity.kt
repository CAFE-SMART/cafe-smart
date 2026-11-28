package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// Detalle de la venta: qué sublote se vendió, cuántos kg y a qué precio.
// Versión de Room de VentaSublote en data/model/Venta.kt.

/**
 * - Llave primaria compuesta (venta_id, sublote_id): un sublote aparece una sola vez por venta.
 * - Con RESTRICT no se puede borrar una venta ni un sublote que ya tengan detalle de venta.
 * - pesoKg: kg vendidos de ese sublote.
 * - precioKg: pesos enteros por kg.
 */
@Entity(
    tableName = "venta_sublote",
    primaryKeys = ["venta_id", "sublote_id"],
    foreignKeys = [
        ForeignKey(
            entity = VentaEntity::class,
            parentColumns = ["id"],
            childColumns = ["venta_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SubloteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sublote_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["venta_id"]),
        Index(value = ["sublote_id"]),
    ],
)
data class VentaSubloteEntity(
    @ColumnInfo(name = "venta_id") val ventaId: Long,
    @ColumnInfo(name = "sublote_id") val subloteId: Long,
    @ColumnInfo(name = "peso_kg") val pesoKg: Double,
    @ColumnInfo(name = "precio_kg") val precioKg: Long,
)
