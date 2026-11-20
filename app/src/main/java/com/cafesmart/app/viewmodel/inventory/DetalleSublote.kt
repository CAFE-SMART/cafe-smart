package com.cafesmart.app.viewmodel.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.formatoKg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado de "Inventario · Detalle del sublote" (solo lectura). Los textos ya vienen formateados:
 * la pantalla solo los dibuja.
 * - [cargando]: true hasta que Room entrega la primera lectura (evita parpadear "no existe").
 * - [noEncontrado]: el id de la ruta no es un número o no hay un sublote con ese id.
 */
data class DetalleSubloteUiState(
    val cargando: Boolean = true,
    val noEncontrado: Boolean = false,
    val detalle: DetalleSublote? = null,
)

/** Los datos que muestra la pantalla, ya convertidos a texto. */
data class DetalleSublote(
    val id: Long,
    val codigo: String,        // "Sublote #12"
    val tipo: String,          // "Verde"
    val calidad: String,       // "Regular"
    val pesoInicial: String,   // "1.000 kg"
    val pesoActual: String,    // "120,5 kg"
    val pesoActualBultos: String, // "3,0 bultos"
    val precioPorKg: String,   // "$ 14.000"
    val valorEnBodega: String, // "$ 1.687.000"
    val fecha: String,         // "3 de junio de 2026"
    val agotado: Boolean,
)

/** Del modelo al texto de la pantalla. Función pura para probarla sin Room. */
fun armarDetalle(item: SubloteConLote, pesoBultoKg: Double = KG_POR_BULTO): DetalleSublote {
    val s = item.sublote
    return DetalleSublote(
        id = s.id,
        codigo = "Sublote #${s.id}",
        tipo = item.tipoCafe.etiqueta,
        calidad = item.calidad.etiqueta,
        pesoInicial = formatoKg(redondearKg(s.pesoInicialKg)),
        pesoActual = formatoKg(redondearKg(s.pesoDisponibleKg)),
        pesoActualBultos = formatoBultos(redondearKg(s.pesoDisponibleKg), pesoBultoKg),
        precioPorKg = formatoDinero(s.costoKg.toDouble()),
        valorEnBodega = formatoDinero(s.valorEnBodega.toDouble()),
        fecha = formatoFecha(s.fechaIngreso),
        agotado = s.estaAgotado,
    )
}

// Lee un sublote de Room a través del repositorio y se actualiza solo cuando cambia el inventario
// (por eso la pantalla no tiene botón de recargar). [batchId] es el texto que llega en la ruta.
class DetalleSubloteViewModel(
    inventario: InventoryRepository,
    batchId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleSubloteUiState())
    val uiState: StateFlow<DetalleSubloteUiState> = _uiState

    init {
        val id = batchId.toLongOrNull()
        if (id == null) {
            _uiState.value = DetalleSubloteUiState(cargando = false, noEncontrado = true)
        } else {
            viewModelScope.launch {
                inventario.observarSublote(id).collect { item ->
                    _uiState.value = if (item == null) {
                        DetalleSubloteUiState(cargando = false, noEncontrado = true)
                    } else {
                        DetalleSubloteUiState(cargando = false, detalle = armarDetalle(item))
                    }
                }
            }
        }
    }
}
