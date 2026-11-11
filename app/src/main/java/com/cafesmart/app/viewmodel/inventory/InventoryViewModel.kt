package com.cafesmart.app.viewmodel.inventory

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.model.LoteConSublotes
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.sample.CAPACIDAD_EJEMPLO_KG
import com.cafesmart.app.data.sample.lotesEjemplo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla Inventario: el resumen (ya ordenado) y la opción de orden elegida. */
data class InventoryUiState(
    val resumen: ResumenInventario,
    val orden: OrdenInventario = OrdenInventario.MAS_RECIENTE,
)

// Guarda el estado en un StateFlow y recibe las acciones de la vista (igual que SettingsViewModel).
// Los lotes aún son de ejemplo (luego, Room). Con [negocios] y [uid] la capacidad de la bodega y el peso del
// bulto salen del Negocio guardado y la pantalla se actualiza sola al cambiarlos en Ajustes; sin ellos
// (pruebas y vistas previas) se usa [capacidadKg] fija.
class InventoryViewModel(
    private val lotes: List<LoteConSublotes> = lotesEjemplo(),
    capacidadKg: Double = CAPACIDAD_EJEMPLO_KG,
    negocios: NegocioRepository? = null,
    uid: String? = null,
) : ViewModel() {

    private val conNegocio = negocios != null && uid != null
    // Sin configurar (0) hasta que Room entregue el negocio; ese estado es el de un usuario nuevo.
    private var capacidad = if (conNegocio) 0.0 else capacidadKg
    private var pesoBulto = KG_POR_BULTO

    private val _uiState = MutableStateFlow(
        InventoryUiState(resumen = resumirInventario(lotes, capacidad, OrdenInventario.MAS_RECIENTE, pesoBulto))
    )
    val uiState: StateFlow<InventoryUiState> = _uiState

    init {
        if (negocios != null && uid != null) {
            viewModelScope.launch {
                negocios.observar(uid).collect { negocio ->
                    capacidad = negocio?.capacidadBodegaKg ?: 0.0
                    pesoBulto = negocio?.pesoBultoKg ?: KG_POR_BULTO
                    _uiState.update { it.copy(resumen = resumirInventario(lotes, capacidad, it.orden, pesoBulto)) }
                }
            }
        }
    }

    /** El usuario eligió otra opción del botón "Más reciente": se reordena la lista. */
    fun cambiarOrden(orden: OrdenInventario) = _uiState.update {
        it.copy(orden = orden, resumen = resumirInventario(lotes, capacidad, orden, pesoBulto))
    }
}
