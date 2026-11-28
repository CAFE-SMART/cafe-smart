package com.cafesmart.app.viewmodel.drying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SecadoConDetalle
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.secado.SecadoRepository
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.sale.diasDesde
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Paso 4 de Secado: los secados en curso y la cancelacion de uno.

// Textos propuestos (no estan en el Figma; falta confirmarlos con el equipo).
const val MENSAJE_SIN_SECADOS_EN_CURSO = "No tienes secados en curso."
const val TITULO_CANCELAR_SECADO = "¿Cancelar este secado?"
const val CUERPO_CANCELAR_SECADO = "El café vuelve a tu inventario como estaba antes de enviarlo."
const val BOTON_CONFIRMAR_CANCELAR_SECADO = "Sí, cancelar"
const val BOTON_DESCARTAR_CANCELAR_SECADO = "No"

// "Lleva 3 días" / "Lleva 1 día" / "Empezó hoy" (propuesto)
fun textoLlevaDias(dias: Int): String = when {
    dias <= 0 -> "Empezó hoy"
    dias == 1 -> "Lleva 1 día"
    else -> "Lleva $dias días"
}

// Una tarjeta de secado en curso, con los textos ya listos.
data class SecadoEnCursoEnPantalla(
    val detalle: SecadoConDetalle,
    // "Secado del 3 de junio"
    val titulo: String,
    // "4 sublotes"
    val sublotesTexto: String,
    // Calidades que hay entre los sublotes enviados, en el orden buena, regular, mala
    val calidades: List<Calidad>,
    // "290 kg"
    val kgEnviadosTexto: String,
    // "3 de junio de 2026"
    val fechaInicioTexto: String,
    // "Lleva 2 días"
    val diasTexto: String
)

data class SecadosEnCursoUiState(
    val secados: List<SecadoConDetalle> = emptyList(),
    // Inventario del repositorio: de aqui sale la calidad de cada sublote enviado (vive en su lote).
    val inventario: List<SubloteConLote> = emptyList(),
    val hoy: Fecha = fechaDeHoy(),
    // Secado que el usuario eligio; si es null (o ya no esta) se usa el primero.
    val elegidoId: Long? = null,
    // true mientras se muestra el dialogo "¿Cancelar este secado?"
    val confirmandoCancelacion: Boolean = false,
    // true hasta que el repositorio entrega la lista por primera vez (asi no se ve "no hay secados" por un instante)
    val cargando: Boolean = false
) {
    // El elegido: el que se toco o, si no, el primero (con uno solo, ese mismo).
    val elegido: SecadoConDetalle?
        get() = secados.firstOrNull { it.secado.id == elegidoId } ?: secados.firstOrNull()

    val mensajeSinSecados: String?
        get() = if (!cargando && secados.isEmpty()) MENSAJE_SIN_SECADOS_EN_CURSO else null

    val tarjetas: List<SecadoEnCursoEnPantalla>
        get() = secados.map { aPantalla(it) }

    val tarjetaElegida: SecadoEnCursoEnPantalla?
        get() = elegido?.let { aPantalla(it) }

    private fun aPantalla(detalle: SecadoConDetalle): SecadoEnCursoEnPantalla {
        val inicio = detalle.secado.fechaInicio
        return SecadoEnCursoEnPantalla(
            detalle = detalle,
            // formatoFecha da "3 de junio de 2026"; para el titulo se quita el año.
            titulo = "Secado del ${formatoFecha(inicio).removeSuffix(" de ${inicio.anio}")}",
            sublotesTexto = textoSublotes(detalle.enviados.size),
            calidades = Calidad.entries.filter { c ->
                detalle.enviados.any { e -> inventario.any { it.sublote.id == e.sublote.id && it.calidad == c } }
            },
            kgEnviadosTexto = formatoKg(detalle.pesoEnviadoKg),
            fechaInicioTexto = formatoFecha(inicio),
            diasTexto = textoLlevaDias(diasDesde(inicio, hoy))
        )
    }
}

class SecadosEnCursoViewModel(
    private val repositorio: SecadoRepository,
    hoy: Fecha = fechaDeHoy()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecadosEnCursoUiState(hoy = hoy, cargando = true))
    val uiState: StateFlow<SecadosEnCursoUiState> = _uiState

    init {
        refrescar()
    }

    // Vuelve a leer los secados del repositorio (por ejemplo al volver a la pantalla).
    fun refrescar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                secados = repositorio.secadosEnCurso(), inventario = repositorio.inventario(), cargando = false
            )
        }
    }

    // Elige una tarjeta. Si el id no es de un secado en curso no hace nada.
    fun elegirSecado(id: Long) {
        val estado = _uiState.value
        if (estado.secados.none { it.secado.id == id }) return
        _uiState.value = estado.copy(elegidoId = id)
    }

    // "Cancelar secado": abre el dialogo. Sin secado elegido no hace nada.
    fun pedirCancelar() {
        val estado = _uiState.value
        if (estado.elegido == null) return
        _uiState.value = estado.copy(confirmandoCancelacion = true)
    }

    // "No": cierra el dialogo.
    fun descartarCancelar() {
        _uiState.value = _uiState.value.copy(confirmandoCancelacion = false)
    }

    // "Sí, cancelar": el repositorio devuelve el cafe al inventario y la lista se vuelve a leer.
    // Si el dialogo no esta abierto no hace nada (asi no se cancela dos veces: el dialogo se cierra al empezar).
    // onCancelado se llama solo cuando el repositorio cancelo con exito y la lista ya se volvio a leer.
    fun confirmarCancelar(onCancelado: () -> Unit = {}) {
        val estado = _uiState.value
        val id = estado.elegido?.secado?.id ?: return
        if (!estado.confirmandoCancelacion) return
        _uiState.value = estado.copy(confirmandoCancelacion = false)
        viewModelScope.launch {
            // Si el repositorio dice que ya no esta en curso, igual se refresca la lista (y no se avisa exito).
            val exito = try {
                repositorio.cancelarSecado(id)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
            _uiState.value = _uiState.value.copy(
                secados = repositorio.secadosEnCurso(),
                inventario = repositorio.inventario(),
                elegidoId = null
            )
            if (exito) onCancelado()
        }
    }
}
