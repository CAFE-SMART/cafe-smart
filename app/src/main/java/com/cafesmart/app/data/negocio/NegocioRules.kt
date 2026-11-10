package com.cafesmart.app.data.negocio

import com.cafesmart.app.data.local.NegocioEntity

// Reglas puras para pasar los datos del registro a la entidad Negocio. Probadas con JUnit.

/**
 * Arma el Negocio que se guarda al terminar el registro.
 * Quita los espacios de los extremos. El tipo se guarda con el nombre del enum (ver NegocioEntity).
 */
fun negocioDesdeRegistro(
    uid: String,
    nombre: String,
    tipo: String,
    telefono: String,
    creadoEn: Long,
): NegocioEntity = NegocioEntity(
    uid = uid,
    nombre = nombre.trim(),
    tipo = tipo,
    telefono = telefono.trim(),
    creadoEn = creadoEn,
)
