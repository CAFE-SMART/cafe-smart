package com.cafesmart.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.inventario.OrigenOcupacion
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

// Pantalla "Ajustes · Capacidad de bodega": cuántos kilos caben y cuánto pesa un bulto (entidad Negocio en Room).

const val AVISO_CAPACIDAD_ERROR = "No se pudo guardar. Inténtalo de nuevo."
const val AVISO_CAPACIDAD_SIN_NEGOCIO = "Primero completa la información de tu empresa en Ajustes."

/**
 * @property kgOcupados kilos de café que hay hoy en la bodega.
 * @property errores por campo; solo se muestran después de tocar "Guardar cambios".
 * @property guardado true cuando ya se guardó: la vista vuelve a donde estaba.
 */
data class CapacidadUiState(
    val form: CapacidadFormulario = CapacidadFormulario(),
    val kgOcupados: Double = 0.0,
    val errores: Map<String, String> = emptyMap(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val sinSesion: Boolean = false,
    val aviso: AvisoUi? = null,
) {
    /** Capacidad escrita (kg) si es un número válido y mayor a 0; con ella se calcula "Disponible". */
    val capacidadKg: Double? get() = kgDesdeTexto(form.capacidad)?.takeIf { it > 0 }

    /** Peso de un bulto escrito, o null si todavía no es un número válido. */
    val pesoBultoKg: Double? get() = kgDesdeTexto(form.pesoBulto)?.takeIf { it > 0 }
}

class CapacidadBodegaViewModel(
    sesion: SessionGateway,
    private val negocios: NegocioRepository,
    private val ocupacion: OrigenOcupacion,
) : ViewModel() {

    private val uid: String? = sesion.usuarioActual()?.uid
    private var actual: NegocioEntity? = null

    private val _uiState = MutableStateFlow(CapacidadUiState(sinSesion = uid == null, cargando = uid != null))
    val uiState: StateFlow<CapacidadUiState> = _uiState

    init {
        uid?.let { id ->
            viewModelScope.launch {
                actual = negocios.observar(id).firstOrNull()
                val kg = ocupacion.kgOcupados()
                val negocio = actual
                _uiState.update {
                    it.copy(
                        kgOcupados = kg,
                        // Capacidad 0 = sin configurar: el campo queda vacío para que la persona la escriba.
                        form = CapacidadFormulario(
                            capacidad = negocio?.capacidadBodegaKg?.takeIf { c -> c > 0 }?.let(::textoDeKg) ?: "",
                            pesoBulto = textoDeKg(negocio?.pesoBultoKg ?: com.cafesmart.app.viewmodel.inventory.KG_POR_BULTO),
                        ),
                        cargando = false,
                    )
                }
            }
        }
    }

    /** Cambia el formulario; si ya se mostraron errores, se recalculan para que desaparezcan al corregir. */
    fun editar(cambio: (CapacidadFormulario) -> CapacidadFormulario) = _uiState.update { estado ->
        val form = cambio(estado.form)
        estado.copy(
            form = form,
            errores = if (estado.errores.isEmpty()) emptyMap() else validarCapacidadBodega(form, estado.kgOcupados),
        )
    }

    /** Valida y, si todo está bien, guarda en Room. */
    fun guardar() {
        val estado = _uiState.value
        if (uid == null || estado.cargando || estado.guardando) return
        val errores = validarCapacidadBodega(estado.form, estado.kgOcupados)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }
        val negocio = actual
        if (negocio == null) {
            _uiState.update { it.copy(aviso = AvisoUi(AVISO_CAPACIDAD_SIN_NEGOCIO, TipoAviso.ERROR)) }
            return
        }
        _uiState.update { it.copy(errores = emptyMap(), guardando = true) }
        viewModelScope.launch {
            try {
                negocios.guardar(negocioConCapacidad(negocio, estado.form))
                _uiState.update { it.copy(guardando = false, guardado = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(guardando = false, aviso = AvisoUi(AVISO_CAPACIDAD_ERROR, TipoAviso.ERROR)) }
            }
        }
    }

    fun ocultarAviso() = _uiState.update { it.copy(aviso = null) }
}
