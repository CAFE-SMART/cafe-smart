package com.cafesmart.app.viewmodel.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.finanzas.FinanzasRepository
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.finanzas.ResumenFinanciero
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.TipoAviso
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// Pantalla "Resumen financiero · Panel": lo que ganó el negocio en el periodo y de dónde sale.

const val ERROR_RESUMEN = "No se pudo cargar el resumen. Inténtalo de nuevo."
const val AVISO_RESUMEN_ACTUALIZADO = "Resumen actualizado"

/**
 * @property cargando true solo la primera vez, cuando todavía no hay nada que mostrar (se ve la ruedita grande).
 * @property actualizando true mientras se vuelve a leer (primera carga, cambio de periodo o botón recargar):
 *   el ícono de recargar gira y la pantalla sigue mostrando lo anterior.
 * @property aviso barra verde "Resumen actualizado" (o roja si falla) tras tocar recargar.
 */
data class ResumenUiState(
    val periodo: PeriodoFinanciero = PeriodoFinanciero.HOY,
    val resumen: ResumenFinanciero? = null,
    val cargando: Boolean = true,
    val actualizando: Boolean = true,
    val error: String? = null,
    val aviso: AvisoUi? = null,
)

class ResumenFinancieroViewModel(
    private val repositorio: FinanzasRepository,
    // Al tocar recargar, la ruedita se ve al menos este tiempo; si no, una lectura instantánea parecería que no pasó nada.
    private val pausaMinimaMs: Long = 600,
) : ViewModel() {

    private val _estado = MutableStateFlow(ResumenUiState())
    val estado: StateFlow<ResumenUiState> = _estado

    init {
        cargar(manual = false)
    }

    fun cambiarPeriodo(periodo: PeriodoFinanciero) {
        if (periodo == _estado.value.periodo) return
        _estado.update { it.copy(periodo = periodo) }
        cargar(manual = false)
    }

    /** Botón de recargar: vuelve a leer, gira el ícono y avisa con una barra verde (o roja si falla). */
    fun recargar() {
        if (_estado.value.actualizando) return
        cargar(manual = true)
    }

    fun ocultarAviso() = _estado.update { it.copy(aviso = null) }

    private fun cargar(manual: Boolean) {
        val periodo = _estado.value.periodo
        _estado.update { it.copy(actualizando = true, cargando = it.resumen == null, error = null) }
        viewModelScope.launch {
            try {
                val resumen = repositorio.resumen(periodo)
                if (manual) delay(pausaMinimaMs)
                // Si mientras tanto cambió el periodo, esta respuesta ya no sirve.
                _estado.update {
                    if (it.periodo != periodo) it
                    else it.copy(
                        resumen = resumen, cargando = false, actualizando = false,
                        aviso = if (manual) AvisoUi(AVISO_RESUMEN_ACTUALIZADO) else it.aviso
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (manual) delay(pausaMinimaMs)
                _estado.update {
                    it.copy(
                        cargando = false, actualizando = false,
                        error = if (it.resumen == null) ERROR_RESUMEN else null,
                        aviso = if (manual || it.resumen != null) AvisoUi(ERROR_RESUMEN, TipoAviso.ERROR) else null
                    )
                }
            }
        }
    }
}

/**
 * Frase corta que explica el resultado en palabras de todos los días (para una persona de 40 años):
 * ganó, perdió, quedó en cero o todavía no hay ventas.
 */
fun mensajeDeUtilidad(utilidad: Long, periodo: PeriodoFinanciero, ventas: Int): String = when {
    ventas == 0 -> "${periodo.etiqueta} todavía no registras ventas."
    utilidad > 0 -> "${periodo.etiqueta} ganas más de lo que gastas."
    utilidad < 0 -> "${periodo.etiqueta} gastas más de lo que ganas."
    else -> "${periodo.etiqueta} no ganas ni pierdes."
}
