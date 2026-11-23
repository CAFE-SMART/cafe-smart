package com.cafesmart.app.viewmodel.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.finanzas.diasEntre
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.FACTOR_RENDIMIENTO_MAX
import com.cafesmart.app.data.model.FACTOR_RENDIMIENTO_MIN
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.HUMEDAD_MAX_PORCENTAJE
import com.cafesmart.app.data.model.HUMEDAD_MIN_PORCENTAJE
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.purchase.formatoKg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado de "Inventario · Detalle del sublote" (solo lectura). Los textos ya vienen formateados:
 * la pantalla solo los dibuja.
 * - [cargando]: true hasta que Room entrega la primera lectura (evita parpadear "no existe").
 * - [noEncontrado]: el id de la ruta no es un número o no hay un sublote con ese id.
 */
data class DetalleSubloteUiState(
    val cargando: Boolean = true,
    val noEncontrado: Boolean = false,
    val detalle: DetalleSublote? = null,
)

/** Los datos que muestra la pantalla, ya convertidos a texto. */
data class DetalleSublote(
    val id: Long,
    val codigo: String,        // "Sublote #12"
    val tipo: String,          // "Verde"
    val calidad: String,       // "Regular"
    val calidadTipo: Calidad,  // para el color de la etiqueta de calidad
    val pesoInicial: String,   // "1.000 kg"
    val pesoActual: String,    // "120,5 kg"
    val pesoActualBultos: String, // "3,0 bultos"
    val precioPorKg: String,   // "$ 14.000"
    val valorEnBodega: String, // "$ 1.687.000"
    val etiquetaFecha: String, // "Fecha de compra" (o "Fecha de ingreso" si el sublote nació de un secado)
    val fecha: String,         // "3 de junio de 2026"
    val tiempoEnBodega: String, // "Hoy", "1 día", "12 días"
    val humedad: String,       // "12 %" o "Sin dato"
    val rangoHumedad: String,  // "Entre 8 y 15 %"
    val factorRendimiento: String, // "95" o "Sin dato"
    val rangoFactor: String,   // "Entre 70 y 140"
    val humedadConDato: Boolean,
    val factorConDato: Boolean,
    val agotado: Boolean,
)

// 0 -> "Hoy", 1 -> "1 día", 12 -> "12 días". Una fecha futura cuenta como "Hoy".
fun textoTiempoEnBodega(ingreso: Fecha, hoy: Fecha): String = when (val dias = diasEntre(ingreso, hoy)) {
    in Int.MIN_VALUE..0 -> "Hoy"
    1 -> "1 día"
    else -> "$dias días"
}

// 12.0 -> "12 %", 12.5 -> "12,5 %"
private fun textoHumedad(valor: Double): String =
    (if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString().replace('.', ',')) + " %"

private fun enteros(valor: Double) = valor.toInt()

// Texto de ayuda bajo "Humedad" / "Factor de rendimiento" (Figma: "Sin dato · entre 8 y 14 %").
// Con dato muestra solo el rango; sin dato antepone "Sin dato · " y el rango queda en minúscula.
fun subtituloDatoCalidad(rango: String, conDato: Boolean): String =
    if (conDato) rango else "Sin dato · ${rango.replaceFirstChar { it.lowercase() }}"

/** Del modelo al texto de la pantalla. Función pura para probarla sin Room. */
fun armarDetalle(
    item: SubloteConLote,
    pesoBultoKg: Double = KG_POR_BULTO,
    hoy: Fecha = fechaDeHoy(),
): DetalleSublote {
    val s = item.sublote
    return DetalleSublote(
        id = s.id,
        codigo = "Sublote #${s.id}",
        tipo = item.tipoCafe.etiqueta,
        calidad = item.calidad.etiqueta,
        calidadTipo = item.calidad,
        pesoInicial = formatoKg(redondearKg(s.pesoInicialKg)),
        pesoActual = formatoKg(redondearKg(s.pesoDisponibleKg)),
        pesoActualBultos = formatoBultos(redondearKg(s.pesoDisponibleKg), pesoBultoKg),
        precioPorKg = formatoDinero(s.costoKg.toDouble()),
        valorEnBodega = formatoDinero(s.valorEnBodega.toDouble()),
        etiquetaFecha = if (s.compraId != null) "Fecha de compra" else "Fecha de ingreso",
        fecha = formatoFecha(s.fechaIngreso),
        tiempoEnBodega = textoTiempoEnBodega(s.fechaIngreso, hoy),
        humedad = s.humedadPorcentaje?.let(::textoHumedad) ?: "Sin dato",
        rangoHumedad = "Entre ${enteros(HUMEDAD_MIN_PORCENTAJE)} y ${enteros(HUMEDAD_MAX_PORCENTAJE)} %",
        factorRendimiento = s.factorRendimiento?.toString() ?: "Sin dato",
        rangoFactor = "Entre $FACTOR_RENDIMIENTO_MIN y $FACTOR_RENDIMIENTO_MAX",
        humedadConDato = s.humedadPorcentaje != null,
        factorConDato = s.factorRendimiento != null,
        agotado = s.estaAgotado,
    )
}

// Lee un sublote de Room a través del repositorio y se actualiza solo cuando cambia el inventario
// (por eso la pantalla no tiene botón de recargar). [batchId] es el texto que llega en la ruta.
// Con [negocios] y [uid] usa los kg por bulto configurados en el negocio (como Inventario); sin ellos, KG_POR_BULTO.
class DetalleSubloteViewModel(
    inventario: InventoryRepository,
    batchId: String,
    negocios: NegocioRepository? = null,
    uid: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleSubloteUiState())
    val uiState: StateFlow<DetalleSubloteUiState> = _uiState

    // Última lectura de Room y kg por bulto vigentes; cualquiera de los dos que cambie vuelve a armar el detalle.
    private var leido = false
    private var item: SubloteConLote? = null
    private var pesoBulto = KG_POR_BULTO

    init {
        val id = batchId.toLongOrNull()
        if (id == null) {
            _uiState.value = DetalleSubloteUiState(cargando = false, noEncontrado = true)
        } else {
            viewModelScope.launch {
                inventario.observarSublote(id).collect {
                    leido = true
                    item = it
                    publicar()
                }
            }
            if (negocios != null && uid != null) {
                viewModelScope.launch {
                    negocios.observar(uid).collect { negocio ->
                        pesoBulto = negocio?.pesoBultoKg ?: KG_POR_BULTO
                        publicar()
                    }
                }
            }
        }
    }

    private fun publicar() {
        if (!leido) return // Todavía no llegó la primera lectura de Room: se sigue mostrando "cargando".
        val actual = item
        _uiState.value = if (actual == null) {
            DetalleSubloteUiState(cargando = false, noEncontrado = true)
        } else {
            DetalleSubloteUiState(cargando = false, detalle = armarDetalle(actual, pesoBulto))
        }
    }
}
