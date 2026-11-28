package com.cafesmart.app.viewmodel.drying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SecadoConDetalle
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.secado.SecadoRepository
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.formatoFechaLarga
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
import com.cafesmart.app.viewmodel.purchase.pesoEnPantalla
import com.cafesmart.app.viewmodel.sale.cantidadDesdeTexto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// Pasos 5 y 6 de Secado: datos de finalizacion (kilos secos por calidad, factor, fecha, gastos) y resumen final.
// Un solo ViewModel para los dos pasos: el paso 6 se muestra con el resultado que guarda el paso 5.

// "6,9 %" (la merma del Figma lleva un decimal; formatoPorcentaje de Inventario redondea a entero)
fun formatoMermaPorcentaje(porcentaje: Double): String =
    String.format(Locale.US, "%.1f", porcentaje).replace('.', ',') + " %"

// Cifras del resumen de un secado, con los textos listos. Se arman siempre desde un SecadoConDetalle,
// asi la merma, su porcentaje y su valor salen de las formulas del modelo y no se repiten aqui.
data class ResumenSecado(
    // "290 kg"
    val entradaTexto: String,
    // "270 kg"
    val salidaSecaTexto: String,
    // "20 kg"
    val mermaKgTexto: String,
    // "6,9 %"
    val mermaPorcentajeTexto: String,
    // "$ 280.000"
    val valorMermaTexto: String
)

fun resumenDe(detalle: SecadoConDetalle): ResumenSecado = ResumenSecado(
    entradaTexto = formatoKg(detalle.pesoEnviadoKg),
    salidaSecaTexto = formatoKg(detalle.pesoSecoKg),
    mermaKgTexto = formatoKg(detalle.mermaKg),
    mermaPorcentajeTexto = formatoMermaPorcentaje(detalle.mermaPorcentaje),
    valorMermaTexto = formatoDinero(detalle.valorMerma.toDouble())
)

// Una calidad que resulto del secado (Paso 6): "Calidad buena", "140 kg"
data class CalidadSecada(val calidad: Calidad, val kg: Double) {
    val etiqueta: String get() = "Calidad ${calidad.etiqueta.lowercase()}"
    val kgTexto: String get() = formatoKg(kg)
}

data class FinalizarSecadoUiState(
    // El secado en curso que se finaliza; null si ya no esta en curso (o no existe).
    val secado: SecadoConDetalle? = null,
    // true hasta que el repositorio entrega el secado: mientras tanto la pantalla no dice "ya no esta en curso".
    val cargando: Boolean = false,
    // Lo que se escribe, ya limpio y con punto decimal ("12.5"); vacio = 0.
    val kilosBuena: String = "",
    val kilosRegular: String = "",
    val kilosMala: String = "",
    // Solo digitos; si se escribe una coma queda "92.5" para poder decir "Escribe solo números."
    val factorTexto: String = "",
    val hoy: Fecha = fechaDeHoy(),
    val fechaFin: Fecha = hoy,
    // "¿Hubo gastos?": false = "No". Solo decide a donde se navega despues; el secado se finaliza igual.
    val huboGastos: Boolean = false,
    // Lo que devolvio el repositorio al finalizar; no es null desde que se confirma (Paso 6).
    val resultado: SecadoConDetalle? = null,
    val calidadesSecadas: List<CalidadSecada> = emptyList(),
    // true desde que se pulsa "Confirmar" (bloquea el doble toque); vuelve a false si el repositorio rechaza.
    val bloqueado: Boolean = false,
    // Mensaje del repositorio si rechazo la finalizacion (no deberia pasar si puedeConfirmar fue true).
    val errorAlConfirmar: String? = null
) {
    // ---------- Paso 5: datos ----------

    fun kilosTexto(calidad: Calidad): String = when (calidad) {
        Calidad.BUENA -> kilosBuena
        Calidad.REGULAR -> kilosRegular
        Calidad.MALA -> kilosMala
    }

    // Lo que muestra el campo, con coma decimal: "12,5"
    fun kilosEnPantalla(calidad: Calidad): String = pesoEnPantalla(kilosTexto(calidad))

    val factorEnPantalla: String
        get() = factorTexto.replace('.', ',')

    val kilosSecos: Map<Calidad, Double>
        // El texto ya viene limpio (punto decimal): no se pasa otra vez por limpiarPeso, que tomaria el punto como de miles.
        get() = Calidad.entries.associateWith { cantidadDesdeTexto(kilosTexto(it)) ?: 0.0 }

    val kilosSecosTotalKg: Double
        get() = kilosSecosTotal(kilosSecos)

    // "270 kg" (total de las tres calidades)
    val kilosSecosTotalTexto: String
        get() = formatoKg(kilosSecosTotalKg)

    private val sinKilosEscritos: Boolean
        get() = Calidad.entries.all { kilosTexto(it).isEmpty() }

    private val errorDeKilos: String?
        get() = secado?.let { errorKilosSecos(kilosSecos, it.pesoEnviadoKg) }

    val kilosValidos: Boolean
        get() = secado != null && errorDeKilos == null

    // Con los tres campos vacios no se muestra error (solo "Confirmar" sigue apagado).
    val errorKilos: String?
        get() = if (sinKilosEscritos) null else errorDeKilos

    // "Entre los tres, máximo 290 kg." siempre visible como ayuda
    val ayudaKilos: String?
        get() = secado?.let { ayudaKilosSecos(it.pesoEnviadoKg) }

    val errorFactor: String?
        get() = errorFactor(factorTexto)

    val ayudaFactor: String
        get() = AYUDA_FACTOR

    val factorEsValido: Boolean
        get() = factorValido(factorTexto)

    val errorFecha: String?
        get() = secado?.let { errorFechaFin(it.secado.fechaInicio, fechaFin, hoy) }

    val fechaValida: Boolean
        get() = secado != null && errorFecha == null

    // "Hoy, 10 de junio de 2026" o "10 de junio de 2026"
    val fechaFinTexto: String
        get() = formatoFechaLarga(fechaFin, hoy)

    val fechaMilis: Long
        get() = fechaAMilis(fechaFin)

    // Secado con los sublotes secos que saldrian con lo escrito (provisionales: no estan en el inventario).
    // Es el mismo SecadoConDetalle del resultado, asi el resumen del Paso 5 y el del Paso 6 usan las mismas formulas.
    // null si los kilos no son validos (la merma no se puede calcular).
    private val detalleProvisional: SecadoConDetalle?
        get() {
            val actual = secado ?: return null
            if (!kilosValidos) return null
            val total = kilosSecosTotalKg
            val costoKg = costoKgSeco(actual.enviados, total)
            val provisionales = kilosSecos.mapNotNull { (_, kg) ->
                val redondeado = redondearKg(kg)
                if (redondeado <= 0.0) null else Sublote(
                    loteId = 0,
                    secadoId = actual.secado.id,
                    pesoInicialKg = redondeado,
                    pesoDisponibleKg = redondeado,
                    costoKg = costoKg,
                    fechaIngreso = fechaFin
                )
            }
            return actual.copy(resultantes = provisionales)
        }

    // Resumen del Paso 5 (entrada, salida seca, merma); null mientras los kilos no sean validos.
    val resumen: ResumenSecado?
        get() = detalleProvisional?.let { resumenDe(it) }

    // "Confirmar": todo valido, sin resultado todavia y sin otro toque en proceso.
    val puedeConfirmar: Boolean
        get() = secado != null && resultado == null && !bloqueado &&
            kilosValidos && factorEsValido && fechaValida

    // ---------- Paso 6: resumen final ----------

    val finalizado: Boolean
        get() = resultado != null

    val resumenFinal: ResumenSecado?
        get() = resultado?.let { resumenDe(it) }

    // "10 de junio de 2026"
    val fechaFinResumenTexto: String?
        get() = resultado?.secado?.fechaFin?.let { formatoFecha(it) }
}

