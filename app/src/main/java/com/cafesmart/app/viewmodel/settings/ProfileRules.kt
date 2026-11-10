package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.session.UsuarioSesion
import com.cafesmart.app.ui.auth.BusinessType

// Reglas para armar el perfil que muestra Ajustes. Funciones puras, probadas con JUnit.

/** Texto que se muestra cuando la cuenta no tiene nombre (por ejemplo, creada desde la consola de Firebase). */
const val NOMBRE_FALTANTE = "Sin nombre"

/** Lo que muestra la tarjeta de perfil. */
data class PerfilUsuario(
    // null = la cuenta no tiene nombre; la pantalla muestra NOMBRE_FALTANTE.
    val nombre: String? = null,
    // null = no se muestra la línea del correo.
    val correo: String? = null,
    // Texto visible del tipo de negocio ("Compraventa"); null = no se muestra la etiqueta.
    val tipoNegocio: String? = null,
    // Nombre del negocio; null = la fila "Información de empresa" muestra su subtítulo por defecto.
    val nombreNegocio: String? = null,
)

/**
 * Arma el perfil con la cuenta (Firebase) y el negocio (Room).
 *
 * - nombre y correo: de la cuenta, sin espacios en los extremos; vacíos se vuelven null.
 *   El nombre ya NO se saca del correo: si falta, la pantalla dice NOMBRE_FALTANTE y el correo se ve aparte.
 * - tipoNegocio: la etiqueta de BusinessType; si el valor guardado no es un tipo conocido, null.
 * - nombreNegocio: del negocio; vacío se vuelve null.
 * - sin sesión o sin negocio: esos campos quedan en null.
 */
fun construirPerfil(usuario: UsuarioSesion?, negocio: NegocioEntity? = null): PerfilUsuario =
    PerfilUsuario(
        nombre = usuario?.nombre.limpio(),
        correo = usuario?.correo.limpio(),
        tipoNegocio = negocio?.tipo?.let(::etiquetaTipoNegocio),
        nombreNegocio = negocio?.nombre.limpio(),
    )

/** "COMPRAVENTA" -> "Compraventa". null si el valor no es un BusinessType. */
fun etiquetaTipoNegocio(tipo: String): String? =
    BusinessType.entries.firstOrNull { it.name == tipo.trim() }?.label

private fun String?.limpio(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
