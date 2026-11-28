package com.cafesmart.app.viewmodel.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.inventario.OrigenOcupacion
import com.cafesmart.app.data.purchase.PurchaseRepository
import com.cafesmart.app.data.purchase.ResultadoProductor
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.productorSinRegistrar
import com.cafesmart.app.data.borradores.BorradorDeOperacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    // Copia en memoria de los productores guardados en Room (se carga con cargarProductores)
    val productores: List<Productor> = emptyList(),
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
    val fecha: Fecha = fechaDeHoy(),
    // Bodega: capacidad configurada (0 = sin configurar) y kilos que hay hoy; los avisos salen de aqui.
    val capacidadKg: Double = 0.0,
    val kgOcupados: Double = 0.0,
    // Lo que dejo la compra en la bodega, fijado al confirmarla (para "Compra registrada").
    val avisoAlRegistrar: AvisoCapacidad? = null,
    // true mientras se guarda la compra en Room (el boton Confirmar se bloquea para no guardarla dos veces)
    val guardando: Boolean = false,
    // Mensaje cuando no se pudo guardar la compra; null si no hay error
    val errorAlGuardar: String? = null,
    // Mensaje cuando no se pudo leer, guardar o modificar un productor; null si no hay error
    val errorProductor: String? = null,
    // Fallos seguidos al confirmar la compra; vuelve a 0 al reiniciar la compra
    val intentosFallidos: Int = 0
) {
    // true cuando ya se fallo demasiadas veces: el boton "Confirmar compra" se bloquea
    val reintentosAgotados: Boolean
        get() = intentosFallidos >= MAX_INTENTOS_GUARDAR_COMPRA

    // ---------- Avisos de capacidad (informativos, nunca bloquean) ----------

    // Paso 1: la capacidad aun no esta configurada.
    val avisoSinConfigurar: Boolean
        get() = capacidadKg <= 0.0

    // Pasos 2 y 3: lo que dejaria la compra que se esta escribiendo; null si queda por debajo de 90 %.
    // Ya registrada, kgOcupados incluye la compra: se devuelve null para no contarla dos veces
    // (el aviso de "Compra registrada" es avisoAlRegistrar, fijado antes de guardar).
    val avisoCapacidad: AvisoCapacidad?
        get() = if (compraRegistrada) null else avisoCapacidadCompra(capacidadKg, kgOcupados, totalKg(cafes))

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
        get() = if (nuevoProductor.documento.isEmpty()) null else validarDocumento(nuevoProductor.documento, nuevoProductor.tipo, productores.map { it.documento })

    val nombreError: String?
        get() = if (nuevoProductor.nombre.isEmpty()) null else validarNombre(nuevoProductor.nombre, nuevoProductor.tipo)

    val telefonoError: String?
        get() = validarTelefono(nuevoProductor.telefono)

    val guardarNuevoHabilitado: Boolean
        get() = productorValido(nuevoProductor, productores.map { it.documento })

    // ---------- Hoja "Productores" ----------

    val productoresEnLista: List<ProductorEnPantalla>
        get() = productoresDeLaLista(productores, busquedaLista).map { aPantalla(it) }

    val mensajeSinResultadosLista: String?
        get() = if (productoresEnLista.isEmpty()) SIN_RESULTADOS else null

    val edicionEsEmpresa: Boolean
        get() = datosEdicion.tipo == TipoDocumento.NIT

    // Al modificar, el documento del propio productor no cuenta como repetido
    private val documentosSinElEditado: List<String>
        get() = productores.filter { it != productorEnEdicion }.map { it.documento }

    val edicionDocumentoError: String?
        get() = validarDocumento(datosEdicion.documento, datosEdicion.tipo, documentosSinElEditado)

    val edicionNombreError: String?
        get() = validarNombre(datosEdicion.nombre, datosEdicion.tipo)

    val edicionTelefonoError: String?
        get() = validarTelefono(datosEdicion.telefono)

    val guardarEdicionHabilitado: Boolean
        get() = productorValido(datosEdicion, documentosSinElEditado)

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
                // Con punto de miles y coma decimal, como el precio ("12.500,5").
                peso = pesoEnPantalla(cafe.peso),
                precio = precioEnPantalla(cafe.precio),
                errorPeso = errorPeso(cafe),
                errorPrecio = errorPrecio(cafe.precio),
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
    val sublotes: List<CafeValidado>
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
// El repositorio (Room) y el origen de la ocupacion llegan por el constructor: asi las pruebas usan versiones falsas.
class PurchaseViewModel(
    private val repositorio: PurchaseRepository,
    private val ocupacion: OrigenOcupacion,
    // Recuperar una compra interrumpida (ver BorradorCompra). null = sin borrador (pruebas y vistas previas).
    private val borrador: BorradorDeOperacion? = null,
) : ViewModel() {

    // _uiState es privado: solo el ViewModel puede cambiar el estado.
    private val _uiState = MutableStateFlow(PurchaseUiState())
    // uiState es de solo lectura: la vista lo observa y se redibuja cuando cambia.
    val uiState: StateFlow<PurchaseUiState> = _uiState

    // Borrador recuperado que todavía espera la lista de productores para volver al paso 2 o 3.
    private var borradorPendiente: BorradorCompra? = null

    init {
        if (borrador != null) {
            decodificarBorradorCompra(borrador.leer())?.let { recuperado ->
                _uiState.value = recuperado.aplicar(_uiState.value)
                if (recuperado.esperaProductor) borradorPendiente = recuperado
            }
            // Cada cambio del formulario se recuerda; un formulario vacío o una compra registrada borran el borrador.
            // Mientras se espera la lista de productores no se guarda, para no perder el paso recuperado.
            viewModelScope.launch {
                _uiState.collect { estado ->
                    if (borradorPendiente == null) borrador.guardar(BorradorCompra.de(estado)?.codificar())
                }
            }
        }
    }

    // Lee los productores guardados en Room. La vista lo pide una vez al abrir Compras.
    fun cargarProductores() {
        viewModelScope.launch {
            try {
                recargarProductores()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Se queda la lista que ya habia; el registro de compra sigue funcionando.
                _uiState.update { it.copy(errorProductor = MENSAJE_ERROR_CARGAR_PRODUCTORES) }
            }
        }
    }

    // Vuelve a leer la lista desde Room (por ejemplo, si el documento ya existia y la copia estaba desactualizada).
    private suspend fun recargarProductores() {
        val guardados = repositorio.listarProductores()
        val pendiente = borradorPendiente
        borradorPendiente = null
        _uiState.update {
            val conLista = it.copy(productores = guardados, errorProductor = null)
            // El borrador recuperado vuelve a su paso solo si su productor sigue existiendo.
            if (pendiente != null && it.paso == 1) pendiente.aplicar(conLista, guardados) else conLista
        }
    }

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
        // Primero se cambia el formulario y luego el estado con ese formulario.
        _uiState.value = _uiState.value.let { it.copy(nuevoProductor = it.nuevoProductor.conTipo(tipo)) }
    }

    // Solo numeros, sin puntos, comas ni espacios
    fun cambiarDocumento(texto: String) {
        _uiState.value = _uiState.value.let { it.copy(nuevoProductor = it.nuevoProductor.conDocumento(texto)) }
    }

    // Solo letras y espacios
    fun cambiarNombre(texto: String) {
        _uiState.value = _uiState.value.let { it.copy(nuevoProductor = it.nuevoProductor.conNombre(texto)) }
    }

    fun cambiarTelefono(texto: String) {
        _uiState.value = _uiState.value.let { it.copy(nuevoProductor = it.nuevoProductor.conTelefono(texto)) }
    }

    // "Guardar productor": lo guarda en Room, lo agrega a la lista, lo deja elegido y activa Continuar
    fun guardarNuevoProductor() {
        val estado = _uiState.value
        if (!estado.guardarNuevoHabilitado) return
        val nuevo = crearProductor(estado.nuevoProductor)
        _uiState.update { it.copy(errorProductor = null) }
        viewModelScope.launch {
            try {
                when (val resultado = repositorio.guardarProductor(nuevo)) {
                    // El productor guardado ya trae su id: es el que se usa al registrar la compra.
                    is ResultadoProductor.Guardado -> _uiState.update {
                        it.copy(
                            productores = it.productores + resultado.productor,
                            productorElegido = resultado.productor,
                            nuevoProductor = NuevoProductor(),
                            modo = ModoProductor.REGISTRADO,
                            busqueda = ""
                        )
                    }
                    // La copia estaba desactualizada: al recargarla, documentoError muestra el aviso de siempre.
                    ResultadoProductor.DocumentoRepetido -> recargarProductores()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No se pudo guardar: lo escrito se conserva y se avisa para intentarlo de nuevo.
                _uiState.update { it.copy(errorProductor = MENSAJE_ERROR_GUARDAR_PRODUCTOR) }
            }
        }
    }

    // "Cancelar": borra lo escrito; la opcion que estaba elegida no cambia
    fun cancelarNuevoProductor() {
        _uiState.value = _uiState.value.copy(nuevoProductor = NuevoProductor(), errorProductor = null)
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
        _uiState.value = _uiState.value.let { it.copy(datosEdicion = it.datosEdicion.conTipo(tipo)) }
    }

    fun cambiarDocumentoEdicion(texto: String) {
        _uiState.value = _uiState.value.let { it.copy(datosEdicion = it.datosEdicion.conDocumento(texto)) }
    }

    fun cambiarNombreEdicion(texto: String) {
        _uiState.value = _uiState.value.let { it.copy(datosEdicion = it.datosEdicion.conNombre(texto)) }
    }

    fun cambiarTelefonoEdicion(texto: String) {
        _uiState.value = _uiState.value.let { it.copy(datosEdicion = it.datosEdicion.conTelefono(texto)) }
    }

    // "Guardar cambios": lo modifica en Room y reemplaza al productor en la lista y, si estaba elegido, tambien ahi
    fun guardarEdicion() {
        val estado = _uiState.value
        val original = estado.productorEnEdicion
        if (original == null || !estado.guardarEdicionHabilitado) return
        // El formulario no trae el id: se conserva el del original para modificar la misma fila.
        val editado = crearProductor(estado.datosEdicion).copy(id = original.id)
        _uiState.update { it.copy(errorProductor = null) }
        viewModelScope.launch {
            try {
                when (repositorio.actualizarProductor(editado)) {
                    is ResultadoProductor.Guardado -> _uiState.update {
                        it.copy(
                            productores = it.productores.map { p -> if (p == original) editado else p },
                            productorElegido = if (it.productorElegido == original) editado else it.productorElegido,
                            productorDeLaCompra = if (it.productorDeLaCompra == original) editado else it.productorDeLaCompra,
                            productorEnEdicion = null
                        )
                    }
                    ResultadoProductor.DocumentoRepetido -> recargarProductores()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No se pudo guardar: la hoja sigue abierta con lo escrito y se avisa.
                _uiState.update { it.copy(errorProductor = MENSAJE_ERROR_GUARDAR_PRODUCTOR) }
            }
        }
    }

    // "Cancelar" en la edicion: vuelve a la lista sin guardar
    fun cancelarEdicion() {
        _uiState.value = _uiState.value.copy(productorEnEdicion = null, errorProductor = null)
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

    // Guarda la compra en Room (la Compra y un Sublote por cada cafe, en una sola transaccion) y muestra
    // "Compra registrada". Si falla no se muestra y queda el mensaje de error para intentarlo de nuevo.
    fun confirmarCompra() {
        val estado = _uiState.value
        // Tampoco se guarda de nuevo una compra ya registrada ni despues de agotar los reintentos.
        if (!estado.cafesListos || estado.guardando || estado.compraRegistrada || estado.reintentosAgotados) return
        // El aviso se calcula ANTES de guardar, con la ocupacion de ese momento: despues de guardar,
        // kgOcupados ya incluye esta compra y se contaria dos veces.
        val aviso = estado.avisoCapacidad
        // Un productor guardado trae id; "Sin registrar" tiene id 0 y se guarda como productorId = null.
        val productorId = estado.productorDeLaCompra?.id?.takeIf { it != 0L }
        _uiState.value = estado.copy(guardando = true, errorAlGuardar = null)
        viewModelScope.launch {
            try {
                repositorio.registrarCompra(productorId, estado.fecha, estado.sublotes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    val fallos = it.intentosFallidos + 1
                    it.copy(
                        guardando = false,
                        intentosFallidos = fallos,
                        errorAlGuardar = if (fallos >= MAX_INTENTOS_GUARDAR_COMPRA) MENSAJE_ERROR_GUARDAR_COMPRA_AGOTADO else MENSAJE_ERROR_GUARDAR_COMPRA
                    )
                }
                return@launch
            }
            // La compra YA quedo guardada: si leer los kilos falla, no se muestra como error (un reintento la duplicaria).
            // Con la compra guardada, la ocupacion de hoy la incluye: queda lista para la siguiente compra.
            val kgHoy = try {
                ocupacion.kgOcupados()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    compraRegistrada = true,
                    avisoAlRegistrar = aviso,
                    guardando = false,
                    // Sin lectura: se suman los kilos de esta compra a los que ya se tenian.
                    kgOcupados = kgHoy ?: (it.kgOcupados + totalKg(estado.cafes))
                )
            }
        }
    }

    // La bodega cambio (se configuro la capacidad o cambiaron los kilos). Una compra ya registrada conserva su aviso.
    fun actualizarBodega(capacidadKg: Double, kgOcupados: Double) {
        if (_uiState.value.compraRegistrada) return
        _uiState.value = _uiState.value.copy(capacidadKg = capacidadKg, kgOcupados = kgOcupados)
    }

    // Empieza una compra desde cero, conservando los productores registrados
    fun reiniciarCompra() {
        // Un estado nuevo vuelve todo a sus valores iniciales, menos la lista de productores.
        val actual = _uiState.value
        _uiState.value = PurchaseUiState(
            productores = actual.productores,
            capacidadKg = actual.capacidadKg,
            kgOcupados = actual.kgOcupados
        )
    }

    // Reemplaza solo la tarjeta que cambio
    private fun reemplazarCafe(indice: Int, nuevo: CafeFormulario) {
        // Si la posicion coincide usa la tarjeta nueva; si no, deja la que estaba.
        val cafes = _uiState.value.cafes.mapIndexed { i, actual -> if (i == indice) nuevo else actual }
        _uiState.value = _uiState.value.copy(cafes = cafes)
    }
}
