package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Quien vende el café. Es la versión de Room de data/model/Productor.kt (la data class del modelo
// sigue siendo la que valida los datos; esta clase solo describe la tabla).
// La compra "Sin registrar" NO crea un productor: se guarda con CompraEntity.productorId = null.

/**
 * - id: lo asigna Room al insertar (0 = todavía no guardado).
 * - documento: solo dígitos (cédula o NIT). Es único: dos productores no pueden compartir documento.
 * - tipoDocumento: nombre del enum TipoDocumento ("CC" o "NIT"), igual que Negocio guarda su tipo.
 * - telefono: opcional; cadena vacía si no se escribió.
 */
@Entity(
    tableName = "productor",
    indices = [Index(value = ["documento"], unique = true)],
)
data class ProductorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val documento: String,
    @ColumnInfo(name = "tipo_documento") val tipoDocumento: String,
    val telefono: String = "",
)
