package com.cafesmart.app.viewmodel.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.finanzas.OrigenFinanciero
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.model.Fecha
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Gráficas del Resumen financiero (ciclo 3, Amelia): distribución de gastos del periodo y balance semanal.
// Vive DENTRO del Resumen financiero, protegido por la clave financiera: solo se crea después de ingresar la clave.
// Sigue el mismo periodo que el Resumen (Hoy / Esta semana) y usa el mismo origen de datos (OrigenFinanciero).

const val ERROR_GRAFICAS = "No se pudieron cargar las gráficas. Inténtalo de nuevo."

/**
 * @property balance los 7 días de "Esta semana"; null con "Hoy" (la tarjeta no se muestra) o si las ventas no están
 *   conectadas (ver [ventasConectadas]).
 */
data class GraficasUiState(
    val periodo: PeriodoFinanciero = PeriodoFinanciero.HOY,
    val cargando: Boolean = true,
    val distribucion: DistribucionGastos? = null,
    val balance: List<DiaBalance>? = null,
    val ventasConectadas: Boolean = true,
    val error: String? = null,
    // Los 7 días de "Esta semana" (para el título "Sáb 21 – Vie 27"), aunque no haya ventas.
    val semana: List<Fecha> = emptyList(),
) {
    /** La tarjeta "Balance semanal" existe solo en "Esta semana". */
    val muestraBalance: Boolean get() = periodo == PeriodoFinanciero.SEMANA
}

class GraficasFinancierasViewModel(
    private val origen: OrigenFinanciero,
    private val hoy: () -> Fecha,
) : ViewModel() {
    private val _estado = MutableStateFlow(GraficasUiState())
    val estado: StateFlow<GraficasUiState> = _estado

    init { cargar() }

    /** Mismo periodo que el Resumen. Se limpian los datos para no mostrar los del periodo anterior. */
    fun cambiarPeriodo(periodo: PeriodoFinanciero) {
        if (periodo == _estado.value.periodo) return
        _estado.value = GraficasUiState(periodo = periodo)
        cargar()
    }

    /** Vuelve a leer (el botón de recargar del Resumen también recarga las gráficas). */
    fun cargar() {
        val periodo = _estado.value.periodo
        _estado.update { it.copy(cargando = it.distribucion == null, error = null) }
        viewModelScope.launch {
            try {
                val fecha = hoy()
                val distribucion = distribucionGastos(origen.gastos(), periodo, fecha)
                val conectadas = origen.ventasConectadas
                val balance = if (periodo == PeriodoFinanciero.SEMANA && conectadas) balanceSemanal(origen.ventas(), fecha) else null
                // Si mientras tanto cambió el periodo, esta respuesta ya no sirve.
                _estado.update {
                    if (it.periodo != periodo) it
                    else GraficasUiState(periodo, false, distribucion, balance, conectadas, semana = diasDeLaSemana(fecha))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { if (it.periodo != periodo) it else it.copy(cargando = false, error = ERROR_GRAFICAS) }
            }
        }
    }
}
