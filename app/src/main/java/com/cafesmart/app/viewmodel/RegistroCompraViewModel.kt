package com.cafesmart.app.viewmodel

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.CafeFormulario
import com.cafesmart.app.data.Fecha
import com.cafesmart.app.data.ModoProductor
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.Productor
import com.cafesmart.app.data.Sublote
import com.cafesmart.app.data.productorSinRegistrar
import com.cafesmart.app.domain.cafesCompletos
import com.cafesmart.app.domain.crearProductor
import com.cafesmart.app.domain.crearSublote
import com.cafesmart.app.domain.errorPeso
import com.cafesmart.app.domain.errorPrecio
import com.cafesmart.app.domain.fechaDeHoy
import com.cafesmart.app.domain.hayMasProductores
import com.cafesmart.app.domain.limpiarDocumento
import com.cafesmart.app.domain.limpiarNombre
import com.cafesmart.app.domain.limpiarPeso
import com.cafesmart.app.domain.limpiarPrecio
import com.cafesmart.app.domain.limpiarTelefono
import com.cafesmart.app.domain.mensajeAgregarCafe
import com.cafesmart.app.domain.mensajePendientePaso1
import com.cafesmart.app.domain.mensajePendientePaso2
import com.cafesmart.app.domain.productoresVisibles
import com.cafesmart.app.domain.puedeContinuarPaso1
import com.cafesmart.app.domain.totalEstimado
import com.cafesmart.app.domain.totalKg
import com.cafesmart.app.domain.validarDocumento
import com.cafesmart.app.domain.validarNombre
import com.cafesmart.app.domain.validarTelefono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Todo lo que muestran las pantallas del registro de compra.
// Los valores calculados (get()) usan las reglas de la capa de dominio.
data class RegistroCompraUiState(
    val paso: Int = 1,
    // Paso 1: productor
    val modo: ModoProductor? = null,
    val busqueda: String = "",
    val verTodos: Boolean = false,
    // Lista en memoria hasta que se incorpore Room
    val productores: List<Productor> = emptyList(),
    val productorElegido: Productor? = null,
    val nuevoProductor: NuevoProductor = NuevoProductor(),
    // El productor con el que se hace la compra (sale del paso 1)
    val productorDeLaCompra: Productor? = null,
    // Paso 2: una tarjeta "Café N" por cada cafe, y la fecha
    val cafes: List<CafeFormulario> = listOf(CafeFormulario()),
    val fecha: Fecha = fechaDeHoy()
) {
    val productoresEnPantalla: List<Productor>
        get() = productoresVisibles(productores, busqueda, verTodos)

    val hayMasProductores: Boolean
        get() = hayMasProductores(productores, busqueda, verTodos)

    // El error solo se muestra cuando el campo ya tiene algo escrito
    val documentoError: String?
        get() = if (nuevoProductor.documento.isEmpty()) null else validarDocumento(nuevoProductor.documento, nuevoProductor.tipo, productores)

    val nombreError: String?
        get() = if (nuevoProductor.nombre.isEmpty()) null else validarNombre(nuevoProductor.nombre, nuevoProductor.tipo)

    val telefonoError: String?
        get() = validarTelefono(nuevoProductor.telefono)

    val continuarPaso1Habilitado: Boolean
        get() = puedeContinuarPaso1(modo, productorElegido, nuevoProductor, productores)

    // Lo que falta para poder continuar (se muestra sobre el boton)
    val mensajePendientePaso1: String?
        get() = mensajePendientePaso1(modo, productorElegido, nuevoProductor, productores)

    val erroresPeso: List<String?>
        get() = cafes.map { errorPeso(it) }

    val erroresPrecio: List<String?>
        get() = cafes.map { errorPrecio(it) }

    val totalKg: Double
        get() = totalKg(cafes)

    val totalEstimado: Double
        get() = totalEstimado(cafes)

    // Se puede agregar otro cafe o continuar solo si todas las tarjetas estan completas
    val cafesListos: Boolean
        get() = cafesCompletos(cafes)

    val mensajePendientePaso2: String?
        get() = mensajePendientePaso2(cafes)

    val mensajeAgregarCafe: String?
        get() = mensajeAgregarCafe(cafes)

    val sublotes: List<Sublote>
        get() = cafes.mapNotNull { crearSublote(it) }
}

// Capa de Aplicacion (MVVM): guarda el estado del registro de compra en un StateFlow
// y recibe las acciones de las pantallas. Como vive en un ViewModel, los datos
// no se pierden al girar el celular.
class RegistroCompraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroCompraUiState())
    val uiState: StateFlow<RegistroCompraUiState> = _uiState.asStateFlow()

    // ---------------- Paso 1 ----------------

    fun elegirModo(modo: ModoProductor) {
        _uiState.value = _uiState.value.copy(modo = modo)
    }

    fun cambiarBusqueda(texto: String) {
        _uiState.value = _uiState.value.copy(busqueda = texto)
    }

    fun verMasProductores() {
        _uiState.value = _uiState.value.copy(verTodos = true)
    }

    fun elegirProductor(productor: Productor) {
        _uiState.value = _uiState.value.copy(productorElegido = productor)
    }

    // Limpia lo escrito: solo numeros en el documento y el celular, solo letras en el nombre
    fun cambiarNuevoProductor(datos: NuevoProductor) {
        _uiState.value = _uiState.value.copy(
            nuevoProductor = datos.copy(
                documento = limpiarDocumento(datos.documento),
                nombre = limpiarNombre(datos.nombre),
                telefono = limpiarTelefono(datos.telefono)
            )
        )
    }

    fun continuarPaso1() {
        val estado = _uiState.value
        if (!estado.continuarPaso1Habilitado) return
        _uiState.value = when (estado.modo) {
            ModoProductor.REGISTRADO -> estado.copy(productorDeLaCompra = estado.productorElegido, paso = 2)
            ModoProductor.SIN_REGISTRAR -> estado.copy(productorDeLaCompra = productorSinRegistrar, paso = 2)
            ModoProductor.NUEVO -> {
                // Se guarda en la lista y queda elegido por si vuelve al paso 1
                val nuevo = crearProductor(estado.nuevoProductor)
                estado.copy(
                    productores = estado.productores + nuevo,
                    productorElegido = nuevo,
                    productorDeLaCompra = nuevo,
                    nuevoProductor = NuevoProductor(),
                    modo = ModoProductor.REGISTRADO,
                    paso = 2
                )
            }
            null -> estado
        }
    }

    // ---------------- Paso 2 ----------------

    fun volverAlPaso1() {
        _uiState.value = _uiState.value.copy(paso = 1)
    }

    // Reemplaza solo la tarjeta que cambio; el precio se guarda solo con digitos
    fun cambiarCafe(indice: Int, cafe: CafeFormulario) {
        val limpio = cafe.copy(peso = limpiarPeso(cafe.peso), precio = limpiarPrecio(cafe.precio))
        val cafes = _uiState.value.cafes.mapIndexed { i, actual -> if (i == indice) limpio else actual }
        _uiState.value = _uiState.value.copy(cafes = cafes)
    }

    fun quitarCafe(indice: Int) {
        val cafes = _uiState.value.cafes.filterIndexed { i, _ -> i != indice }
        _uiState.value = _uiState.value.copy(cafes = cafes)
    }

    fun agregarCafe() {
        _uiState.value = _uiState.value.copy(cafes = _uiState.value.cafes + CafeFormulario())
    }

    fun cambiarFecha(fecha: Fecha) {
        _uiState.value = _uiState.value.copy(fecha = fecha)
    }
}
