package com.cafesmart.app.viewmodel.sale

import androidx.lifecycle.ViewModel
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.NuevoCliente
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.purchase.SIN_RESULTADOS
import com.cafesmart.app.viewmodel.purchase.conDocumento
import com.cafesmart.app.viewmodel.purchase.conNombre
import com.cafesmart.app.viewmodel.purchase.conTelefono
import com.cafesmart.app.viewmodel.purchase.conTipo
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
import com.cafesmart.app.viewmodel.purchase.limpiarPrecio
import com.cafesmart.app.viewmodel.purchase.precioKg
import com.cafesmart.app.viewmodel.purchase.precioValido
import com.cafesmart.app.viewmodel.purchase.validarDocumento
import com.cafesmart.app.viewmodel.purchase.validarNombre
import com.cafesmart.app.viewmodel.purchase.validarTelefono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Un cliente tal como lo muestra la vista en la lista (textos ya listos)
data class ClienteEnPantalla(
    val cliente: Cliente,
    val nombre: String,
    // Por ejemplo "Cédula 16780012" o "NIT 9003421215"
    val documento: String,
    val elegido: Boolean
)

// Estado del registro de venta.
// Los datos se guardan arriba; los valores con get() son lo que la vista muestra,
// ya calculados aqui, para que la vista no tenga logica.
data class SaleUiState(
    // Clientes registrados, del mas antiguo al mas reciente. Temporal: se reemplazan por los de Room.
    val clientes: List<Cliente> = clientesDeEjemplo,
    // Opcion elegida en el paso 1; null mientras no se elige ninguna
    val modo: ModoCliente? = null,
    // Lo que se escribio en el buscador de la tarjeta "Cliente registrado" y en el de la hoja "Clientes"
    val busqueda: String = "",
    val busquedaLista: String = "",
    // Lo que se va escribiendo en la tarjeta "Cliente nuevo"
    val nuevoCliente: NuevoCliente = NuevoCliente(),
    // Cliente que se modifica en la hoja "Clientes" (null mientras se ve la lista) y lo que se va escribiendo
    val clienteEnEdicion: Cliente? = null,
    val datosEdicion: NuevoCliente = NuevoCliente(),
    // Cliente elegido de la lista; null mientras no se elige ninguno
    val cliente: Cliente? = null,
    val elegidos: List<SubloteElegido> = emptyList(),
    // Precio por kg de cada calidad, solo con digitos ("20000"); la vista lo muestra como "20.000"
    val precios: Map<Calidad, String> = emptyMap(),
    // Lo que se escribio en el campo "Cantidad a vender" de cada sublote (por id). Se guarda aunque sea invalido,
    // para que la vista muestre lo escrito y su error; sin texto, el campo muestra la cantidad elegida.
    val textosCantidad: Map<Long, String> = emptyMap()
) {
    // ---------- Paso 1: cliente ----------

    val registradoSeleccionado: Boolean
        get() = modo == ModoCliente.REGISTRADO

    val sinRegistrarSeleccionado: Boolean
        get() = modo == ModoCliente.SIN_REGISTRAR

    val nuevoSeleccionado: Boolean
        get() = modo == ModoCliente.NUEVO

    // Los clientes de la tarjeta "Cliente registrado": los recientes o los que coinciden con la busqueda.
    val clientesEnPantalla: List<ClienteEnPantalla>
        get() = clientesVisibles(clientes, busqueda).map { aPantalla(it) }

    val tituloLista: String
        get() = if (busqueda.isBlank()) "Recientes" else "Resultados"

    // Mensaje cuando no hay clientes para mostrar (null si si hay)
    val mensajeListaVacia: String?
        get() = when {
            clientesEnPantalla.isNotEmpty() -> null
            busqueda.isBlank() -> "Aún no tienes clientes registrados. Usa \"Cliente nuevo\" para crear uno."
            else -> SIN_RESULTADOS
        }

    val hayMasClientes: Boolean
        get() = hayMasClientes(clientes, busqueda)

    // La hoja "Clientes" (Ver más clientes): todos, segun su propio buscador.
    val clientesEnLista: List<ClienteEnPantalla>
        get() = clientesDeLaLista(clientes, busquedaLista).map { aPantalla(it) }

    val mensajeSinResultadosLista: String?
        get() = if (clientesEnLista.isEmpty()) SIN_RESULTADOS else null

    val esEmpresa: Boolean
        get() = nuevoCliente.tipo == TipoDocumento.NIT

    // Se marca el primer campo que falta (documento, luego nombre) y los que tienen algo escrito mal.
    val documentoError: String?
        get() = errorDocumentoNuevo(nuevoCliente, clientes, nuevoSeleccionado)

    val nombreError: String?
        get() = errorNombreNuevo(nuevoCliente, clientes, nuevoSeleccionado)

    val telefonoError: String?
        get() = validarTelefono(nuevoCliente.telefono)

    // Modificar cliente: el documento del propio cliente no cuenta como repetido.
    private val clientesSinElEditado: List<Cliente>
        get() = clientes.filter { it != clienteEnEdicion }

    val edicionEsEmpresa: Boolean
        get() = datosEdicion.tipo == TipoDocumento.NIT

    val edicionDocumentoError: String?
        get() = validarDocumento(datosEdicion.documento, datosEdicion.tipo, documentosDe(clientesSinElEditado))

    val edicionNombreError: String?
        get() = validarNombre(datosEdicion.nombre, datosEdicion.tipo)

    val edicionTelefonoError: String?
        get() = validarTelefono(datosEdicion.telefono)

    val guardarEdicionHabilitado: Boolean
        get() = clienteValido(datosEdicion, clientesSinElEditado)

    val continuarPaso1Habilitado: Boolean
        get() = puedeContinuarPaso1(modo, cliente, nuevoCliente, clientes)

    // Texto sobre "Continuar" que dice que falta (null cuando ya se puede continuar).
    val mensajePendientePaso1: String?
        get() = mensajePendientePaso1(modo, cliente, nuevoCliente, clientes)

    // A quien se le vende: el cliente elegido, o null cuando es "Sin registrar" (Venta.clienteId = null).
    val clienteDeLaVenta: Cliente?
        get() = if (modo == ModoCliente.REGISTRADO) cliente else null

    // Convierte un cliente en la fila que muestra la vista
    private fun aPantalla(cliente: Cliente) = ClienteEnPantalla(
        cliente = cliente,
        nombre = cliente.nombre,
        documento = "${cliente.tipoDocumento.etiqueta} ${cliente.documento}",
        elegido = cliente == this.cliente
    )

    // ---------- Paso 2: cafe ----------

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

    // ---------------- Paso 1: cliente ----------------

    fun elegirModo(modo: ModoCliente) {
        _uiState.value = _uiState.value.copy(modo = modo)
    }

    fun cambiarBusqueda(texto: String) {
        _uiState.value = _uiState.value.copy(busqueda = texto)
    }

    fun elegirCliente(cliente: Cliente) {
        _uiState.value = _uiState.value.copy(cliente = cliente)
    }

    // El formulario "Cliente nuevo" limpia cada campo al escribirlo, igual que el de "Productor nuevo".
    private fun cambiarNuevoCliente(cambio: (NuevoCliente) -> NuevoCliente) {
        _uiState.value = _uiState.value.let { it.copy(nuevoCliente = cambio(it.nuevoCliente)) }
    }

    fun cambiarTipoDocumento(tipo: TipoDocumento) = cambiarNuevoCliente { it.conTipo(tipo) }

    // Solo numeros, sin puntos, comas ni espacios
    fun cambiarDocumento(texto: String) = cambiarNuevoCliente { it.conDocumento(texto) }

    // Solo letras y espacios (una empresa tambien numeros, punto y "&")
    fun cambiarNombre(texto: String) = cambiarNuevoCliente { it.conNombre(texto) }

    fun cambiarTelefono(texto: String) = cambiarNuevoCliente { it.conTelefono(texto) }

    // Hoja "Clientes" (Ver más clientes)
    fun cambiarBusquedaLista(texto: String) {
        _uiState.value = _uiState.value.copy(busquedaLista = texto)
    }

    // Al cerrar la hoja se limpia su buscador y se descarta la modificacion que quedo a medias,
    // para que la proxima vez abra desde cero
    fun cerrarLista() {
        _uiState.value = _uiState.value.copy(busquedaLista = "", clienteEnEdicion = null)
    }

    // "Editar": carga los datos del cliente en el formulario de la hoja
    fun editarCliente(cliente: Cliente) {
        _uiState.value = _uiState.value.copy(
            clienteEnEdicion = cliente,
            datosEdicion = NuevoCliente(
                tipo = cliente.tipoDocumento,
                documento = cliente.documento,
                nombre = cliente.nombre,
                telefono = cliente.telefono
            )
        )
    }

    private fun cambiarEdicion(cambio: (NuevoCliente) -> NuevoCliente) {
        _uiState.value = _uiState.value.let { it.copy(datosEdicion = cambio(it.datosEdicion)) }
    }

    fun cambiarTipoEdicion(tipo: TipoDocumento) = cambiarEdicion { it.conTipo(tipo) }

    fun cambiarDocumentoEdicion(texto: String) = cambiarEdicion { it.conDocumento(texto) }

    fun cambiarNombreEdicion(texto: String) = cambiarEdicion { it.conNombre(texto) }

    fun cambiarTelefonoEdicion(texto: String) = cambiarEdicion { it.conTelefono(texto) }

    // "Guardar cambios": reemplaza al cliente en la lista (conservando su id) y, si estaba elegido, tambien ahi
    fun guardarEdicion() {
        val estado = _uiState.value
        val original = estado.clienteEnEdicion
        if (original == null || !estado.guardarEdicionHabilitado) return
        val editado = crearCliente(estado.datosEdicion).copy(id = original.id)
        _uiState.value = estado.copy(
            clientes = estado.clientes.map { if (it == original) editado else it },
            cliente = if (estado.cliente == original) editado else estado.cliente,
            clienteEnEdicion = null
        )
    }

    // "Cancelar" en la modificacion: vuelve a la lista sin guardar
    fun cancelarEdicion() {
        _uiState.value = _uiState.value.copy(clienteEnEdicion = null)
    }

    // "Continuar" del paso 1. Con "Cliente nuevo" lo registra: lo agrega a la lista y lo deja elegido.
    // Con las otras opciones no cambia nada: el cliente ya esta elegido. Si falta algo no hace nada.
    fun continuarPaso1() {
        val estado = _uiState.value
        if (!estado.continuarPaso1Habilitado) return
        if (estado.modo == ModoCliente.NUEVO) {
            val nuevo = crearCliente(estado.nuevoCliente)
            _uiState.value = estado.copy(
                clientes = estado.clientes + nuevo,
                cliente = nuevo,
                modo = ModoCliente.REGISTRADO,
                nuevoCliente = NuevoCliente(),
                busqueda = ""
            )
        }
    }

    // ---------------- Paso 2: cafe ----------------

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
    // Conserva los clientes registrados.
    fun reiniciarVenta() {
        _uiState.value = SaleUiState(clientes = _uiState.value.clientes)
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
