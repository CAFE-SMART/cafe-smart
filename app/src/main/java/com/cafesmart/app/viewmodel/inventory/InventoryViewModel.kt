package com.cafesmart.app.viewmodel.inventory

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.sample.CAPACIDAD_EJEMPLO_KG
import com.cafesmart.app.data.sample.lotesEjemplo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Estado de la pantalla Inventario: el resumen (ya ordenado) y la opción de orden elegida. */
data class InventoryUiState(
    val resumen: ResumenInventario,
    val orden: OrdenInventario = OrdenInventario.MAS_RECIENTE,
)

// Guarda el estado en un StateFlow y recibe las acciones de la vista (igual que SettingsViewModel).
// Por ahora lee datos de ejemplo; cuando exista Room se cambian estos dos parámetros por el repositorio.
class InventoryViewModel(
    private val lotes: List<LoteConSublotes> = lotesEjemplo(),
    private val capacidadKg: Double = CAPACIDAD_EJEMPLO_KG,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InventoryUiState(resumen = resumirInventario(lotes, capacidadKg, OrdenInventario.MAS_RECIENTE))
    )
    val uiState: StateFlow<InventoryUiState> = _uiState

    /** El usuario eligió otra opción del botón "Más reciente": se reordena la lista. */
    fun cambiarOrden(orden: OrdenInventario) = _uiState.update {
        it.copy(orden = orden, resumen = resumirInventario(lotes, capacidadKg, orden))
    }
}
