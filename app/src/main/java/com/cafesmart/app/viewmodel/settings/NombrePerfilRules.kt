package com.cafesmart.app.viewmodel.settings

// Reglas del nombre de la persona (Ajustes > Editar perfil). Son las mismas del registro:
// nombre y apellidos obligatorios. Funciones puras, probadas con JUnit.

/** Máximo de caracteres del nombre completo (el campo muestra "Máximo 70 caracteres"). */
const val NOMBRE_PERFIL_MAX = 70

const val ERROR_NOMBRE_VACIO = "Ingresa tu nombre."
const val ERROR_APELLIDOS_VACIOS = "Ingresa tus apellidos."
const val ERROR_NOMBRE_LARGO = "El nombre es muy largo. Máximo $NOMBRE_PERFIL_MAX caracteres."

/** Quita espacios de los extremos y deja un solo espacio entre palabras. */
fun normalizarNombre(nombre: String): String =
    nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

/**
 * Mensaje de error del nombre escrito, o null si es válido.
 * Con una sola palabra falta el apellido, igual que en el registro (que pide nombre y apellidos por separado).
 */
fun errorDeNombrePerfil(nombre: String): String? {
    val limpio = normalizarNombre(nombre)
    return when {
        limpio.isEmpty() -> ERROR_NOMBRE_VACIO
        limpio.split(" ").size < 2 -> ERROR_APELLIDOS_VACIOS
        limpio.length > NOMBRE_PERFIL_MAX -> ERROR_NOMBRE_LARGO
        else -> null
    }
}

/** Iniciales para el avatar: primera letra de las dos primeras palabras ("Juan Pérez" -> "JP"). null si no hay nombre. */
fun inicialesDe(nombre: String?): String? {
    val palabras = nombre?.let(::normalizarNombre)?.split(" ")?.filter { it.isNotEmpty() }.orEmpty()
    if (palabras.isEmpty()) return null
    return palabras.take(2).joinToString("") { it.first().uppercase() }
}
