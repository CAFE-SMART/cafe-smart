package com.cafesmart.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.session.FirebaseSessionGateway
import com.cafesmart.app.data.session.SessionGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Estado de la pantalla Ajustes. */
data class SettingsUiState(
    val perfil: PerfilUsuario = PerfilUsuario(NOMBRE_POR_DEFECTO, null),
    // true mientras se muestra la hoja "¿Cerrar sesión?".
    val mostrarConfirmacion: Boolean = false,
)

// Guarda el estado en un StateFlow y recibe las acciones de la vista (igual que PurchaseViewModel).
class SettingsViewModel(
    private val sesion: SessionGateway = FirebaseSessionGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(perfil = construirPerfil(sesion.usuarioActual())))
    val uiState: StateFlow<SettingsUiState> = _uiState

    /** Vuelve a leer el usuario de la sesión (por ejemplo, tras editar el perfil). */
    fun actualizarPerfil() = _uiState.update { it.copy(perfil = construirPerfil(sesion.usuarioActual())) }

    /** El usuario tocó "Cerrar sesión": se abre la hoja de confirmación. */
    fun pedirCerrarSesion() = _uiState.update { it.copy(mostrarConfirmacion = true) }

    /** El usuario cerró la hoja sin confirmar. */
    fun cancelarCierre() = _uiState.update { it.copy(mostrarConfirmacion = false) }

    /** El usuario confirmó: cierra la sesión y oculta la hoja. La navegación al login la hace la vista. */
    fun confirmarCierre() {
        sesion.cerrarSesion()
        _uiState.update { it.copy(mostrarConfirmacion = false) }
    }
}
