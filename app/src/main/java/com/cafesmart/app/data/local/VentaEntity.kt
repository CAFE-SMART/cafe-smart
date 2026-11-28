package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Operación de venta (Venta 1 → N VentaSublote). Versión de Room de data/model/Venta.kt.

/**
 * - clienteId: null = venta "Sin registrar" (no hay cliente ficticio). Si hay cliente, no se
 *   puede borrar mientras tenga ventas (RESTRICT) para no perder el historial.
 * - fecha: milisegundos en UTC a medianoche. Se convierte con fechaAMilis / milisAFecha.
 * - totalRecibido: pesos enteros, "foto" de lo recibido al confirmar.
 */
@Entity(
    tableName = "venta",
    foreignKeys = [
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["cliente_id"])],
)
data class VentaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cliente_id") val clienteId: Long? = null,
    val fecha: Long,
    @ColumnInfo(name = "total_recibido") val totalRecibido: Long,
)
