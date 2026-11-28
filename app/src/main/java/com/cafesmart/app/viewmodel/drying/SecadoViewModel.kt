package com.cafesmart.app.viewmodel.drying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.SecadoRepository
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.formatoFechaLarga
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
import com.cafesmart.app.viewmodel.purchase.milisAFecha
import com.cafesmart.app.viewmodel.purchase.pesoEnPantalla
import com.cafesmart.app.viewmodel.sale.cantidadDesdeTexto
import com.cafesmart.app.viewmodel.sale.cantidadValida
import com.cafesmart.app.viewmodel.sale.diasDesde
import com.cafesmart.app.viewmodel.sale.errorCantidad
import com.cafesmart.app.viewmodel.sale.mensajeCantidad
import com.cafesmart.app.viewmodel.sale.resumenGrupo
import com.cafesmart.app.viewmodel.sale.textoComprado
import com.cafesmart.app.viewmodel.sale.textoSeleccionados
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Modelo de lo que muestra la vista (textos ya listos), igual que en Ventas.

// Un sublote tal como lo muestra el paso 1
data class SubloteSecadoEnPantalla(
    val sublote: SubloteConLote,
    // "Sublote 3"
    val titulo: String,
    // "60 kg disponibles · Comprado hace 3 días"
    val detalle: String,
    val elegido: Boolean,
    // Cantidad que se manda a secar, como se ve en la tarjeta elegida: "60 kg"
    val cantidadTexto: String
)

// Un grupo del paso 1: la etiqueta de una calidad y sus sublotes
data class GrupoSecado(
    val calidad: Calidad,
    // "Calidad buena"
    val etiqueta: String,
    // "2 sublotes · 200 kg"
    val resumen: String,
    val sublotes: List<SubloteSecadoEnPantalla>
)

// Hoja "Ajustar cantidad" (se abre con "Cambiar"). subloteId es el sublote que se ajusta y texto lo que se va
// escribiendo, ya limpio y con punto decimal ("12.5"); se guarda aunque sea invalido para mostrarlo con su error.
data class AjusteCantidad(
    val subloteId: Long,
    val texto: String
)

// La hoja "Ajustar cantidad" tal como la muestra la vista
data class AjusteEnPantalla(
    // "Sublote 1"
    val titulo: String,
    // "120 kg"
    val disponibleTexto: String,
    // Lo que muestra el campo, con coma decimal: "12,5"
    val texto: String,
    // "Debe ser menor o igual a 120 kg." (ayuda siempre visible; si la cantidad pasa, sale tambien como error)
    val ayuda: String,
    val error: String?,
    // true si lo escrito sirve (mayor a 0 y sin pasar de lo disponible): activa "Confirmar"
    val valido: Boolean
)

