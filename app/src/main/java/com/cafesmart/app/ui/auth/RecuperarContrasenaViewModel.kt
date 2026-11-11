package com.cafesmart.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.auth.AuthRepository
import com.cafesmart.app.data.auth.AuthResult
import com.cafesmart.app.data.auth.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Pantalla "Cuenta · Recuperar contraseña" (Figma, sección NUEVAS): se escribe el correo, se pide el
// enlace a Firebase y se muestra "Revisa tu correo". La validación es pura (sin Android) para probarla.

const val ERROR_CORREO_VACIO = "Escribe el correo de tu cuenta."
const val ERROR_CORREO_INVALIDO = "Ingresa un correo válido. Por ejemplo: nombre@correo.com"

private val correoRecuperacionRegex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/** Devuelve el mensaje de error del correo escrito, o null si el formato es válido. No comprueba que exista. */
fun errorDeCorreoParaRecuperar(correo: String): String? {
    val limpio = correo.trim()
    return when {
        limpio.isEmpty() -> ERROR_CORREO_VACIO
        !correoRecuperacionRegex.matches(limpio) -> ERROR_CORREO_INVALIDO
        else -> null
    }
}

/**
 * @property correo lo que está escrito en el campo.
 * @property error mensaje claro para la persona (validación o falla de Firebase), o null.
 * @property enviando true mientras se espera la respuesta de Firebase.
 * @property enviadoA correo al que se envió el enlace; si no es null se muestra "Revisa tu correo".
 */
data class RecuperarContrasenaUiState(
    val correo: String = "",
    val error: String? = null,
    val enviando: Boolean = false,
    val enviadoA: String? = null
)

class RecuperarContrasenaViewModel(
    private val repository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(RecuperarContrasenaUiState())
    val state: StateFlow<RecuperarContrasenaUiState> = _state.asStateFlow()

    /** Cada letra que se escribe quita el error anterior. */
    fun cambiarCorreo(valor: String) {
        _state.value = _state.value.copy(correo = valor, error = null)
    }

    /** Valida el correo y pide el enlace. Si el formato es malo no llama a Firebase. */
    fun enviar() {
        val actual = _state.value
        if (actual.enviando) return
        val error = errorDeCorreoParaRecuperar(actual.correo)
        if (error != null) {
            _state.value = actual.copy(error = error)
            return
        }
        pedirEnlace(actual.correo.trim())
    }

    /** Vuelve a pedir el enlace al mismo correo desde la pantalla de confirmación. */
    fun reenviar() {
        val correo = _state.value.enviadoA ?: return
        if (_state.value.enviando) return
        pedirEnlace(correo)
    }

    /** Deja la pantalla como nueva (al salir, para que no quede el correo escrito). */
    fun reiniciar() {
        _state.value = RecuperarContrasenaUiState()
    }

    private fun pedirEnlace(correo: String) {
        _state.value = _state.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            val resultado = repository.sendPasswordReset(correo)
            _state.value = when (resultado) {
                AuthResult.Success -> _state.value.copy(enviando = false, error = null, enviadoA = correo)
                is AuthResult.Failure -> _state.value.copy(enviando = false, error = resultado.error.mensaje)
            }
        }
    }
}
