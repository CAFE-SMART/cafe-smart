package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.GastoDelSublote
import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import java.math.BigInteger

/**
 * Encabezado de "Gastos del sublote" como en el Figma ("Sublote 1 · Verde · Regular"): el número real del sublote
 * (el mismo que muestra su Detalle) y el tipo y la calidad de su lote. Si el lote todavía no llegó, solo el número.
 */
fun encabezadoGastosSublote(sublote: Sublote, lotes: List<LoteConSublotes>): String {
    val lote = lotes.firstOrNull { it.lote.id == sublote.loteId }?.lote
    return if (lote == null) "Sublote #${sublote.id}"
    else "Sublote #${sublote.id} · ${lote.tipoCafe.etiqueta} · ${lote.calidad.etiqueta}"
}

data class GastosSubloteUiState(
    val sublote: Sublote? = null,
    val gastos: List<GastoDelSublote> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val recuperable: Boolean = false
) {
    /** Suma de lo asignado a este sublote (no el monto global de cada gasto). */
    val total: BigInteger get() = sumar(gastos)

    /** Chips del Figma: "Pagado $ …" y "Pendiente $ …". Pagado + Pendiente = [total]. */
    val totalPagado: BigInteger get() = sumar(gastos.filter { it.gasto.estadoPago == EstadoPago.PAGADO })
    val totalPendiente: BigInteger get() = sumar(gastos.filter { it.gasto.estadoPago == EstadoPago.PENDIENTE })

    /** Lista de la pestaña elegida (Pendientes, Pagados o Todos). Solo filtra lo que ya está en Room. */
    fun visibles(filtro: FiltroGasto): List<GastoDelSublote> = when (filtro) {
        FiltroGasto.PENDIENTES -> gastos.filter { it.gasto.estadoPago == EstadoPago.PENDIENTE }
        FiltroGasto.PAGADOS -> gastos.filter { it.gasto.estadoPago == EstadoPago.PAGADO }
        FiltroGasto.TODOS -> gastos
    }

    private fun sumar(filas: List<GastoDelSublote>): BigInteger =
        filas.fold(BigInteger.ZERO) { suma, fila -> suma + BigInteger.valueOf(fila.montoAsignado) }
}

/** Estado vacío de cada pestaña (textos propuestos, pendientes del equipo; el de "Todos" es el de antes). */
fun mensajeSinGastos(filtro: FiltroGasto): String = when (filtro) {
    FiltroGasto.TODOS -> "Este sublote todavía no tiene gastos registrados."
    FiltroGasto.PENDIENTES -> "Este sublote no tiene gastos pendientes."
    FiltroGasto.PAGADOS -> "Este sublote no tiene gastos pagados."
}
