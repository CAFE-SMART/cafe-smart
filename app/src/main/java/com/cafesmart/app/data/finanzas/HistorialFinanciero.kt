package com.cafesmart.app.data.finanzas

import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.viewmodel.finance.calcularUtilidad

// Historial financiero (Figma "Finanzas · Historial financiero"): compras, ventas y gastos en una sola lista,
// de lo más reciente a lo más antiguo, con filtro por tipo y agrupada por día. Todo es lógica pura (sin Android).

enum class TipoMovimiento { COMPRA, VENTA, GASTO }

/**
 * Una fila del historial.
 * [montoPesos] lleva el signo desde el punto de vista del negocio: las ventas suman (+) y las compras y los
 * gastos restan (−), por eso son negativos.
 */
data class MovimientoFinanciero(
    val tipo: TipoMovimiento,
    val fecha: Fecha,
    val titulo: String,
    val detalle: String,
    val montoPesos: Long,
)

/** Las cuatro opciones del filtro: Todos, Compras, Ventas y Gastos. */
enum class FiltroMovimiento(val etiqueta: String, val tipo: TipoMovimiento?) {
    TODOS("Todos", null),
    COMPRAS("Compras", TipoMovimiento.COMPRA),
    VENTAS("Ventas", TipoMovimiento.VENTA),
    GASTOS("Gastos", TipoMovimiento.GASTO),
}

/** Un día con sus movimientos, para mostrar "Hoy · 2 de junio de 2026" y debajo sus filas. */
data class GrupoDia(val etiqueta: String, val movimientos: List<MovimientoFinanciero>)

/**
 * Mezcla ventas, compras y gastos en una lista ordenada de la fecha más reciente a la más antigua.
 * Con la misma fecha quedan primero las ventas, luego las compras y luego los gastos (la ordenación es estable).
 */
fun armarMovimientos(
    ventas: List<VentaRegistrada>,
    compras: List<CompraRegistrada>,
    gastos: List<GastoRegistrado>,
): List<MovimientoFinanciero> {
    val deVentas = ventas.map {
        MovimientoFinanciero(
            tipo = TipoMovimiento.VENTA,
            fecha = it.fecha,
            titulo = "Venta · ${it.cliente}",
            detalle = it.detalle,
            montoPesos = calcularUtilidad(it.kgVendidos, it.precioVentaKg, it.precioCompraKg, it.gastos).ingresosPesos,
        )
    }
    val deCompras = compras.map {
        MovimientoFinanciero(TipoMovimiento.COMPRA, it.fecha, "Compra · ${it.productor}", it.detalle, -it.totalPesos)
    }
    val deGastos = gastos.map {
        MovimientoFinanciero(TipoMovimiento.GASTO, it.fecha, "Gasto · ${it.concepto}", it.detalle, -it.montoPesos)
    }
    return (deVentas + deCompras + deGastos).sortedByDescending { claveDeFecha(it.fecha) }
}

/** Deja solo los movimientos del tipo elegido; con "Todos" devuelve la lista completa. */
fun filtrarMovimientos(movimientos: List<MovimientoFinanciero>, filtro: FiltroMovimiento): List<MovimientoFinanciero> =
    if (filtro.tipo == null) movimientos else movimientos.filter { it.tipo == filtro.tipo }

/** Agrupa por día (en el orden en que vienen) y le pone a cada día su título. */
fun agruparPorDia(movimientos: List<MovimientoFinanciero>, hoy: Fecha): List<GrupoDia> =
    movimientos.groupBy { it.fecha }.map { (fecha, delDia) -> GrupoDia(etiquetaDeDia(fecha, hoy), delDia) }

/** "Hoy · 2 de junio de 2026", "Ayer · 1 de junio de 2026" o, para días anteriores, solo la fecha. */
fun etiquetaDeDia(fecha: Fecha, hoy: Fecha): String {
    val texto = "${fecha.dia} de ${MESES[fecha.mes - 1]} de ${fecha.anio}"
    return when (diasEntre(fecha, hoy)) {
        0 -> "Hoy · $texto"
        1 -> "Ayer · $texto"
        else -> texto
    }
}

/** Mensaje cuando no hay nada que mostrar: distinto si no hay ningún movimiento o si el filtro no encontró. */
fun mensajeHistorialVacio(filtro: FiltroMovimiento): String = when (filtro) {
    FiltroMovimiento.TODOS ->
        "Todavía no hay movimientos. Cuando registres una compra, una venta o un gasto, aparecerá aquí."
    FiltroMovimiento.COMPRAS -> "Todavía no hay compras registradas."
    FiltroMovimiento.VENTAS -> "Todavía no hay ventas registradas."
    FiltroMovimiento.GASTOS -> "Todavía no hay gastos registrados."
}

// Año, mes y día en un solo número (20261102) para ordenar sin depender de java.time (minSdk 24).
private fun claveDeFecha(f: Fecha): Int = f.anio * 10_000 + f.mes * 100 + f.dia

private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
