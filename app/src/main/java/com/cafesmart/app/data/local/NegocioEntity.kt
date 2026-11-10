package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cafesmart.app.viewmodel.inventory.KG_POR_BULTO

// Datos del negocio cafetero. Firebase Auth solo guarda la cuenta (correo, contraseña y nombre);
// lo del negocio vive aquí, en Room. Una fila por cuenta: el id es el uid de Firebase.

/**
 * - uid: id del usuario en Firebase Authentication.
 * - nombre: nombre del negocio (por ejemplo, "Café La Esperanza").
 * - tipo: nombre del enum BusinessType ("COMPRAVENTA", "COOPERATIVA", "PERSONALIZADO"). Se guarda el
 *   nombre y no el texto visible para que cambiar la etiqueta no rompa los datos guardados.
 * - telefono: celular de contacto, tal como se escribió en el registro.
 * - capacidadBodegaKg: 0 = bodega sin configurar (estado propio, no es bodega vacía). Se configura en Ajustes.
 * - pesoBultoKg: kg de un bulto; valor inicial KG_POR_BULTO (40,7). Se configurará en Ajustes.
 * Los nombres y apellidos del administrador NO van aquí: están en la cuenta de Firebase (displayName).
 * La contraseña nunca se guarda en Room.
 */
@Entity(tableName = "negocio")
data class NegocioEntity(
    @PrimaryKey val uid: String,
    val nombre: String,
    val tipo: String,
    val telefono: String,
    @ColumnInfo(name = "capacidad_bodega_kg") val capacidadBodegaKg: Double = 0.0,
    @ColumnInfo(name = "peso_bulto_kg") val pesoBultoKg: Double = KG_POR_BULTO,
    // Fecha de creación en milisegundos (System.currentTimeMillis).
    @ColumnInfo(name = "creado_en") val creadoEn: Long,
)
