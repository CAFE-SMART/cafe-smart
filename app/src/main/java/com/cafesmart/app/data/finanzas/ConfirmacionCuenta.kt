package com.cafesmart.app.data.finanzas

import com.cafesmart.app.data.auth.AuthError
import com.cafesmart.app.data.auth.authErrorDesdeCodigo
import com.cafesmart.app.data.auth.await
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlin.coroutines.cancellation.CancellationException

// "Olvidé mi clave de métricas": se confirma la identidad con la cuenta, sin correo ni pregunta de seguridad.
// Con cuenta de correo se pide la contraseña de la cuenta; con cuenta de Google, volver a entrar con Google.

enum class ProveedorCuenta { CORREO, GOOGLE }

interface ConfirmacionCuenta {
    /** Con qué entró la persona a la app. */
    fun proveedor(): ProveedorCuenta

    /** Comprueba la contraseña de la cuenta. null = es correcta; si no, el error. */
    suspend fun confirmarConContrasena(contrasena: String): AuthError?

    /** Vuelve a autenticar con Google. null = confirmado; si no, el error. */
    suspend fun confirmarConGoogle(): AuthError?
}

/** Versión real, sobre Firebase (reautenticación del usuario actual). */
object FirebaseConfirmacionCuenta : ConfirmacionCuenta {

    override fun proveedor(): ProveedorCuenta {
        val proveedores = FirebaseAuth.getInstance().currentUser?.providerData.orEmpty().map { it.providerId }
        return if ("password" !in proveedores && "google.com" in proveedores) ProveedorCuenta.GOOGLE else ProveedorCuenta.CORREO
    }

    override suspend fun confirmarConContrasena(contrasena: String): AuthError? {
        val usuario = FirebaseAuth.getInstance().currentUser ?: return AuthError.DESCONOCIDO
        val correo = usuario.email ?: return AuthError.DESCONOCIDO
        return try {
            usuario.reauthenticate(EmailAuthProvider.getCredential(correo, contrasena)).await()
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

    // Pendiente: depende de "Iniciar sesión con Google" (otra tarea del ciclo 3). Mientras tanto avisa.
    override suspend fun confirmarConGoogle(): AuthError? = AuthError.GOOGLE_NO_DISPONIBLE
}
