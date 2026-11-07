package com.cafesmart.app.viewmodel.sale

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPrecio
import com.cafesmart.app.viewmodel.purchase.precioKg
import com.cafesmart.app.viewmodel.purchase.precioValido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Estado del registro de venta.
// Los datos se guardan arriba; los valores con get() son lo que la vista muestra,
// ya calculados aqui, para que la vista no tenga logica.
data class SaleUiState(
    // null mientras no se elige a quien se le vende
    val cliente: Cliente? = null,
    val elegidos: List<SubloteElegido> = emptyList(),
    // Precio por kg de cada calidad, solo con digitos ("20000"); la vista lo muestra como "20.000"
    val precios: Map<Calidad, String> = emptyMap()
) {
    // Cada calidad que hay entre los sublotes elegidos pide su propio precio por kg.
    val calidadesElegidas: List<Calidad>
        get() = Calidad.entries.filter { calidad -> elegidos.any { it.calidad == calidad } }

    // orEmpty(): si todavia no se escribio el precio de esa calidad devuelve texto vacio.
    private fun textoPrecio(calidad: Calidad): String = precios[calidad].orEmpty()

    fun errorDePrecio(calidad: Calidad): String? = errorPrecio(textoPrecio(calidad))

    val totalKg: Double
        get() = elegidos.sumOf { it.cantidadKg }

    // Cada sublote vale su cantidad por el precio de su calidad (0 mientras no se escribe el precio).
    val totalARecibir: Long
        get() = elegidos.sumOf { subtotal(it.cantidadKg, precioKg(textoPrecio(it.calidad)) ?: 0.0) }

    val totalKgTexto: String
        get() = formatoKg(totalKg)

    val totalARecibirTexto: String
        get() = formatoDinero(totalARecibir.toDouble())

    // "Continuar" del paso 2: hay al menos un sublote y cada calidad elegida tiene un precio valido.
    val puedeContinuar: Boolean
        get() = elegidos.isNotEmpty() && calidadesElegidas.all { precioValido(textoPrecio(it)) }
}

// ViewModel (MVVM): aqui esta la logica del registro de venta.
// Guarda el estado en un StateFlow y recibe las acciones de la vista.
class SaleViewModel : ViewModel() {

    // _uiState es privado: solo el ViewModel puede cambiar el estado.
    private val _uiState = MutableStateFlow(SaleUiState())
    // uiState es de solo lectura: la vista lo observa y se redibuja cuando cambia.
    val uiState: StateFlow<SaleUiState> = _uiState

    fun elegirCliente(cliente: Cliente) {
        _uiState.value = _uiState.value.copy(cliente = cliente)
    }

    // Marca o desmarca un sublote. Al marcarlo se vende todo lo que tiene disponible.
    fun alternarSublote(sublote: SubloteConLote) {
        val elegidos = _uiState.value.elegidos
        val id = sublote.sublote.id
        val nuevos = if (elegidos.any { it.id == id }) {
            // filterNot deja todos los sublotes menos el desmarcado.
            elegidos.filterNot { it.id == id }
        } else {
            elegidos + SubloteElegido(sublote, sublote.sublote.pesoDisponibleKg)
        }
        _uiState.value = _uiState.value.copy(elegidos = nuevos)
    }

    // Cambia cuantos kg se venden de un sublote. Si la cantidad no es valida no cambia nada.
    fun cambiarCantidad(subloteId: Long, cantidadKg: Double) {
        val nuevos = _uiState.value.elegidos.map {
            if (it.id == subloteId && cantidadValida(cantidadKg, it.disponibleKg)) it.copy(cantidadKg = cantidadKg) else it
        }
        _uiState.value = _uiState.value.copy(elegidos = nuevos)
    }

    // Se guarda solo con digitos ("20000"); la vista recibe "20.000"
    fun cambiarPrecio(calidad: Calidad, texto: String) {
        val precios = _uiState.value.precios + (calidad to limpiarPrecio(texto))
        _uiState.value = _uiState.value.copy(precios = precios)
    }
}