// Estado de los pasos 1 a 3 de Secado.
// Los datos se guardan arriba; los valores con get() son lo que la vista muestra, ya calculados aqui,
// para que la vista no tenga logica.
data class SecadoUiState(
    // Sublotes con cafe (de todos los tipos); solo los verdes se pueden secar. Vienen del repositorio.
    val inventario: List<SubloteConLote> = emptyList(),
    // Sublotes elegidos y cuantos kg de cada uno se mandan a secar (al elegirlo se manda todo lo disponible)
    val elegidos: List<SubloteElegido> = emptyList(),
    // Hoja "Ajustar cantidad": null mientras esta cerrada
    val ajuste: AjusteCantidad? = null,
    // Hoy y la fecha de inicio (por defecto hoy); "Cambiar" en el paso 2 la modifica
    val hoy: Fecha = fechaDeHoy(),
    val fechaInicio: Fecha = hoy,
    // true cuando se inicia el secado: evita iniciarlo dos veces
    val secadoIniciado: Boolean = false,
    // true mientras el repositorio guarda el inicio: tambien evita iniciarlo dos veces (doble toque)
    val iniciando: Boolean = false,
    // Mensaje bajo "Iniciar secado" cuando el repositorio rechazo el inicio; null si no hay error. Se limpia al reintentar.
    val errorAlIniciar: String? = null,
    // true hasta que el repositorio entrega el inventario por primera vez
    val cargando: Boolean = false
) {
    // ---------- Paso 1: elegir el cafe ----------

    private val sublotes: List<SubloteConLote>
        get() = sublotesParaSecar(inventario)

    private fun estaElegido(item: SubloteConLote): Boolean = elegidos.any { it.id == item.sublote.id }

    // Mensaje cuando no hay cafe verde para secar (null si si hay)
    val mensajeSinCafe: String?
        get() = if (!cargando && sublotes.isEmpty()) MENSAJE_SIN_CAFE_VERDE else null

    // "Seleccionar todo": "4 de 4 seleccionados"
    val textoSeleccionarTodo: String
        get() = textoSeleccionados(sublotes.count { estaElegido(it) }, sublotes.size)

    val todosElegidos: Boolean
        get() = sublotes.isNotEmpty() && sublotes.all { estaElegido(it) }

    // Algunos pero no todos: la casilla muestra un "-"
    val algunosElegidos: Boolean
        get() = !todosElegidos && sublotes.any { estaElegido(it) }

    val grupos: List<GrupoSecado>
        get() = sublotes.groupBy { it.calidad }.map { (calidad, items) ->
            GrupoSecado(
                calidad = calidad,
                etiqueta = "Calidad ${calidad.etiqueta.lowercase()}",
                resumen = resumenGrupo(items.size, items.sumOf { it.sublote.pesoDisponibleKg }),
                sublotes = items.map { aPantalla(it) }
            )
        }

    private fun aPantalla(item: SubloteConLote): SubloteSecadoEnPantalla {
        val id = item.sublote.id
        return SubloteSecadoEnPantalla(
            sublote = item,
            titulo = "Sublote $id",
            detalle = "${formatoKg(item.sublote.pesoDisponibleKg)} disponibles · " +
                textoComprado(diasDesde(item.sublote.fechaIngreso, hoy)),
            elegido = estaElegido(item),
            cantidadTexto = formatoKg(elegidos.firstOrNull { it.id == id }?.cantidadKg ?: 0.0)
        )
    }

    // La hoja "Ajustar cantidad", lista para mostrar; null si esta cerrada
    val ajusteEnPantalla: AjusteEnPantalla?
        get() {
            val enEdicion = ajuste ?: return null
            val elegido = elegidos.firstOrNull { it.id == enEdicion.subloteId } ?: return null
            val cantidad = cantidadDesdeTexto(enEdicion.texto)
            return AjusteEnPantalla(
                titulo = "Sublote ${elegido.id}",
                disponibleTexto = formatoKg(elegido.disponibleKg),
                texto = pesoEnPantalla(enEdicion.texto),
                ayuda = mensajeCantidad(elegido.disponibleKg),
                error = errorCantidad(cantidad, elegido.disponibleKg),
                valido = cantidadValida(cantidad, elegido.disponibleKg)
            )
        }

    // "Total a secar": 290 kg
    val totalKg: Double
        get() = totalASecarKg(elegidos)

    val totalKgTexto: String
        get() = formatoKg(totalKg)

    // "Continuar" del paso 1
    val puedeContinuar: Boolean
        get() = cafeListoParaSecar(elegidos)

    // Texto sobre "Continuar" que dice que falta (null cuando ya se puede continuar)
    val mensajePendientePaso1: String?
        get() = mensajeSinElegir(elegidos)

    // ---------- Pasos 2 y 3: confirmar e iniciado ----------

    val calidadesElegidas: List<Calidad>
        get() = calidadesDe(elegidos)

    // "4 sublotes"
    val sublotesElegidosTexto: String
        get() = textoSublotes(elegidos.size)

    // "Hoy, 3 de junio de 2026" (paso 2) o "3 de junio de 2026" si no es hoy
    val fechaTexto: String
        get() = formatoFechaLarga(fechaInicio, hoy)

    // "3 de junio de 2026" (paso 3)
    val fechaResumenTexto: String
        get() = formatoFecha(fechaInicio)

    val fechaMilis: Long
        get() = fechaAMilis(fechaInicio)

    // "Iniciar secado": el paso 1 esta completo y el secado no se ha iniciado ya
    val puedeIniciar: Boolean
        get() = !secadoIniciado && !iniciando && puedeContinuar
}

// ViewModel (MVVM): aqui esta la logica de los pasos 1 a 3 de Secado.
// Guarda el estado en un StateFlow y recibe las acciones de la vista.
class SecadoViewModel(private val repositorio: SecadoRepository) : ViewModel() {

