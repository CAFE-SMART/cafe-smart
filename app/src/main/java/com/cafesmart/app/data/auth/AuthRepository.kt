package com.cafesmart.app.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/** Lo que el ViewModel necesita de la autenticación. Con una interfaz se puede probar sin Firebase. */
interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String, displayName: String): AuthResult
    fun signOut()
}

/** Implementación real con Firebase Authentication (correo y contraseña). */
class FirebaseAuthRepository : AuthRepository {

    // lazy: no toca Firebase hasta usarlo (así los tests que crean el ViewModel no fallan).
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override suspend fun signIn(email: String, password: String): AuthResult =
        intentar { auth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun register(email: String, password: String, displayName: String): AuthResult =
        intentar {
            val usuario = auth.createUserWithEmailAndPassword(email, password).await().user
            // El nombre es un detalle: si falla, la cuenta ya existe y el registro cuenta como exitoso.
            try {
                val perfil = UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                usuario?.updateProfile(perfil)?.await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }

    override fun signOut() = auth.signOut()

    private suspend fun intentar(bloque: suspend () -> Unit): AuthResult =
        try {
            bloque()
            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            AuthResult.Failure(authErrorDesdeCodigo(e.errorCode))
        } catch (e: FirebaseNetworkException) {
            AuthResult.Failure(AuthError.SIN_CONEXION)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.DESCONOCIDO)
        }
}

// Convierte un Task de Google en una función suspend, sin necesitar otra librería.
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
