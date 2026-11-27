package com.cafesmart.app.viewmodel.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.sample.CAPACIDAD_EJEMPLO_KG
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// Mismo estilo que ERROR_RESUMEN y ERROR_HISTORIAL (Finanzas). Texto pendiente de confirmar con el equipo.
const val ERROR_INVENTARIO = "No se pudo cargar el inventario. Inténtalo de nuevo."

/**
 * Estado de Inventario, compartido por las tres vistas.
 * - [resumen]: lo que muestra "Resumen (Todos)" (ya ordenado según [orden]).
 * - [lotes]: los lotes con sus sublotes tal como están en Room; de aquí salen "Lista por tipo" y "Sublotes del grupo".
 * - [cargando]: true hasta que Room entrega la primera lectura. Mientras tanto la lista está vacía y la pantalla
 *   no debe mostrar el estado vacío (sería un parpadeo falso de "no tienes café").
 * - [error]: no null si Room falló o entregó un dato que no se pudo leer. La pantalla muestra el mensaje y
 *   "Reintentar" en vez de un inventario incompleto (y en vez de cerrar la app).
 */
data class InventoryUiState(
    val resumen: ResumenInventario,
    val orden: OrdenInventario = OrdenInventario.MAS_RECIENTE,
    val lotes: List<LoteConSublotes> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
) {
    /** Filas de "Lista por tipo": las calidades de [tipo] con su peso y sublotes. */
    fun calidadesDe(tipo: TipoCafe): List<ResumenCalidad> = resumirCalidades(lotes, tipo, orden)

    /** Filas de "Sublotes del grupo": los sublotes con café del lote [loteId]. */
    fun sublotesDe(loteId: Long): List<SubloteConLote> = sublotesDelLote(lotes, loteId, orden)
}

// Guarda el estado en un StateFlow y recibe las acciones de la vista (igual que SettingsViewModel).
// Los lotes y sublotes salen de Room a través de [inventario]: cuando Compras (o Ventas o Secado) guarda algo, la
// pantalla se actualiza sola, sin reiniciar la app. Con [negocios] y [uid] la capacidad de la bodega y el peso
// del bulto salen del Negocio guardado y también se actualizan solos al cambiarlos en Ajustes; sin ellos (pruebas
// y vistas previas) se usa [capacidadKg] fija.
class InventoryViewModel(
    private val inventario: InventoryRepository,
    capacidadKg: Double = CAPACIDAD_EJEMPLO_KG,
    private val negocios: NegocioRepository? = null,
    private val uid: String? = null,
) : ViewModel() {

    private val conNegocio = negocios != null && uid != null
    // Sin configurar (0) hasta que Room entregue el negocio; ese estado es el de un usuario nuevo.
    private var capacidad = if (conNegocio) 0.0 else capacidadKg
    private var pesoBulto = KG_POR_BULTO
    // Último inventario leído de Room. Las dos lecturas (inventario y negocio) y el cambio de orden llegan al
    // hilo principal, así que estas variables no se pisan.
    private var lotes: List<LoteConSublotes> = emptyList()

    private val _uiState = MutableStateFlow(
        InventoryUiState(
            resumen = resumirInventario(lotes, capacidad, OrdenInventario.MAS_RECIENTE, pesoBulto),
            cargando = true,
        )
    )
    val uiState: StateFlow<InventoryUiState> = _uiState

    // Las dos lecturas en curso. Si una falla se cancelan ambas; "Reintentar" las vuelve a abrir.
    private var lecturas: List<Job> = emptyList()

    init {
        observar()
    }

    /** Botón "Reintentar" del estado de error: vuelve a leer Room desde cero. */
    fun reintentar() {
        if (_uiState.value.error == null) return
        _uiState.update { it.copy(error = null, cargando = true) }
        observar()
    }

    /** El usuario eligió otra opción del botón "Más reciente": se reordena la lista. */
    fun cambiarOrden(orden: OrdenInventario) = recalcular(orden = orden)

    // Cualquier excepción de Room, del paso de entidad a modelo (enum desconocido) o de las validaciones de
    // Sublote (dato fuera de rango) termina en el estado de error. Sin esto, en viewModelScope cerraría la app.
    private fun observar() {
        lecturas.forEach { it.cancel() }
        val inventarioJob = viewModelScope.launch {
            protegido {
                inventario.observarLotes().collect { nuevos ->
                    lotes = nuevos
                    // Con la primera lectura (aunque venga vacía) ya se sabe si hay café o no.
                    recalcular(cargando = false)
                }
            }
        }
        val negocioJob = if (negocios != null && uid != null) {
            viewModelScope.launch {
                protegido {
                    negocios.observar(uid).collect { negocio ->
                        capacidad = negocio?.capacidadBodegaKg ?: 0.0
                        pesoBulto = negocio?.pesoBultoKg ?: KG_POR_BULTO
                        recalcular()
                    }
                }
            }
        } else null
        lecturas = listOfNotNull(inventarioJob, negocioJob)
    }

    private suspend fun protegido(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mostrarError()
        }
    }

    private fun mostrarError() {
        lecturas.forEach { it.cancel() }
        _uiState.update { it.copy(cargando = false, error = ERROR_INVENTARIO) }
    }

    // Vuelve a calcular el resumen con lo último que se sabe. Los parámetros que no se pasan se quedan como estaban.
    private fun recalcular(
        orden: OrdenInventario? = null,
        cargando: Boolean? = null,
    ) = _uiState.update {
        val ordenFinal = orden ?: it.orden
        it.copy(
            resumen = resumirInventario(lotes, capacidad, ordenFinal, pesoBulto),
            orden = ordenFinal,
            lotes = lotes,
            cargando = cargando ?: it.cargando,
        )
    }
}
