package com.cafesmart.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.session.SessionGateway
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.TipoAviso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Estado de "Información de empresa". */
data class NegocioInfoUiState(
    val form: NegocioFormulario = NegocioFormulario(),
    // Errores por campo; solo se muestran después de tocar "Guardar".
    val errores: Map<String, String> = emptyMap(),
    // true mientras se lee el negocio guardado (no se deja editar todavía).
    val cargando: Boolean = true,
    // true mientras se guarda (evita doble toque).
    val guardando: Boolean = false,
    // true cuando ya se guardó: la vista vuelve a Ajustes.
    val guardado: Boolean = false,
    // true si no hay sesión: no hay a quién asociar el negocio.
    val sinSesion: Boolean = false,
    // Aviso rojo arriba si no se pudo guardar; null = sin aviso. (El verde de "guardado" lo muestra Ajustes.)
    val aviso: AvisoUi? = null,
)

/** Texto del aviso rojo cuando no se pudo guardar el negocio. */
const val AVISO_NEGOCIO_ERROR = "No se pudo guardar. Inténtalo de nuevo."

// Completa o edita el nombre, el tipo y el teléfono del negocio de la cuenta (entidad Negocio en Room).
// Sirve también para las cuentas que quedaron sin Negocio (creadas antes de Room o tras reinstalar).
class NegocioInfoViewModel(
    sesion: SessionGateway,
    private val negocios: NegocioRepository,
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val uid: String? = sesion.usuarioActual()?.uid
    private var actual: NegocioEntity? = null

    private val _uiState = MutableStateFlow(NegocioInfoUiState(sinSesion = uid == null, cargando = uid != null))
    val uiState: StateFlow<NegocioInfoUiState> = _uiState

    init {
        uid?.let { id ->
            viewModelScope.launch {
                actual = negocios.observar(id).firstOrNull()
                _uiState.update { it.copy(form = formularioDesde(actual), cargando = false) }
            }
        }
    }

    /** Cambia el formulario. Si ya se mostraron errores, se recalculan para que desaparezcan al corregir. */
    fun editar(cambio: (NegocioFormulario) -> NegocioFormulario) = _uiState.update { estado ->
        val form = cambio(estado.form)
        estado.copy(form = form, errores = if (estado.errores.isEmpty()) emptyMap() else validarNegocio(form))
    }

    /** Valida y, si todo está bien, guarda en Room. */
    fun guardar() {
        val estado = _uiState.value
        val id = uid ?: return
        if (estado.cargando || estado.guardando) return
        val errores = validarNegocio(estado.form)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }
        _uiState.update { it.copy(errores = emptyMap(), guardando = true) }
        viewModelScope.launch {
            try {
                negocios.guardar(negocioAGuardar(id, estado.form, actual, reloj()))
                _uiState.update { it.copy(guardando = false, guardado = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(guardando = false, aviso = AvisoUi(AVISO_NEGOCIO_ERROR, TipoAviso.ERROR)) }
            }
        }
    }

    /** El aviso ya se mostró: se borra. */
    fun ocultarAviso() = _uiState.update { it.copy(aviso = null) }
}
