package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// A quien se le vende el café. Es la versión de Room de data/model/Cliente.kt (la data class del modelo
// sigue siendo la que valida los datos; esta clase solo describe la tabla).
// La venta "Sin registrar" NO crea un cliente: se guarda con VentaEntity.clienteId = null.

/**
 * - id: lo asigna Room al insertar (0 = todavía no guardado).
 * - documento: solo dígitos (cédula o NIT). Es único: dos clientes no pueden compartir documento.
 * - tipoDocumento: nombre del enum TipoDocumento ("CC" o "NIT"), igual que Productor.
 * - telefono: opcional; cadena vacía si no se escribió.
 */
@Entity(
    tableName = "cliente",
    indices = [Index(value = ["documento"], unique = true)],
)
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val documento: String,
    @ColumnInfo(name = "tipo_documento") val tipoDocumento: String,
    val telefono: String = "",
)
