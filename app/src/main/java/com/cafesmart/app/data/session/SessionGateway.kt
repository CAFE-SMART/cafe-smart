package com.cafesmart.app.data.session

import com.google.firebase.auth.FirebaseAuth

// Acceso a la sesión del usuario. Es una interfaz para que la lógica de Ajustes se pueda probar con
// JUnit sin Firebase (en las pruebas se usa una versión falsa).

/**
 * Datos de la cuenta con sesión iniciada (Firebase Auth). Cada campo puede venir vacío.
 * Los datos del negocio (nombre, tipo, teléfono) no van aquí: están en la entidad Negocio de Room.
 */
data class UsuarioSesion(
    // uid de Firebase; es el id del Negocio en Room.
    val uid: String?,
    // Nombres y apellidos (displayName de Firebase).
    val nombre: String?,
    val correo: String?,
)

interface SessionGateway {
    /** Usuario con sesión iniciada, o null si no hay sesión (por ejemplo, en modo demostración). */
    fun usuarioActual(): UsuarioSesion?

    /** Cierra la sesión del usuario. */
    fun cerrarSesion()
}

/** Versión real, sobre Firebase Authentication. */
object FirebaseSessionGateway : SessionGateway {
    override fun usuarioActual(): UsuarioSesion? =
        FirebaseAuth.getInstance().currentUser?.let {
            UsuarioSesion(uid = it.uid, nombre = it.displayName, correo = it.email)
        }

    override fun cerrarSesion() = FirebaseAuth.getInstance().signOut()
}
