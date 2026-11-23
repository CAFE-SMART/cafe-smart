package com.cafesmart.app.viewmodel.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.ResultadoCliente
import com.cafesmart.app.data.sale.VentaRepository
import com.cafesmart.app.data.sale.NuevoCliente
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.viewmodel.purchase.CafeEnResumen
import com.cafesmart.app.viewmodel.purchase.SIN_RESULTADOS
import com.cafesmart.app.viewmodel.purchase.conDocumento
import com.cafesmart.app.viewmodel.purchase.conNombre
import com.cafesmart.app.viewmodel.purchase.conTelefono
import com.cafesmart.app.viewmodel.purchase.conTipo
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
import com.cafesmart.app.viewmodel.purchase.limpiarPrecio
import com.cafesmart.app.viewmodel.purchase.milisAFecha
import com.cafesmart.app.viewmodel.purchase.pesoEnPantalla
import com.cafesmart.app.viewmodel.purchase.precioEnPantalla
import com.cafesmart.app.viewmodel.purchase.precioKg
import com.cafesmart.app.viewmodel.purchase.precioValido
import com.cafesmart.app.viewmodel.purchase.validarDocumento
import com.cafesmart.app.viewmodel.purchase.validarNombre
import com.cafesmart.app.viewmodel.purchase.validarTelefono
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Un cliente tal como lo muestra la vista en la lista (textos ya listos)
data class ClienteEnPantalla(
    val cliente: Cliente,
    val nombre: String,
    // Por ejemplo "Cédula 16780012" o "NIT 9003421215"
    val documento: String,
    val elegido: Boolean
)

// Un sublote tal como lo muestra la vista en el paso 2 (textos ya listos)
data class SubloteEnPantalla(
    val sublote: SubloteConLote,
    // "Sublote 5"
    val titulo: String,
    // "200 kg disponibles · Comprado hace 6 días"
    val detalle: String,
    val elegido: Boolean,
    // Cantidad que se vende, como se ve en la tarjeta elegida: "150 kg"
    val cantidadTexto: String,
    // Lo que muestra el campo "Cantidad a vender" cuando se pulsa "Cambiar": "150" o lo que se escribio
    val cantidadCampo: String,
    // "Debe ser menor o igual a 200 kg." (ayuda siempre visible; si la cantidad pasa, sale como error)
    val ayudaCantidad: String,
    val errorCantidad: String?,
    // true si lo escrito en el campo sirve (mayor a 0 y sin pasar de lo disponible)
    val cantidadValida: Boolean
)

// Campo "Precio por kg · calidad buena" que aparece cuando hay un sublote elegido de esa calidad
data class PrecioEnPantalla(
    val titulo: String,
    // Con punto de miles: "20.000"
    val texto: String,
    val error: String?,
    val ayuda: String
)

// Un grupo del paso 2: la etiqueta de una calidad, sus sublotes y (si hay alguno elegido) su precio
data class GrupoCalidad(
    val calidad: Calidad,
    // "Calidad buena"
    val etiqueta: String,
    // "2 sublotes · 280 kg"
    val resumen: String,
    val sublotes: List<SubloteEnPantalla>,
    val precio: PrecioEnPantalla?
)

