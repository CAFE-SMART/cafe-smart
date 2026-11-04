package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.session.UsuarioSesion

// Reglas para armar el perfil que muestra Ajustes. Funciones puras, probadas con JUnit.

const val NOMBRE_POR_DEFECTO = "Usuario"

/** Lo que muestra la tarjeta de perfil: el nombre y, si se conoce, el tipo de negocio. */
data class PerfilUsuario(
    val nombre: String,
    // null = no se muestra la etiqueta del tipo de negocio.
    val tipoNegocio: String?,
)

/**
 * Arma el perfil a partir del usuario de la sesión.
 *
 * - nombre: el nombre del usuario; si está vacío, lo que va antes del "@" del correo;
 *   si tampoco hay correo, NOMBRE_POR_DEFECTO. Se quitan los espacios de los extremos.
 * - tipoNegocio: se muestra solo si no está vacío.
 * - sin sesión (usuario null): nombre por defecto y sin tipo de negocio.
 */
fun construirPerfil(usuario: UsuarioSesion?): PerfilUsuario {
    if (usuario == null) return PerfilUsuario(NOMBRE_POR_DEFECTO, null)
    val nombre = usuario.nombre?.trim().orEmpty().ifEmpty {
        usuario.correo?.trim().orEmpty().substringBefore("@").trim().ifEmpty { NOMBRE_POR_DEFECTO }
    }
    return PerfilUsuario(nombre, usuario.tipoNegocio?.trim()?.takeIf { it.isNotEmpty() })
}
