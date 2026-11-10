package com.cafesmart.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.session.SessionGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla Ajustes. */
data class SettingsUiState(
    val perfil: PerfilUsuario = PerfilUsuario(),
    // true mientras se muestra la hoja "¿Cerrar sesión?".
    val mostrarConfirmacion: Boolean = false,
)

// Guarda el estado en un StateFlow y recibe las acciones de la vista (igual que PurchaseViewModel).
// La cuenta sale de la sesión (Firebase) y el tipo y nombre del negocio, de Room.
class SettingsViewModel(
    sesion: SessionGateway,
    private val negocios: NegocioRepository,
) : ViewModel() {

    private val usuario = sesion.usuarioActual()

    private val _uiState = MutableStateFlow(SettingsUiState(perfil = construirPerfil(usuario)))
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        // Sin uid (sin sesión) no hay negocio que buscar.
        usuario?.uid?.let { uid ->
            viewModelScope.launch {
                negocios.observar(uid).collect { negocio ->
                    _uiState.update { it.copy(perfil = construirPerfil(usuario, negocio)) }
                }
            }
        }
    }

    /** El usuario tocó "Cerrar sesión": se abre la hoja de confirmación. */
    fun pedirCerrarSesion() = _uiState.update { it.copy(mostrarConfirmacion = true) }

    /** El usuario cerró la hoja sin confirmar. */
    fun cancelarCierre() = _uiState.update { it.copy(mostrarConfirmacion = false) }

    /**
     * El usuario confirmó: solo oculta la hoja. El signOut y la vuelta al login los hace
     * cerrarSesion() de AppNavHost, para que exista un único lugar que cierra la sesión.
     */
    fun confirmarCierre() = _uiState.update { it.copy(mostrarConfirmacion = false) }
}
