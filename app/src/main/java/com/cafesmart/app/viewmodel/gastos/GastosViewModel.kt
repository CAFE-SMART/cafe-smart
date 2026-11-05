package com.cafesmart.app.viewmodel.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.gastos.DemoGastosRepository
import com.cafesmart.app.data.gastos.GastosRepository
import com.cafesmart.app.data.model.Sublote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Único propietario del borrador, filtros y resultados. Las dos pantallas reciben estado
 * y emiten acciones; no escriben datos. Sobrevive a recomposiciones y rotaciones, no al proceso.
 * [repository] permite verificar fallos sin conectar servicios externos.
 * [sublotes] solo debe contener inventario real; por defecto no se fabrican opciones.
 */
class GastosViewModel(
    private val repository: GastosRepository = DemoGastosRepository(),
    sublotes: List<Sublote> = emptyList()
) : ViewModel() {
    private val mutableState = MutableStateFlow(GastosUiState(sublotes = sublotes))
    val uiState: StateFlow<GastosUiState> = mutableState
    private var siguienteId = 1L
    private var idBorrador = siguienteId

    init {
        viewModelScope.launch {
            repository.gastos.collect { gastos -> mutableState.update { it.copy(gastos = gastos) } }
        }
    }

    /** Conserva el texto escrito; no borra caracteres inválidos para ocultar errores. */
    fun editar(cambio: (GastoFormulario) -> GastoFormulario) {
        if (uiState.value.resultado != ResultadoGasto.EDICION) return
        mutableState.update { it.copy(formulario = cambio(it.formulario), errores = GastoErrores()) }
    }

    fun filtrar(filtro: FiltroGasto) { mutableState.update { it.copy(filtro = filtro) } }

    /** Relee la fuente demo observable; no simula una sincronización remota inexistente. */
    fun actualizar() { mutableState.update { it.copy(gastos = repository.gastos.value) } }

    /** Confirma disponibilidad y reparto antes de mostrar la hoja, sin guardar todavía. */
    fun solicitarConfirmacion() {
        if (uiState.value.resultado != ResultadoGasto.EDICION) return
        val estado = uiState.value
        val errores = GastosValidation.validar(estado.formulario, estado.sublotes)
        mutableState.update { it.copy(errores = errores, resultado =
            if (errores.hayErrores) ResultadoGasto.EDICION else ResultadoGasto.CONFIRMACION) }
    }

    fun volverAEditar() {
        if (uiState.value.resultado != ResultadoGasto.GUARDANDO) {
            mutableState.update { it.copy(resultado = ResultadoGasto.EDICION) }
        }
    }

    /**
     * Bloquea el doble toque antes de lanzar la corrutina. El ID se mantiene al reintentar;
     * un error nunca pasa por EXITO. La cancelación de la sesión se propaga correctamente.
     */
    fun confirmar() {
        val estado = uiState.value
        if (estado.resultado !in listOf(ResultadoGasto.CONFIRMACION, ResultadoGasto.ERROR)) return
        mutableState.update { it.copy(resultado = ResultadoGasto.GUARDANDO) }
        viewModelScope.launch {
            try {
                val (gasto, reparto) = GastosValidation.convertir(estado.formulario, idBorrador, estado.sublotes)
                withContext(Dispatchers.IO) { repository.guardar(gasto, reparto) }
                mutableState.update { it.copy(resultado = ResultadoGasto.EXITO, filtro = FiltroGasto.TODOS) }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                mutableState.update { it.copy(resultado = ResultadoGasto.ERROR) }
            }
        }
    }

    /** Nuevo ID solo al descartar o terminar el borrador anterior. El listado permanece. */
    fun nuevoRegistro() {
        if (uiState.value.resultado == ResultadoGasto.GUARDANDO) return
        siguienteId = maxOf(siguienteId, uiState.value.gastos.maxOfOrNull { it.id } ?: 0L) + 1
        idBorrador = siguienteId
        mutableState.update { it.copy(formulario = GastoFormulario(), errores = GastoErrores(), resultado = ResultadoGasto.EDICION) }
    }
}
