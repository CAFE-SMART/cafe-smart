package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Operación de compra (Compra 1 → N Sublote). Versión de Room de data/model/Compra.kt.

/**
 * - productorId: null = compra "Sin registrar" (no hay productor ficticio). Si hay productor, no se
 *   puede borrar mientras tenga compras (RESTRICT) para no perder el historial.
 * - fecha: milisegundos en UTC a medianoche. Se convierte con fechaAMilis / milisAFecha.
 * - totalPagado: pesos enteros, "foto" de lo pagado al confirmar (no cambia si se edita un sublote).
 */
@Entity(
    tableName = "compra",
    foreignKeys = [
        ForeignKey(
            entity = ProductorEntity::class,
            parentColumns = ["id"],
            childColumns = ["productor_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["productor_id"])],
)
data class CompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "productor_id") val productorId: Long? = null,
    val fecha: Long,
    @ColumnInfo(name = "total_pagado") val totalPagado: Long,
)