// Estado del registro de venta.
// Los datos se guardan arriba; los valores con get() son lo que la vista muestra,
// ya calculados aqui, para que la vista no tenga logica.
data class SaleUiState(
    // Clientes registrados en Room, del mas antiguo al mas reciente (se leen con cargarClientes).
    val clientes: List<Cliente> = emptyList(),
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
    val textosCantidad: Map<Long, String> = emptyMap(),
    // Sublotes del inventario de Room, tambien los agotados (sublotesDelTipo ya los quita). Se leen con observarInventario.
    val inventario: List<SubloteConLote> = emptyList(),
    // Pestana del paso 2 ("Verde", "Seco"...)
    val tipoCafe: TipoCafe = TipoCafe.SECO,
    // Hoy y la fecha de la venta (por defecto hoy); "Cambiar" en el paso 3 la modifica
    val hoy: Fecha = fechaDeHoy(),
    val fecha: Fecha = hoy,
    // true cuando se confirma la venta: evita registrarla dos veces
    val ventaRegistrada: Boolean = false,
    // Aviso cuando no se pudo leer o guardar un cliente (null si todo va bien).
    val errorCliente: String? = null,
    // true mientras se guarda o modifica un cliente en Room: un segundo toque se ignora
    val guardandoCliente: Boolean = false
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
        documento = documentoTexto(cliente),
        elegido = cliente == this.cliente
    )

    // ---------- Paso 2: cafe ----------

    private val sublotesDelTipo: List<SubloteConLote>
        get() = sublotesDelTipo(inventario, tipoCafe)

    private fun estaElegido(sublote: SubloteConLote): Boolean = elegidos.any { it.id == sublote.sublote.id }

    // Pestanas de tipo de cafe y la que esta activa
    val tipos: List<TipoCafe>
        get() = tiposParaVender

    // Mensaje cuando no hay cafe de este tipo para vender (null si si hay)
    val mensajeSinCafe: String?
        get() = if (sublotesDelTipo.isEmpty()) "No tienes café ${tipoCafe.etiqueta.lowercase()} disponible para vender." else null

    // "Seleccionar todo": cuantos hay elegidos de los que se pueden vender
    val textoSeleccionarTodo: String
        get() = textoSeleccionados(sublotesDelTipo.count { estaElegido(it) }, sublotesDelTipo.size)

    val todosElegidos: Boolean
        get() = sublotesDelTipo.isNotEmpty() && sublotesDelTipo.all { estaElegido(it) }

    // Algunos pero no todos: la casilla muestra un "-"
    val algunosElegidos: Boolean
        get() = !todosElegidos && sublotesDelTipo.any { estaElegido(it) }

    val grupos: List<GrupoCalidad>
        get() = sublotesDelTipo.groupBy { it.calidad }.map { (calidad, sublotes) ->
            val elegidosDeCalidad = elegidos.filter { it.calidad == calidad }
            GrupoCalidad(
                calidad = calidad,
                etiqueta = "Calidad ${calidad.etiqueta.lowercase()}",
                resumen = resumenGrupo(sublotes.size, sublotes.sumOf { it.sublote.pesoDisponibleKg }),
                sublotes = sublotes.map { aPantalla(it) },
                precio = if (elegidosDeCalidad.isEmpty()) null else PrecioEnPantalla(
                    titulo = "Precio por kg · calidad ${calidad.etiqueta.lowercase()}",
                    texto = precioEnPantalla(textoPrecio(calidad)),
                    // Con el precio sin escribir se marca el campo con lo que falta; escrito mal, con su propio error.
                    error = errorDePrecio(calidad) ?: mensajePrecioPendiente(calidad, textoPrecio(calidad)).takeIf { textoPrecio(calidad).isEmpty() },
                    ayuda = ayudaPrecio(costoPromedio(elegidosDeCalidad))
                )
            )
        }

    private fun aPantalla(item: SubloteConLote): SubloteEnPantalla {
        val id = item.sublote.id
        val disponible = item.sublote.pesoDisponibleKg
        return SubloteEnPantalla(
            sublote = item,
            titulo = "Sublote $id",
            detalle = "${formatoKg(disponible)} disponibles · ${textoComprado(diasDesde(item.sublote.fechaIngreso, hoy))}",
            elegido = estaElegido(item),
            cantidadTexto = formatoKg(elegidos.firstOrNull { it.id == id }?.cantidadKg ?: 0.0),
            cantidadCampo = campoCantidad(id),
            ayudaCantidad = mensajeCantidad(disponible),
            errorCantidad = errorDeCantidad(id),
            cantidadValida = cantidadValida(cantidadDesdeTexto(textoCantidad(id)), disponible)
        )
    }

    // Cada calidad que hay entre los sublotes elegidos pide su propio precio por kg.
    val calidadesElegidas: List<Calidad>
        get() = Calidad.entries.filter { calidad -> elegidos.any { it.calidad == calidad } }

    // orEmpty(): si todavia no se escribio el precio de esa calidad devuelve texto vacio.
    private fun textoPrecio(calidad: Calidad): String = precios[calidad].orEmpty()

    fun errorDePrecio(calidad: Calidad): String? = errorPrecio(textoPrecio(calidad))

    // Texto del campo "Cantidad a vender" de un sublote: lo escrito, o la cantidad elegida si no se ha tocado.
    fun textoCantidad(subloteId: Long): String =
        textosCantidad[subloteId] ?: elegidos.firstOrNull { it.id == subloteId }?.cantidadKg?.let(::formatoKgCampo).orEmpty()

    // Lo que se ve en el campo: coma para decimales y punto para miles, como en Compras.
    fun campoCantidad(subloteId: Long): String = pesoEnPantalla(textoCantidad(subloteId))

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

    // Redondeado a gramos: sumar decimales puede dar 4.999999 cuando son 5 kg y rechazaria una venta valida.
    val totalKg: Double
        get() = redondearKg(elegidos.sumOf { it.cantidadKg })

    // Cada sublote vale su cantidad por el precio de su calidad (0 mientras no se escribe el precio).
    val totalARecibir: Long
        get() = elegidos.sumOf { subtotal(it.cantidadKg, precioKg(textoPrecio(it.calidad)) ?: 0.0) }

    val totalKgTexto: String
        get() = formatoKg(totalKg)

    val totalARecibirTexto: String
        get() = formatoDinero(totalARecibir.toDouble())

    // "Continuar" del paso 2: hay al menos un sublote, cada cantidad escrita es valida, el total llega al minimo
    // de una venta y cada calidad elegida tiene un precio valido.
    // Mensaje cuando lo que se vende suma menos del minimo de una venta (null si no hay problema).
    val errorDeTotal: String?
        get() = errorTotalKg(totalKg, elegidos.isNotEmpty())

    val puedeContinuar: Boolean
        get() = elegidos.isNotEmpty() && cantidadesValidas && errorDeTotal == null &&
            calidadesElegidas.all { precioValido(textoPrecio(it)) }

    // Un precio de 1 a 3 digitos no sale en rojo mientras se escribe: el aviso dice de que calidad es.
    private fun avisoPrecioSinMarcar(): String? =
        calidadesElegidas.firstOrNull { errorDePrecio(it) == null && textoPrecio(it).isNotEmpty() && !precioValido(textoPrecio(it)) }
            ?.let { "Revisa el precio por kg de la calidad ${it.etiqueta.lowercase()}." }

    // Texto sobre "Continuar" del paso 2. Si hay un campo en rojo (precio o cantidad) solo avisa que se revise;
    // el error exacto esta en el campo. El minimo de kg no se repite: ya se ve en rojo arriba.
    val mensajePendientePaso2: String?
        get() = when {
            elegidos.isEmpty() -> "Elige al menos un sublote para continuar."
            !cantidadesValidas -> if (elegidos.any { errorDeCantidad(it.id) != null }) {
                MENSAJE_REVISA_CAMPOS
            } else {
                "Escribe la cantidad a vender de cada sublote elegido."
            }
            errorDeTotal != null -> null
            calidadesElegidas.any { mensajePrecioPendiente(it, textoPrecio(it)) != null } -> avisoPrecioSinMarcar() ?: MENSAJE_REVISA_CAMPOS
            else -> null
        }

    // ---------- Paso 3: confirmar ----------

    // Cliente de la venta como se ve en el resumen; "Sin registrar" cuando no se eligio ninguno.
    val clienteTexto: String
        get() = clienteDeLaVenta?.nombre ?: "Sin registrar"

    // "Cédula 16780012" / "NIT 9003421215"; null cuando la venta es sin registrar.
    val clienteDocumentoTexto: String?
        get() = clienteDeLaVenta?.let(::documentoTexto)

    val fechaResumenTexto: String
        get() = formatoFecha(fecha)

    val fechaMilis: Long
        get() = fechaAMilis(fecha)

    val errorDeFecha: String?
        get() = errorFecha(fecha, hoy, elegidos)

    // Un cafe del resumen por cada sublote vendido: "Seco · Sublote 5", "150 kg × $ 20.000" y su total.
    val cafesEnResumen: List<CafeEnResumen>
        get() = elegidos.map {
            val precio = precioKg(textoPrecio(it.calidad)) ?: 0.0
            CafeEnResumen(
                tipoCafe = it.sublote.tipoCafe.etiqueta,
                titulo = "${it.sublote.tipoCafe.etiqueta} · Sublote ${it.id}",
                calidad = "Calidad ${it.calidad.etiqueta.lowercase()}",
                detalle = "${formatoKg(it.cantidadKg)} × ${formatoDinero(precio)}",
                total = formatoDinero(subtotal(it.cantidadKg, precio).toDouble())
            )
        }

    // Linea "Café" de la pantalla final: el sublote, o cuantos sublotes si fueron varios.
    val cafeResumenTexto: String
        get() = if (elegidos.size == 1) cafesEnResumen.first().titulo else "${tipoCafe.etiqueta} · ${elegidos.size} sublotes"

    // "Confirmar venta": todo lo anterior esta bien y la venta no se ha registrado ya.
    val puedeConfirmar: Boolean
        get() = !ventaRegistrada && continuarPaso1Habilitado && puedeContinuar && errorDeFecha == null
}

