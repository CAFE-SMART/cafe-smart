package com.cafesmart.app.data.session

import com.cafesmart.app.data.auth.AuthError
import com.cafesmart.app.data.auth.authErrorDesdeCodigo
import com.cafesmart.app.data.auth.await
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.coroutines.cancellation.CancellationException

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

    /** Cambia el nombre de la cuenta (displayName de Firebase). Devuelve null si salió bien, o el error. */
    suspend fun actualizarNombre(nombre: String): AuthError?
}

/** Versión real, sobre Firebase Authentication. */
object FirebaseSessionGateway : SessionGateway {
    override fun usuarioActual(): UsuarioSesion? =
        FirebaseAuth.getInstance().currentUser?.let {
            UsuarioSesion(uid = it.uid, nombre = it.displayName, correo = it.email)
        }

    override fun cerrarSesion() = FirebaseAuth.getInstance().signOut()

    override suspend fun actualizarNombre(nombre: String): AuthError? {
        val usuario = FirebaseAuth.getInstance().currentUser ?: return AuthError.DESCONOCIDO
        return try {
            val cambio = UserProfileChangeRequest.Builder().setDisplayName(nombre).build()
            usuario.updateProfile(cambio).await()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            authErrorDesdeCodigo(e.errorCode)
        } catch (e: FirebaseNetworkException) {
            AuthError.SIN_CONEXION
        } catch (e: Exception) {
            AuthError.DESCONOCIDO
        }
    }
}
