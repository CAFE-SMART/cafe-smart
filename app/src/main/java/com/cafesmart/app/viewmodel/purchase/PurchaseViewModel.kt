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
import com.cafesmart.app.data.purchase.productoresDeEjemplo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Un productor de la lista, tal como lo muestra la vista
// productor se conserva para devolverlo al ViewModel cuando se toca la fila.
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
    val puedeQuitar: Boolean,
    // true: se muestra el formulario completo; false: solo la tira con el resumen
    val abierto: Boolean,
    // Textos de la tira, por ejemplo "Café 1 · Verde" y "Buena · 25 kg × $ 14.000"
    val tituloTira: String,
    val detalleTira: String,
    // Vacio mientras el cafe este incompleto
    val totalTira: String
)

// Un cafe en el resumen del paso 3, tal como lo muestra la vista
data class CafeEnResumen(
    val tipoCafe: String,
    val titulo: String,
    val calidad: String,
    val detalle: String,
    val total: String
)

// Estado del registro de compra.
// Los datos se guardan arriba; los valores con get() son lo que la vista muestra,
// ya calculados aqui (true/false y textos), para que la vista no tenga logica.
data class PurchaseUiState(
    val paso: Int = 1,
    // true cuando se confirma la compra: se muestra la pantalla "Compra registrada"
    val compraRegistrada: Boolean = false,
    // Paso 1: productor
    val modo: ModoProductor? = null,
    val busqueda: String = "",
    // Lista en memoria hasta que se incorpore Room
    val productores: List<Productor> = productoresDeEjemplo,
    val productorElegido: Productor? = null,
    val nuevoProductor: NuevoProductor = NuevoProductor(),
    // Hoja "Productores": busqueda propia y el productor que se esta modificando
    val busquedaLista: String = "",
    val productorEnEdicion: Productor? = null,
    val datosEdicion: NuevoProductor = NuevoProductor(),
    // El productor con el que se hace la compra (sale del paso 1)
    val productorDeLaCompra: Productor? = null,
    // Paso 2: una tarjeta "Café N" por cada cafe, y la fecha
    val cafes: List<CafeFormulario> = listOf(CafeFormulario()),
    // Posicion del cafe que se esta editando; los demas se ven como tiras
    val cafeAbierto: Int = 0,
    val fecha: Fecha = fechaDeHoy()
) {
    // ---------- Paso 1 ----------

    val registradoSeleccionado: Boolean
        get() = modo == ModoProductor.REGISTRADO

    val sinRegistrarSeleccionado: Boolean
        get() = modo == ModoProductor.SIN_REGISTRAR

    // get() calcula el valor cada vez que se lee; no se guarda aparte.
    val productoresEnPantalla: List<ProductorEnPantalla>
        // map convierte cada Productor en una fila lista para mostrar.
        get() = productoresVisibles(productores, busqueda).map { aPantalla(it) }

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
        get() = hayMasProductores(productores, busqueda)

    val esEmpresa: Boolean
        get() = nuevoProductor.tipo == TipoDocumento.NIT

    // El error solo se muestra cuando el campo ya tiene algo escrito
    val documentoError: String?
        get() = if (nuevoProductor.documento.isEmpty()) null else validarDocumento(nuevoProductor.documento, nuevoProductor.tipo, productores)

    val nombreError: String?
        get() = if (nuevoProductor.nombre.isEmpty()) null else validarNombre(nuevoProductor.nombre, nuevoProductor.tipo)

    val telefonoError: String?
        get() = validarTelefono(nuevoProductor.telefono)

    val guardarNuevoHabilitado: Boolean
        get() = productorValido(nuevoProductor, productores)

    // ---------- Hoja "Productores" ----------

    val productoresEnLista: List<ProductorEnPantalla>
        get() = productoresDeLaLista(productores, busquedaLista).map { aPantalla(it) }

    val mensajeSinResultadosLista: String?
        get() = if (productoresEnLista.isEmpty()) SIN_RESULTADOS else null

    val edicionEsEmpresa: Boolean
        get() = datosEdicion.tipo == TipoDocumento.NIT

    // Al modificar, el documento del propio productor no cuenta como repetido
    private val productoresSinElEditado: List<Productor>
        get() = productores.filter { it != productorEnEdicion }

    val edicionDocumentoError: String?
        get() = validarDocumento(datosEdicion.documento, datosEdicion.tipo, productoresSinElEditado)

    val edicionNombreError: String?
        get() = validarNombre(datosEdicion.nombre, datosEdicion.tipo)

    val edicionTelefonoError: String?
        get() = validarTelefono(datosEdicion.telefono)

    val guardarEdicionHabilitado: Boolean
        get() = productorValido(datosEdicion, productoresSinElEditado)

    // Convierte un productor en la fila que muestra la vista
    private fun aPantalla(productor: Productor) = ProductorEnPantalla(
        productor = productor,
        nombre = productor.nombre,
        documento = "${productor.tipoDocumento.etiqueta} ${productor.documento}",
        elegido = productor == productorElegido
    )

    val continuarPaso1Habilitado: Boolean
        get() = puedeContinuarPaso1(modo, productorElegido)

    // Lo que falta para poder continuar (se muestra sobre el boton)
    val mensajePendientePaso1: String?
        get() = mensajePendientePaso1(modo, productorElegido)

    // ---------- Paso 2 ----------

    // mapIndexed es como map, pero tambien entrega la posicion de cada tarjeta.
    val cafesEnPantalla: List<CafeEnPantalla>
        get() = cafes.mapIndexed { indice, cafe ->
            // null si a este cafe todavia le faltan datos
            val sublote = crearSublote(cafe)
            CafeEnPantalla(
                numero = indice + 1,
                tipoCafe = cafe.tipoCafe,
                calidad = cafe.calidad,
                peso = cafe.peso,
                precio = precioEnPantalla(cafe.precio),
                errorPeso = errorPeso(cafe),
                errorPrecio = errorPrecio(cafe),
                // Solo se puede quitar una tarjeta si queda al menos otra.
                puedeQuitar = cafes.size > 1,
                abierto = indice == cafeAbierto,
                tituloTira = if (cafe.tipoCafe != null) "Café ${indice + 1} · ${cafe.tipoCafe}" else "Café ${indice + 1}",
                detalleTira = if (sublote != null) {
                    "${sublote.calidad.etiqueta} · ${formatoKg(sublote.pesoKg)} × ${formatoDinero(sublote.precioKg)}"
                } else {
                    "Sin completar"
                },
                totalTira = if (sublote != null) formatoDinero(totalDe(sublote)) else ""
            )
        }

    // true cuando ya hay algo escrito: los totales pasan de gris a azul
    val hayTotales: Boolean
        get() = totalKg(cafes) > 0

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

    // mapNotNull convierte las tarjetas en sublotes y descarta las incompletas (null).
    val sublotes: List<Sublote>
        get() = cafes.mapNotNull { crearSublote(it) }

    // ---------- Paso 3 y compra registrada ----------

    // orEmpty(): si aun no hay productor devuelve texto vacio en vez de null.
    val productorTexto: String
        get() = productorDeLaCompra?.nombre.orEmpty()

    val fechaResumenTexto: String
        get() = formatoFecha(fecha)

    val cafesEnResumen: List<CafeEnResumen>
        get() = sublotes.map {
            CafeEnResumen(
                tipoCafe = it.tipoCafe,
                titulo = "Café ${it.tipoCafe.lowercase()}",
                calidad = "Calidad ${it.calidad.etiqueta.lowercase()}",
                detalle = "${formatoKg(it.pesoKg)} × ${formatoDinero(it.precioKg)}",
                total = formatoDinero(totalDe(it))
            )
        }
}