class FinalizarSecadoViewModel(
    private val repositorio: SecadoRepository,
    private val secadoId: Long,
    hoy: Fecha = fechaDeHoy()
) : ViewModel() {

    // Empieza "cargando": el secado llega del repositorio (suspend) un instante despues de crearse.
    private val _uiState = MutableStateFlow(FinalizarSecadoUiState(cargando = true, hoy = hoy))
    val uiState: StateFlow<FinalizarSecadoUiState> = _uiState

    init {
        viewModelScope.launch {
            val secado = repositorio.secadosEnCurso().firstOrNull { it.secado.id == secadoId }
            _uiState.value = _uiState.value.copy(secado = secado, cargando = false)
        }
    }

    // Despues de finalizar no se puede cambiar nada.
    private fun editable(): Boolean = _uiState.value.resultado == null && !_uiState.value.bloqueado

    // Se limpia como el peso de Compras (coma decimal, hasta 3 decimales).
    fun escribirKilos(calidad: Calidad, texto: String) {
        if (!editable()) return
        val limpio = limpiarPeso(texto)
        val estado = _uiState.value
        _uiState.value = when (calidad) {
            Calidad.BUENA -> estado.copy(kilosBuena = limpio)
            Calidad.REGULAR -> estado.copy(kilosRegular = limpio)
            Calidad.MALA -> estado.copy(kilosMala = limpio)
        }
    }

    // El factor es entero: con limpiarPeso solo quedan digitos (y un decimal si se escribe coma, que da error).
    fun escribirFactor(texto: String) {
        if (!editable()) return
        _uiState.value = _uiState.value.copy(factorTexto = limpiarPeso(texto))
    }

    fun cambiarFecha(fecha: Fecha) {
        if (!editable()) return
        _uiState.value = _uiState.value.copy(fechaFin = fecha)
    }

    fun elegirGastos(huboGastos: Boolean) {
        if (!editable()) return
        _uiState.value = _uiState.value.copy(huboGastos = huboGastos)
    }

    // "Confirmar": el repositorio crea los sublotes secos y deja el secado FINALIZADO; el resultado queda en el estado
    // (con gastos o sin ellos). Si falta algo, o ya se finalizo, no hace nada (asi no se finaliza dos veces).
    // onConfirmado se llama solo cuando el secado quedo finalizado; si el repositorio lo rechaza no se llama.
    fun confirmar(onConfirmado: () -> Unit = {}) {
        val estado = _uiState.value
        if (!estado.puedeConfirmar) return
        val secado = estado.secado ?: return
        // Opcional: con el campo vacio queda null y se guarda sin factor.
        val factor = factorDesdeTexto(estado.factorTexto)
        _uiState.value = estado.copy(bloqueado = true, errorAlConfirmar = null)
        viewModelScope.launch {
            try {
                val kilos = estado.kilosSecos
                val resultado = repositorio.finalizarSecado(
                    secadoId = secado.secado.id,
                    fechaFin = estado.fechaFin,
                    kilosSecos = kilos,
                    factorRendimiento = factor
                )
                _uiState.value = _uiState.value.copy(
                    resultado = resultado,
                    calidadesSecadas = Calidad.entries
                        .map { CalidadSecada(it, redondearKg(kilos.getValue(it))) }
                        .filter { it.kg > 0 }
                )
            } catch (e: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(bloqueado = false, errorAlConfirmar = e.message)
                return@launch
            }
            onConfirmado()
        }
    }
}