// Cuando Room no logra guardar, modificar o leer clientes (textos propuestos; falta confirmarlos con el equipo)
const val MENSAJE_ERROR_CARGAR_CLIENTES = "No pudimos cargar tus clientes. Inténtalo de nuevo."
const val MENSAJE_ERROR_GUARDAR_CLIENTE = "No pudimos guardar el cliente. Inténtalo de nuevo."

// ViewModel (MVVM): aqui esta la logica del registro de venta.
// Guarda el estado en un StateFlow y recibe las acciones de la vista.
// El repositorio de ventas y el inventario (Room) llegan por el constructor: asi las pruebas usan versiones falsas.
class SaleViewModel(
    private val repositorio: VentaRepository,
    private val inventario: InventoryRepository
) : ViewModel() {

    // _uiState es privado: solo el ViewModel puede cambiar el estado.
    private val _uiState = MutableStateFlow(SaleUiState())
    // uiState es de solo lectura: la vista lo observa y se redibuja cuando cambia.
    val uiState: StateFlow<SaleUiState> = _uiState

    // Lee los clientes guardados en Room. La vista lo pide al entrar al flujo de venta.
    fun cargarClientes() {
        viewModelScope.launch {
            try {
                recargarClientes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Se queda la lista que ya habia; la venta "Sin registrar" sigue funcionando.
                _uiState.update { it.copy(errorCliente = MENSAJE_ERROR_CARGAR_CLIENTES) }
            }
        }
    }

    private suspend fun recargarClientes() {
        val clientes = repositorio.listarClientes()
        _uiState.update { it.copy(clientes = clientes, errorCliente = null) }
    }

    // Trabajo que lee el inventario; se guarda para no leerlo dos veces si la vista lo pide otra vez.
    private var lecturaInventario: Job? = null

    // Lee el inventario de Room (la misma fuente que la pantalla Inventario) y lo mantiene al dia:
    // cada compra o venta vuelve a emitir la lista. Pide los sublotes agotados tambien.
    fun observarInventario() {
        if (lecturaInventario?.isActive == true) return
        lecturaInventario = viewModelScope.launch {
            inventario.observarLotes().collect { lotes ->
                val sublotes = lotes.flatMap { grupo -> grupo.sublotes.map { SubloteConLote(it, grupo.lote) } }
                _uiState.update { it.copy(inventario = sublotes) }
            }
        }
    }

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

    // "Guardar cambios": lo modifica en Room y reemplaza al cliente en la lista (conservando su id) y, si estaba elegido, tambien ahi
    fun guardarEdicion() {
        val estado = _uiState.value
        val original = estado.clienteEnEdicion
        if (original == null || !estado.guardarEdicionHabilitado || estado.guardandoCliente) return
        val editado = crearCliente(estado.datosEdicion).copy(id = original.id)
        _uiState.update { it.copy(errorCliente = null, guardandoCliente = true) }
        viewModelScope.launch {
            try {
                when (repositorio.actualizarCliente(editado)) {
                    is ResultadoCliente.Guardado -> _uiState.update {
                        it.copy(
                            clientes = it.clientes.map { c -> if (c == original) editado else c },
                            cliente = if (it.cliente == original) editado else it.cliente,
                            clienteEnEdicion = null
                        )
                    }
                    // La copia estaba desactualizada: al recargarla, edicionDocumentoError muestra el aviso de siempre.
                    ResultadoCliente.DocumentoRepetido -> recargarClientes()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No se pudo guardar: la hoja sigue abierta con lo escrito y se avisa.
                _uiState.update { it.copy(errorCliente = MENSAJE_ERROR_GUARDAR_CLIENTE) }
            } finally {
                // Termine bien, con documento repetido, con error o cancelado, se puede volver a tocar.
                _uiState.update { it.copy(guardandoCliente = false) }
            }
        }
    }

    // "Cancelar" en la modificacion: vuelve a la lista sin guardar
    fun cancelarEdicion() {
        _uiState.value = _uiState.value.copy(clienteEnEdicion = null)
    }

    // "Continuar" del paso 1. Con "Cliente nuevo" lo guarda en Room, lo agrega a la lista y lo deja elegido
    // (con el id real). Con las otras opciones no cambia nada: el cliente ya esta elegido.
    // Si falta algo no hace nada. [alTerminar] se llama solo cuando el paso 1 quedo listo (la vista navega al
    // paso 2); si el documento ya existia o no se pudo guardar, no se llama y la persona se queda en el paso 1.
    fun continuarPaso1(alTerminar: () -> Unit = {}) {
        val estado = _uiState.value
        if (!estado.continuarPaso1Habilitado) return
        if (estado.modo != ModoCliente.NUEVO) {
            alTerminar()
            return
        }
        // Un segundo toque mientras se guarda el primero no hace nada: no se guarda dos veces ni se navega dos veces.
        if (estado.guardandoCliente) return
        val nuevo = crearCliente(estado.nuevoCliente)
        _uiState.update { it.copy(errorCliente = null, guardandoCliente = true) }
        viewModelScope.launch {
            try {
                when (val resultado = repositorio.guardarCliente(nuevo)) {
                    // El cliente guardado ya trae su id: es el que se usa al registrar la venta.
                    is ResultadoCliente.Guardado -> {
                        _uiState.update {
                            it.copy(
                                clientes = it.clientes + resultado.cliente,
                                cliente = resultado.cliente,
                                modo = ModoCliente.REGISTRADO,
                                nuevoCliente = NuevoCliente(),
                                busqueda = ""
                            )
                        }
                        alTerminar()
                    }
                    // La copia estaba desactualizada: al recargarla, documentoError muestra el aviso de siempre.
                    ResultadoCliente.DocumentoRepetido -> recargarClientes()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No se pudo guardar: lo escrito se conserva y se avisa para intentarlo de nuevo.
                _uiState.update { it.copy(errorCliente = MENSAJE_ERROR_GUARDAR_CLIENTE) }
            } finally {
                // Termine bien, con documento repetido, con error o cancelado, se puede volver a tocar.
                _uiState.update { it.copy(guardandoCliente = false) }
            }
        }
    }

    // ---------------- Paso 2: cafe ----------------

    // Cambia la pestana de tipo de cafe. La venta es de un solo tipo: lo elegido antes se borra.
    // Con la misma pestana no cambia nada.
    fun elegirTipoCafe(tipo: TipoCafe) {
        val estado = _uiState.value
        if (tipo == estado.tipoCafe) return
        _uiState.value = estado.copy(tipoCafe = tipo, elegidos = emptyList(), precios = emptyMap(), textosCantidad = emptyMap())
    }

    // "Seleccionar todo": si ya estan todos elegidos los quita; si no, elige todos (a los ya elegidos
    // les respeta la cantidad que tenian).
    fun alternarTodos() {
        val estado = _uiState.value
        val delTipo = sublotesDelTipo(estado.inventario, estado.tipoCafe)
        _uiState.value = if (estado.todosElegidos) {
            estado.copy(elegidos = emptyList(), textosCantidad = emptyMap(), precios = emptyMap())
        } else {
            estado.copy(elegidos = delTipo.map { item ->
                estado.elegidos.firstOrNull { it.id == item.sublote.id } ?: SubloteElegido(item, item.sublote.pesoDisponibleKg)
            })
        }
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
        _uiState.value = _uiState.value.copy(elegidos = nuevos, textosCantidad = _uiState.value.textosCantidad - id).sinPreciosSueltos()
    }

    // El precio es por calidad: si ya no queda un sublote elegido de esa calidad, su precio se borra.
    private fun SaleUiState.sinPreciosSueltos(): SaleUiState =
        copy(precios = precios.filterKeys { it in calidadesElegidas })

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
    // Conserva tambien el inventario, que ya tiene descontado lo vendido.
    fun reiniciarVenta() {
        _uiState.value = SaleUiState(clientes = _uiState.value.clientes, inventario = _uiState.value.inventario)
    }

    // Se guarda solo con digitos ("20000"); la vista recibe "20.000"
    fun cambiarPrecio(calidad: Calidad, texto: String) {
        val precios = _uiState.value.precios + (calidad to limpiarPrecio(texto))
        _uiState.value = _uiState.value.copy(precios = precios)
    }

    // ---------------- Paso 3: confirmar ----------------

    // El calendario entrega milisegundos; aqui se convierten a Fecha
    fun cambiarFecha(millis: Long) {
        _uiState.value = _uiState.value.copy(fecha = milisAFecha(millis))
    }

    // "Confirmar venta": descuenta lo vendido del inventario y marca la venta como registrada.
    // Si falta algo, o ya se registro, no hace nada (asi no se puede registrar dos veces).
    // Temporal: cuando exista Room aqui se guardan la Venta y sus VentaSublote.
    // Devuelve true solo si esta llamada registro la venta: un segundo toque no la registra otra vez.
    fun confirmarVenta(): Boolean {
        val estado = _uiState.value
        if (!estado.puedeConfirmar) return false
        _uiState.value = estado.copy(inventario = descontarVenta(estado.inventario, estado.elegidos), ventaRegistrada = true)
        return true
    }
}

// Cantidad como se escribe en el campo: sin ".0" si es entera ("150") y con punto decimal si no ("25.5").
private fun formatoKgCampo(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()
