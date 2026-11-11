package com.cafesmart.app.viewmodel.sale

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
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
    val precios: Map<Calidad, String> = emptyMap(),
    // Lo que se escribio en el campo "Cantidad a vender" de cada sublote (por id). Se guarda aunque sea invalido,
    // para que la vista muestre lo escrito y su error; sin texto, el campo muestra la cantidad elegida.
    val textosCantidad: Map<Long, String> = emptyMap()
) {
    // Cada calidad que hay entre los sublotes elegidos pide su propio precio por kg.
    val calidadesElegidas: List<Calidad>
        get() = Calidad.entries.filter { calidad -> elegidos.any { it.calidad == calidad } }

    // orEmpty(): si todavia no se escribio el precio de esa calidad devuelve texto vacio.
    private fun textoPrecio(calidad: Calidad): String = precios[calidad].orEmpty()

    fun errorDePrecio(calidad: Calidad): String? = errorPrecio(textoPrecio(calidad))

    // Texto del campo "Cantidad a vender" de un sublote: lo escrito, o la cantidad elegida si no se ha tocado.
    fun textoCantidad(subloteId: Long): String =
        textosCantidad[subloteId] ?: elegidos.firstOrNull { it.id == subloteId }?.cantidadKg?.let(::formatoKgCampo).orEmpty()

    // Error de la cantidad escrita: solo aparece cuando pasa de lo disponible (con el campo vacio o en 0 no hay error).
    fun errorDeCantidad(subloteId: Long): String? {
        val elegido = elegidos.firstOrNull { it.id == subloteId } ?: return null
        val escrito = textosCantidad[subloteId] ?: return null
        return errorCantidad(cantidadDesdeTexto(escrito), elegido.disponibleKg)
    }

    // Cada cantidad escrita debe ser valida (mayor a 0 y sin pasar de lo disponible); lo no escrito ya es valido.
    private val cantidadesValidas: Boolean
        get() = elegidos.all { elegido ->
            textosCantidad[elegido.id]?.let { cantidadValida(cantidadDesdeTexto(it), elegido.disponibleKg) } ?: true
        }

    val totalKg: Double
        get() = elegidos.sumOf { it.cantidadKg }

    // Cada sublote vale su cantidad por el precio de su calidad (0 mientras no se escribe el precio).
    val totalARecibir: Long
        get() = elegidos.sumOf { subtotal(it.cantidadKg, precioKg(textoPrecio(it.calidad)) ?: 0.0) }

    val totalKgTexto: String
        get() = formatoKg(totalKg)

    val totalARecibirTexto: String
        get() = formatoDinero(totalARecibir.toDouble())

    // "Continuar" del paso 2: hay al menos un sublote, cada cantidad escrita es valida y cada calidad elegida
    // tiene un precio valido.
    val puedeContinuar: Boolean
        get() = elegidos.isNotEmpty() && cantidadesValidas && calidadesElegidas.all { precioValido(textoPrecio(it)) }
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
        // Al marcar o desmarcar, el campo vuelve a mostrar la cantidad elegida (se borra lo escrito antes).
        _uiState.value = _uiState.value.copy(elegidos = nuevos, textosCantidad = _uiState.value.textosCantidad - id)
    }

    // Lo que la persona escribe en "Cantidad a vender". Se guarda el texto tal cual (limpiado como el peso de
    // Compras) para mostrarlo con su error; la cantidad elegida solo cambia cuando el valor es valido.
    fun escribirCantidad(subloteId: Long, texto: String) {
        val limpio = limpiarPeso(texto)
        val cantidad = cantidadDesdeTexto(limpio)
        val nuevos = _uiState.value.elegidos.map {
            if (it.id == subloteId && cantidadValida(cantidad, it.disponibleKg)) it.copy(cantidadKg = cantidad!!) else it
        }
        _uiState.value = _uiState.value.copy(
            elegidos = nuevos,
            textosCantidad = _uiState.value.textosCantidad + (subloteId to limpio)
        )
    }

    // Cambia cuantos kg se venden de un sublote. Si la cantidad no es valida no cambia nada.
    fun cambiarCantidad(subloteId: Long, cantidadKg: Double) {
        val nuevos = _uiState.value.elegidos.map {
            if (it.id == subloteId && cantidadValida(cantidadKg, it.disponibleKg)) it.copy(cantidadKg = cantidadKg) else it
        }
        // Un cambio por codigo (no por el campo) deja la cantidad elegida como texto del campo.
        _uiState.value = _uiState.value.copy(elegidos = nuevos, textosCantidad = _uiState.value.textosCantidad - subloteId)
    }

    // Empieza una venta desde cero (al cancelarla o despues de registrarla): sin cliente, sublotes ni precios.
    fun reiniciarVenta() {
        _uiState.value = SaleUiState()
    }

    // Se guarda solo con digitos ("20000"); la vista recibe "20.000"
    fun cambiarPrecio(calidad: Calidad, texto: String) {
        val precios = _uiState.value.precios + (calidad to limpiarPrecio(texto))
        _uiState.value = _uiState.value.copy(precios = precios)
    }
}

// Cantidad como se escribe en el campo: sin ".0" si es entera ("150") y con punto decimal si no ("25.5").
private fun formatoKgCampo(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()
