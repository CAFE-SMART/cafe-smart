package com.cafesmart.app.data.session

import com.google.firebase.auth.FirebaseAuth

// Acceso a la sesión del usuario. Es una interfaz para que la lógica de Ajustes se pueda probar con
// JUnit sin Firebase (en las pruebas se usa una versión falsa).

/** Datos del usuario con sesión iniciada. Cada campo puede venir vacío. */
data class UsuarioSesion(
    val nombre: String?,
    val correo: String?,
    // Firebase Authentication no guarda el tipo de negocio (solo nombre y correo). Mientras no se
    // decida dónde guardarlo (Room, Firestore...), llega null y la pantalla no muestra la etiqueta.
    val tipoNegocio: String? = null,
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
        FirebaseAuth.getInstance().currentUser?.let { UsuarioSesion(nombre = it.displayName, correo = it.email) }

    override fun cerrarSesion() = FirebaseAuth.getInstance().signOut()
}
