package com.cafesmart.app.viewmodel.purchase

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.purchase.Calidad
import com.cafesmart.app.data.purchase.Fecha
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.purchase.Productor
import com.cafesmart.app.data.purchase.Sublote
import com.cafesmart.app.data.purchase.TipoDocumento
import com.cafesmart.app.data.purchase.productorSinRegistrar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Un productor de la lista, tal como lo muestra la vista
data class ProductorEnPantalla(
    val productor: Productor,
    val nombre: String,
    val documento: String,
    val elegido: Boolean
)

// Una tarjeta "Café N", tal como la muestra la vista (textos ya listos)
data class CafeEnPantalla(
    val numero: Int,
    val tipoCafe: String?,
    val calidad: Calidad?,
    val peso: String,
    val precio: String,
    val errorPeso: String?,
    val errorPrecio: String?,
    val puedeQuitar: Boolean
)

// Estado del registro de compra.
// Los datos se guardan arriba; los valores con get() son lo que la vista muestra,
// ya calculados aqui (true/false y textos), para que la vista no tenga logica.
data class PurchaseUiState(
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
    // ---------- Paso 1 ----------

    val registradoSeleccionado: Boolean
        get() = modo == ModoProductor.REGISTRADO

    val sinRegistrarSeleccionado: Boolean
        get() = modo == ModoProductor.SIN_REGISTRAR

    val nuevoSeleccionado: Boolean
        get() = modo == ModoProductor.NUEVO

    val productoresEnPantalla: List<ProductorEnPantalla>
        get() = productoresVisibles(productores, busqueda, verTodos).map {
            ProductorEnPantalla(
                productor = it,
                nombre = it.nombre,
                documento = "${it.tipoDocumento.etiqueta} ${it.documento}",
                elegido = it == productorElegido
            )
        }

    val tituloLista: String
        get() = if (busqueda.isBlank()) "Recientes" else "Resultados"

    // Mensaje cuando no hay productores para mostrar (null si si hay)
    val mensajeListaVacia: String?
        get() = when {
            productoresEnPantalla.isNotEmpty() -> null
            busqueda.isBlank() -> "Aún no tienes productores registrados. Usa \"Productor nuevo\" para crear uno."
            else -> SIN_RESULTADOS
        }

    val hayMasProductores: Boolean
        get() = hayMasProductores(productores, busqueda, verTodos)

    val esEmpresa: Boolean
        get() = nuevoProductor.tipo == TipoDocumento.NIT

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

    // ---------- Paso 2 ----------

    val cafesEnPantalla: List<CafeEnPantalla>
        get() = cafes.mapIndexed { indice, cafe ->
            CafeEnPantalla(
                numero = indice + 1,
                tipoCafe = cafe.tipoCafe,
                calidad = cafe.calidad,
                peso = cafe.peso,
                precio = precioEnPantalla(cafe.precio),
                errorPeso = errorPeso(cafe),
                errorPrecio = errorPrecio(cafe),
                puedeQuitar = cafes.size > 1
            )
        }

    val totalKgTexto: String
        get() = formatoKg(totalKg(cafes))

    val totalEstimadoTexto: String
        get() = formatoDinero(totalEstimado(cafes))

    val fechaTexto: String
        get() = formatoFechaLarga(fecha, fechaDeHoy())

    // El calendario de Material trabaja en milisegundos
    val fechaMilis: Long
        get() = fechaAMilis(fecha)

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

// ViewModel (MVVM): aqui esta la logica del registro de compra.
// Guarda el estado en un StateFlow y recibe las acciones de la vista.
// No guarda ninguna referencia a la vista ni a la Activity: si Android destruye
// la pantalla (por ejemplo al girar el celular), el ViewModel sigue vivo con los datos
// y no queda apuntando a una pantalla que ya no existe.
class PurchaseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState

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

    fun cambiarTipoDocumento(tipo: TipoDocumento) {
        val nuevo = _uiState.value.nuevoProductor.copy(tipo = tipo)
        _uiState.value = _uiState.value.copy(nuevoProductor = nuevo)
    }

    // Solo numeros, sin puntos, comas ni espacios
    fun cambiarDocumento(texto: String) {
        val nuevo = _uiState.value.nuevoProductor.copy(documento = limpiarDocumento(texto))
        _uiState.value = _uiState.value.copy(nuevoProductor = nuevo)
    }

    // Solo letras y espacios
    fun cambiarNombre(texto: String) {
        val nuevo = _uiState.value.nuevoProductor.copy(nombre = limpiarNombre(texto))
        _uiState.value = _uiState.value.copy(nuevoProductor = nuevo)
    }

    fun cambiarTelefono(texto: String) {
        val nuevo = _uiState.value.nuevoProductor.copy(telefono = limpiarTelefono(texto))
        _uiState.value = _uiState.value.copy(nuevoProductor = nuevo)
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

    fun cambiarTipoCafe(indice: Int, tipo: String) {
        val cafe = _uiState.value.cafes[indice]
        reemplazarCafe(indice, cafe.copy(tipoCafe = tipo))
    }

    fun cambiarCalidad(indice: Int, calidad: Calidad) {
        val cafe = _uiState.value.cafes[indice]
        reemplazarCafe(indice, cafe.copy(calidad = calidad))
    }

    // Solo digitos y un decimal, maximo 3 decimales
    fun cambiarPeso(indice: Int, texto: String) {
        val cafe = _uiState.value.cafes[indice]
        reemplazarCafe(indice, cafe.copy(peso = limpiarPeso(texto)))
    }

    // Se guarda solo con digitos ("14000"); la vista recibe "14.000"
    fun cambiarPrecio(indice: Int, texto: String) {
        val cafe = _uiState.value.cafes[indice]
        reemplazarCafe(indice, cafe.copy(precio = limpiarPrecio(texto)))
    }

    fun quitarCafe(indice: Int) {
        val cafes = _uiState.value.cafes.filterIndexed { i, _ -> i != indice }
        _uiState.value = _uiState.value.copy(cafes = cafes)
    }

    fun agregarCafe() {
        _uiState.value = _uiState.value.copy(cafes = _uiState.value.cafes + CafeFormulario())
    }

    // El calendario entrega milisegundos; aqui se convierten a Fecha
    fun cambiarFecha(millis: Long) {
        _uiState.value = _uiState.value.copy(fecha = milisAFecha(millis))
    }

    // Reemplaza solo la tarjeta que cambio
    private fun reemplazarCafe(indice: Int, nuevo: CafeFormulario) {
        val cafes = _uiState.value.cafes.mapIndexed { i, actual -> if (i == indice) nuevo else actual }
        _uiState.value = _uiState.value.copy(cafes = cafes)
    }
}
