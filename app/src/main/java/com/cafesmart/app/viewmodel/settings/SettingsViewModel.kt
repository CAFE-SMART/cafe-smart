package com.cafesmart.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.TipoAviso
import com.cafesmart.app.data.session.SessionGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Hoja "Editar perfil" abierta: lo escrito, el error (si lo hay) y si se está guardando. */
data class EdicionPerfil(
    val nombre: String,
    val error: String? = null,
    val guardando: Boolean = false,
)

/** Estado de la pantalla Ajustes. */
data class SettingsUiState(
    val perfil: PerfilUsuario = PerfilUsuario(),
    // true mientras se muestra la hoja "¿Cerrar sesión?".
    val mostrarConfirmacion: Boolean = false,
    // null = la hoja "Editar perfil" está cerrada.
    val edicion: EdicionPerfil? = null,
    // Mensaje de éxito que se ve arriba un momento ("Nombre actualizado"); null = no hay aviso.
    val aviso: AvisoUi? = null,
)

/** Texto del aviso que se muestra al guardar el nombre. */
const val AVISO_NOMBRE_ACTUALIZADO = "Nombre actualizado"

/** Texto del aviso rojo cuando no se pudo guardar el nombre (el detalle sale en la hoja). */
const val AVISO_NOMBRE_ERROR = "No se pudo guardar el nombre"

// Guarda el estado en un StateFlow y recibe las acciones de la vista (igual que PurchaseViewModel).
// La cuenta sale de la sesión (Firebase) y el tipo y nombre del negocio, de Room.
class SettingsViewModel(
    private val sesion: SessionGateway,
    private val negocios: NegocioRepository,
) : ViewModel() {

    // var: al cambiar el nombre se actualiza aquí para que el perfil lo muestre sin volver a leer Firebase.
    private var usuario = sesion.usuarioActual()
    private var negocioActual: NegocioEntity? = null

    private val _uiState = MutableStateFlow(SettingsUiState(perfil = construirPerfil(usuario)))
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        // Sin uid (sin sesión) no hay negocio que buscar.
        usuario?.uid?.let { uid ->
            viewModelScope.launch {
                negocios.observar(uid).collect { negocio ->
                    negocioActual = negocio
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

    /** El usuario tocó la tarjeta de perfil: se abre la hoja con su nombre actual. Sin sesión no hay nada que editar. */
    fun abrirEditarPerfil() {
        if (usuario == null) return
        _uiState.update { it.copy(edicion = EdicionPerfil(nombre = it.perfil.nombre.orEmpty())) }
    }

    /** Cada letra escrita quita el error anterior. */
    fun cambiarNombre(valor: String) =
        _uiState.update { estado -> estado.copy(edicion = estado.edicion?.copy(nombre = valor, error = null)) }

    // Al cancelar también se borra un aviso rojo que quedara pendiente (no debe reaparecer arriba al cerrar la hoja).
    fun cancelarEdicion() = _uiState.update { it.copy(edicion = null, aviso = null) }

    /** Muestra un aviso que dejó otra pantalla (por ejemplo, "Datos del negocio guardados" al volver). */
    fun mostrarAviso(aviso: AvisoUi) = _uiState.update { it.copy(aviso = aviso) }

    /** El aviso ya se mostró: se borra. */
    fun ocultarAviso() = _uiState.update { it.copy(aviso = null) }

    /** Valida con las reglas del registro y, si todo está bien, guarda el nombre en Firebase. */
    fun guardarNombre() {
        val edicion = _uiState.value.edicion ?: return
        if (edicion.guardando) return
        val error = errorDeNombrePerfil(edicion.nombre)
        if (error != null) {
            _uiState.update { it.copy(edicion = edicion.copy(error = error)) }
            return
        }
        val nombre = normalizarNombre(edicion.nombre)
        _uiState.update { it.copy(edicion = edicion.copy(guardando = true, error = null)) }
        viewModelScope.launch {
            val fallo = sesion.actualizarNombre(nombre)
            if (fallo == null) {
                usuario = usuario?.copy(nombre = nombre)
                _uiState.update {
                    it.copy(perfil = construirPerfil(usuario, negocioActual), edicion = null, aviso = AvisoUi(AVISO_NOMBRE_ACTUALIZADO))
                }
            } else {
                _uiState.update {
                    it.copy(
                        edicion = it.edicion?.copy(guardando = false, error = fallo.mensaje),
                        aviso = AvisoUi(AVISO_NOMBRE_ERROR, TipoAviso.ERROR)
                    )
                }
            }
        }
    }
}
