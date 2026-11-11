package com.cafesmart.app.viewmodel.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.finanzas.FiltroMovimiento
import com.cafesmart.app.data.finanzas.FinanzasRepository
import com.cafesmart.app.data.finanzas.GrupoDia
import com.cafesmart.app.data.finanzas.MovimientoFinanciero
import com.cafesmart.app.data.finanzas.agruparPorDia
import com.cafesmart.app.data.finanzas.filtrarMovimientos
import com.cafesmart.app.data.model.Fecha
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// Pantalla "Finanzas · Historial financiero": compras, ventas y gastos juntos, con filtro por tipo.
// Se muestran de a 20; "Ver más movimientos" agrega otros 20.

const val ERROR_HISTORIAL = "No se pudo cargar el historial. Inténtalo de nuevo."
const val MOVIMIENTOS_POR_PAGINA = 20

/**
 * @property movimientos todos los movimientos, del más reciente al más antiguo.
 * @property limite cuántos se muestran hoy; crece de a [MOVIMIENTOS_POR_PAGINA] con "Ver más movimientos".
 * @property hoy día actual, para titular los grupos con "Hoy" y "Ayer".
 */
data class HistorialUiState(
    val hoy: Fecha,
    val filtro: FiltroMovimiento = FiltroMovimiento.TODOS,
    val movimientos: List<MovimientoFinanciero> = emptyList(),
    val limite: Int = MOVIMIENTOS_POR_PAGINA,
    val cargando: Boolean = true,
    val error: String? = null,
) {
    /** Los del tipo elegido. */
    val filtrados: List<MovimientoFinanciero> get() = filtrarMovimientos(movimientos, filtro)

    /** Los días que se ven ahora, con sus filas. */
    val grupos: List<GrupoDia> get() = agruparPorDia(filtrados.take(limite), hoy)

    /** true si quedan movimientos por mostrar. */
    val hayMas: Boolean get() = filtrados.size > limite
}

class HistorialFinancieroViewModel(
    private val repositorio: FinanzasRepository,
    hoy: () -> Fecha,
) : ViewModel() {

    private val _estado = MutableStateFlow(HistorialUiState(hoy = hoy()))
    val estado: StateFlow<HistorialUiState> = _estado

    init {
        cargar()
    }

    /** Cambiar el filtro vuelve a mostrar solo la primera página. */
    fun cambiarFiltro(filtro: FiltroMovimiento) {
        if (filtro == _estado.value.filtro) return
        _estado.update { it.copy(filtro = filtro, limite = MOVIMIENTOS_POR_PAGINA) }
    }

    fun verMas() = _estado.update { it.copy(limite = it.limite + MOVIMIENTOS_POR_PAGINA) }

    /** "Reintentar" tras un error. */
    fun reintentar() = cargar()

    private fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val movimientos = repositorio.movimientos()
                _estado.update { it.copy(movimientos = movimientos, cargando = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = ERROR_HISTORIAL) }
            }
        }
    }
}
