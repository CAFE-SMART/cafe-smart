package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.model.*
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Borrador de presentación: conserva el texto inválido para explicar el error sin transformarlo. */
data class GastoFormulario(
    val concepto: String = "",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: Fecha = hoyEnColombia(),
    val tipo: TipoGasto = TipoGasto.TRANSPORTE,
    val estado: EstadoPago = EstadoPago.PAGADO,
    val aplicaA: AplicaGasto = AplicaGasto.GENERAL,
    // El usuario decide cada asignación: no se infiere un reparto por peso ni partes iguales.
    val asignaciones: Map<Long, String> = emptyMap()
)

/** Errores por sección del formulario; una validación correcta no contiene mensajes. */
data class GastoErrores(
    val concepto: String? = null,
    val monto: String? = null,
    val fecha: String? = null,
    val sublotes: String? = null
) {
    val hayErrores get() = listOf(concepto, monto, fecha, sublotes).any { it != null }
}

/** Filtros exclusivos de la lista; no cambian el total pendiente global. */
enum class FiltroGasto(val etiqueta: String) { PENDIENTES("Pendientes"), PAGADOS("Pagados"), TODOS("Todos") }

/** Etapas del registro; el guardado bloquea edición, cierre y doble envío. */
enum class ResultadoGasto { EDICION, CONFIRMACION, GUARDANDO, EXITO, ERROR }

/** Estado observable compartido por las rutas de Gastos durante la sesión de navegación. */
data class GastosUiState(
    val formulario: GastoFormulario = GastoFormulario(),
    val errores: GastoErrores = GastoErrores(),
    val gastos: List<Gasto> = emptyList(),
    val sublotes: List<Sublote> = emptyList(),
    val filtro: FiltroGasto = FiltroGasto.PENDIENTES,
    val resultado: ResultadoGasto = ResultadoGasto.EDICION
) {
    val visibles get() = gastos.filter {
        when (filtro) {
            FiltroGasto.PENDIENTES -> it.estadoPago == EstadoPago.PENDIENTE
            FiltroGasto.PAGADOS -> it.estadoPago == EstadoPago.PAGADO
            FiltroGasto.TODOS -> true
        }
    }
    // BigInteger evita desbordar Long al sumar varios gastos individualmente válidos.
    val totalPendiente get() = gastos.filter { it.estadoPago == EstadoPago.PENDIENTE }
        .fold(BigInteger.ZERO) { total, gasto -> total + BigInteger.valueOf(gasto.monto) }
}

/** API 24: Calendar sustituye a java.time sin depender de desugaring. */
fun hoyEnColombia(): Fecha = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")).let {
    Fecha(it.get(Calendar.DAY_OF_MONTH), it.get(Calendar.MONTH) + 1, it.get(Calendar.YEAR))
}

/** Representación COP sin centavos: el modelo oficial almacena pesos enteros. */
fun dineroGasto(valor: Number): String = "$ ${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(valor)}"

/** Fecha del modelo oficial, sin convertirla a UTC ni desplazar el día. */
fun fechaGasto(fecha: Fecha): String = String.format(Locale.ROOT, "%02d/%02d/%04d", fecha.dia, fecha.mes, fecha.anio)
