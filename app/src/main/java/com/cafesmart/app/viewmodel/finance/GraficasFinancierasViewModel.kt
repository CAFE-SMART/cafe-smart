package com.cafesmart.app.viewmodel.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.finanzas.OrigenFinanciero
import com.cafesmart.app.data.model.Fecha
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// "Gráficas financieras" (ciclo 3, Amelia): ingresos, gastos y utilidad de los últimos meses. Vive DENTRO del Resumen
// financiero, que ya está protegido por la clave financiera: este ViewModel solo se crea después de ingresar la clave.
// Usa el mismo origen de datos que el Resumen (FinanzasLocal) y la lógica probada de SerieMensual.

const val MESES_GRAFICA = 6
const val ERROR_GRAFICAS = "No se pudieron cargar las gráficas. Inténtalo de nuevo."

data class GraficasUiState(
    val cargando: Boolean = true,
    val serie: List<MesFinanciero> = emptyList(),
    val error: String? = null,
) {
    /** Sin ventas ni gastos en los meses de la gráfica: se muestra el estado vacío en vez de barras en 0. */
    val vacia: Boolean get() = !cargando && error == null && serie.all { it.sinDatos }
}

class GraficasFinancierasViewModel(
    private val origen: OrigenFinanciero,
    private val hoy: () -> Fecha,
    private val meses: Int = MESES_GRAFICA,
) : ViewModel() {
    private val _estado = MutableStateFlow(GraficasUiState())
    val estado: StateFlow<GraficasUiState> = _estado

    init { cargar() }

    /** Vuelve a leer (el botón de recargar del Resumen también recarga las gráficas). */
    fun cargar() {
        _estado.update { it.copy(cargando = it.serie.isEmpty(), error = null) }
        viewModelScope.launch {
            try {
                val serie = serieMensual(origen.ventas(), origen.gastos(), hoy(), meses)
                _estado.value = GraficasUiState(cargando = false, serie = serie)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = ERROR_GRAFICAS) }
            }
        }
    }
}