// ViewModel (MVVM): aqui esta la logica del registro de compra.
// Guarda el estado en un StateFlow y recibe las acciones de la vista.
// No guarda ninguna referencia a la vista ni a la Activity: si Android destruye
// la pantalla (por ejemplo al girar el celular), el ViewModel sigue vivo con los datos
// y no queda apuntando a una pantalla que ya no existe.
// Hereda de ViewModel: Android lo mantiene vivo cuando se gira el celular.
class PurchaseViewModel : ViewModel() {

    // _uiState es privado: solo el ViewModel puede cambiar el estado.
    private val _uiState = MutableStateFlow(PurchaseUiState())
    // uiState es de solo lectura: la vista lo observa y se redibuja cuando cambia.
    val uiState: StateFlow<PurchaseUiState> = _uiState

    // ---------------- Paso 1 ----------------

    // copy() crea una copia del estado cambiando solo lo indicado.
    fun elegirModo(modo: ModoProductor) {
        _uiState.value = _uiState.value.copy(modo = modo)
    }

    fun cambiarBusqueda(texto: String) {
        _uiState.value = _uiState.value.copy(busqueda = texto)
    }

    fun elegirProductor(productor: Productor) {
        _uiState.value = _uiState.value.copy(productorElegido = productor)
    }

    fun cambiarTipoDocumento(tipo: TipoDocumento) {
        // Primero se copia el formulario y luego el estado con ese formulario.
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

    // "Guardar productor": lo agrega a la lista, lo deja elegido y activa Continuar
    fun guardarNuevoProductor() {
        val estado = _uiState.value
        if (!estado.guardarNuevoHabilitado) return
        val nuevo = crearProductor(estado.nuevoProductor)
        _uiState.value = estado.copy(
            productores = estado.productores + nuevo,
            productorElegido = nuevo,
            nuevoProductor = NuevoProductor(),
            modo = ModoProductor.REGISTRADO,
            busqueda = ""
        )
    }

    // "Cancelar": borra lo escrito; la opcion que estaba elegida no cambia
    fun cancelarNuevoProductor() {
        _uiState.value = _uiState.value.copy(nuevoProductor = NuevoProductor())
    }

    // ---------------- Hoja "Productores" ----------------

    fun cambiarBusquedaLista(texto: String) {
        _uiState.value = _uiState.value.copy(busquedaLista = texto)
    }

    // Al cerrar la hoja se limpia todo, para que la proxima vez abra desde cero
    fun cerrarLista() {
        _uiState.value = _uiState.value.copy(
            busquedaLista = "",
            productorEnEdicion = null
        )
    }

    // "Editar": carga los datos del productor en el formulario
    fun editarProductor(productor: Productor) {
        _uiState.value = _uiState.value.copy(
            productorEnEdicion = productor,
            datosEdicion = NuevoProductor(
                tipo = productor.tipoDocumento,
                documento = productor.documento,
                nombre = productor.nombre,
                telefono = productor.telefono
            )
        )
    }

    fun cambiarTipoEdicion(tipo: TipoDocumento) {
        val datos = _uiState.value.datosEdicion.copy(tipo = tipo)
        _uiState.value = _uiState.value.copy(datosEdicion = datos)
    }

    fun cambiarDocumentoEdicion(texto: String) {
        val datos = _uiState.value.datosEdicion.copy(documento = limpiarDocumento(texto))
        _uiState.value = _uiState.value.copy(datosEdicion = datos)
    }

    fun cambiarNombreEdicion(texto: String) {
        val datos = _uiState.value.datosEdicion.copy(nombre = limpiarNombre(texto))
        _uiState.value = _uiState.value.copy(datosEdicion = datos)
    }

    fun cambiarTelefonoEdicion(texto: String) {
        val datos = _uiState.value.datosEdicion.copy(telefono = limpiarTelefono(texto))
        _uiState.value = _uiState.value.copy(datosEdicion = datos)
    }

    // "Guardar cambios": reemplaza al productor en la lista y, si estaba elegido, tambien ahi
    fun guardarEdicion() {
        val estado = _uiState.value
        val original = estado.productorEnEdicion
        if (original == null || !estado.guardarEdicionHabilitado) return
        val editado = crearProductor(estado.datosEdicion)
        _uiState.value = estado.copy(
            productores = estado.productores.map { if (it == original) editado else it },
            productorElegido = if (estado.productorElegido == original) editado else estado.productorElegido,
            productorDeLaCompra = if (estado.productorDeLaCompra == original) editado else estado.productorDeLaCompra,
            productorEnEdicion = null
        )
    }

    // "Cancelar" en la edicion: vuelve a la lista sin guardar
    fun cancelarEdicion() {
        _uiState.value = _uiState.value.copy(productorEnEdicion = null)
    }

    fun continuarPaso1() {
        val estado = _uiState.value
        // Si faltan datos no avanza.
        if (!estado.continuarPaso1Habilitado) return
        // Segun la opcion elegida se define el productor y se pasa al paso 2.
        _uiState.value = when (estado.modo) {
            ModoProductor.REGISTRADO -> estado.copy(productorDeLaCompra = estado.productorElegido, paso = 2)
            ModoProductor.SIN_REGISTRAR -> estado.copy(productorDeLaCompra = productorSinRegistrar, paso = 2)
            // Sin opcion elegida el estado no cambia.
            null -> estado
        }
    }

    // ---------------- Paso 2 ----------------

    fun volverAlPaso1() {
        _uiState.value = _uiState.value.copy(paso = 1)
    }

    // Solo avanza si todas las tarjetas estan completas.
    fun continuarPaso2() {
        if (_uiState.value.cafesListos) {
            _uiState.value = _uiState.value.copy(paso = 3)
        }
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
        // filterIndexed deja todas las tarjetas menos la de esa posicion.
        val cafes = _uiState.value.cafes.filterIndexed { i, _ -> i != indice }
        val abierto = _uiState.value.cafeAbierto
        // Si se quita uno de arriba, el abierto baja una posicion;
        // si se quita el abierto, se abre el ultimo que quede
        val nuevoAbierto = when {
            indice < abierto -> abierto - 1
            indice == abierto -> cafes.lastIndex
            else -> abierto
        }
        _uiState.value = _uiState.value.copy(cafes = cafes, cafeAbierto = nuevoAbierto)
    }

    // "Editar" en una tira: abre ese cafe
    fun abrirCafe(indice: Int) {
        _uiState.value = _uiState.value.copy(cafeAbierto = indice)
    }

    fun agregarCafe() {
        val cafes = _uiState.value.cafes + CafeFormulario()
        // El cafe nuevo queda abierto y los anteriores pasan a ser tiras
        _uiState.value = _uiState.value.copy(cafes = cafes, cafeAbierto = cafes.lastIndex)
    }

    // El calendario entrega milisegundos; aqui se convierten a Fecha
    fun cambiarFecha(millis: Long) {
        _uiState.value = _uiState.value.copy(fecha = milisAFecha(millis))
    }

    // ---------------- Paso 3 ----------------

    fun volverAlPaso2() {
        _uiState.value = _uiState.value.copy(paso = 2)
    }

    // "Editar" en el paso 3: vuelve al paso 2 con ese cafe abierto
    fun editarCafe(indice: Int) {
        _uiState.value = _uiState.value.copy(paso = 2, cafeAbierto = indice)
    }

    // Por ahora solo cambia de pantalla; guardar la compra se conecta cuando exista Room
    fun confirmarCompra() {
        if (_uiState.value.cafesListos) {
            _uiState.value = _uiState.value.copy(compraRegistrada = true)
        }
    }

    // Empieza una compra desde cero, conservando los productores registrados
    fun reiniciarCompra() {
        // Un estado nuevo vuelve todo a sus valores iniciales, menos la lista de productores.
        _uiState.value = PurchaseUiState(productores = _uiState.value.productores)
    }

    // Reemplaza solo la tarjeta que cambio
    private fun reemplazarCafe(indice: Int, nuevo: CafeFormulario) {
        // Si la posicion coincide usa la tarjeta nueva; si no, deja la que estaba.
        val cafes = _uiState.value.cafes.mapIndexed { i, actual -> if (i == indice) nuevo else actual }
        _uiState.value = _uiState.value.copy(cafes = cafes)
    }
}