    // _uiState es privado: solo el ViewModel puede cambiar el estado.
    // Empieza "cargando": el inventario llega del repositorio (suspend) un instante despues de crearse.
    private val _uiState = MutableStateFlow(SecadoUiState(cargando = true))
    // uiState es de solo lectura: la vista lo observa y se redibuja cuando cambia.
    val uiState: StateFlow<SecadoUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inventario = repositorio.inventario(), cargando = false)
        }
    }

    // ---------------- Paso 1: elegir el cafe ----------------

    // "Seleccionar todo": si ya estan todos elegidos los quita; si no, elige todos (a los ya elegidos
    // les respeta la cantidad que tenian).
    fun alternarTodos() {
        val estado = _uiState.value
        val verdes = sublotesParaSecar(estado.inventario)
        _uiState.value = if (estado.todosElegidos) {
            estado.copy(elegidos = emptyList())
        } else {
            estado.copy(elegidos = verdes.map { item ->
                estado.elegidos.firstOrNull { it.id == item.sublote.id } ?: SubloteElegido(item, item.sublote.pesoDisponibleKg)
            })
        }
    }

    // Marca o desmarca un sublote. Al marcarlo se manda todo lo que tiene disponible.
    fun alternarSublote(sublote: SubloteConLote) {
        val estado = _uiState.value
        val id = sublote.sublote.id
        val nuevos = if (estado.elegidos.any { it.id == id }) {
            // filterNot deja todos los sublotes menos el desmarcado.
            estado.elegidos.filterNot { it.id == id }
        } else {
            estado.elegidos + SubloteElegido(sublote, sublote.sublote.pesoDisponibleKg)
        }
        _uiState.value = estado.copy(elegidos = nuevos)
    }

    // "Cambiar": abre la hoja "Ajustar cantidad" con la cantidad elegida. Si el sublote no esta elegido no hace nada.
    fun abrirAjuste(subloteId: Long) {
        val estado = _uiState.value
        val elegido = estado.elegidos.firstOrNull { it.id == subloteId } ?: return
        _uiState.value = estado.copy(ajuste = AjusteCantidad(subloteId, textoDeCantidad(elegido.cantidadKg)))
    }

    // Lo que se escribe en el campo de la hoja. Se limpia como el peso de Compras (coma decimal, hasta 3 decimales).
    // La cantidad elegida no cambia hasta pulsar "Confirmar".
    fun escribirAjuste(texto: String) {
        val estado = _uiState.value
        val ajuste = estado.ajuste ?: return
        _uiState.value = estado.copy(ajuste = ajuste.copy(texto = limpiarPeso(texto)))
    }

    // "Confirmar": guarda la cantidad escrita y cierra la hoja. Si no es valida no cambia nada.
    fun confirmarAjuste() {
        val estado = _uiState.value
        val ajuste = estado.ajuste ?: return
        val elegido = estado.elegidos.firstOrNull { it.id == ajuste.subloteId } ?: return
        val cantidad = cantidadDesdeTexto(ajuste.texto)
        if (cantidad == null || !cantidadValida(cantidad, elegido.disponibleKg)) return
        _uiState.value = estado.copy(
            elegidos = estado.elegidos.map { if (it.id == ajuste.subloteId) it.copy(cantidadKg = cantidad) else it },
            ajuste = null
        )
    }

    // "Cancelar" (o cerrar la hoja): descarta lo escrito y deja la cantidad como estaba.
    fun cancelarAjuste() {
        _uiState.value = _uiState.value.copy(ajuste = null)
    }

    // ---------------- Paso 2: confirmar el inicio ----------------

    // El calendario entrega milisegundos; aqui se convierten a Fecha
    fun cambiarFecha(millis: Long) {
        _uiState.value = _uiState.value.copy(fechaInicio = milisAFecha(millis))
    }

    // "Iniciar secado": el repositorio descuenta lo enviado de cada sublote y el estado queda como iniciado.
    // Si falta algo, o ya se inicio (o se esta iniciando), no hace nada (asi no se puede iniciar dos veces).
    // onIniciado se llama solo cuando el repositorio guardo el secado con exito.
    fun iniciarSecado(onIniciado: () -> Unit = {}) {
        val estado = _uiState.value
        if (!estado.puedeIniciar) return
        _uiState.value = estado.copy(iniciando = true, errorAlIniciar = null)
        viewModelScope.launch {
            try {
                repositorio.iniciarSecado(estado.fechaInicio, estado.elegidos.map { EnvioASecar(it.id, it.cantidadKg) })
            } catch (_: IllegalArgumentException) {
                // El repositorio rechazo el envio: no se inicio nada, se puede volver a intentar y no se avisa exito.
                _uiState.value = _uiState.value.copy(iniciando = false, errorAlIniciar = MENSAJE_NO_SE_PUDO_INICIAR_SECADO)
                return@launch
            }
            // Se conservan los elegidos para el resumen del paso 3; el inventario ya trae lo que quedo.
            _uiState.value = _uiState.value.copy(
                inventario = repositorio.inventario(), ajuste = null, secadoIniciado = true, iniciando = false
            )
            onIniciado()
        }
    }

    // Empieza un secado desde cero (al cancelarlo o despues de iniciarlo): sin sublotes elegidos y con el inventario al dia.
    // onListo se llama cuando el estado ya quedo reiniciado.
    fun reiniciar(onListo: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = SecadoUiState(inventario = repositorio.inventario())
            onListo()
        }
    }
}
