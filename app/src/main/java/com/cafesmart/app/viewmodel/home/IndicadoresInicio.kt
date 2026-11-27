package com.cafesmart.app.viewmodel.home

import com.cafesmart.app.data.finanzas.CompraRegistrada
import com.cafesmart.app.data.finanzas.MovimientoFinanciero
import com.cafesmart.app.data.finanzas.TipoMovimiento
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.finanzas.armarMovimientos
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.viewmodel.finance.calcularUtilidad
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.purchase.milisAFecha

// Inicio: indicadores del día y movimientos recientes (ciclo 3, Amelia, orden 11). Funciones puras para JUnit.
// Reutilizan resumirDia (compras del día) y armarMovimientos (el mismo armado del Historial financiero): no hay
// otra lógica financiera. Las ventas se leen solo si su fuente está conectada (PR #43): sin ella los indicadores de
// ventas quedan en null y la pantalla debe mostrar "—", nunca un 0 que parezca real.

/** Cuántos movimientos muestra "Movimientos recientes". */
const val MOVIMIENTOS_EN_INICIO = 5

/** Ventas de hoy. null en [IndicadoresDelDia.ventas] = la fuente de ventas todavía no está conectada. */
data class VentasDelDia(val ventas: Int, val kgVendidos: Double, val ingresosPesos: Long)

data class IndicadoresDelDia(
    val compras: ComprasDelDia,
    /** Lo que se pagó hoy por las compras, en pesos. */
    val pagadoComprasPesos: Long,
    val ventas: VentasDelDia?,
)

/**
 * Indicadores de [hoy]. El día es el día del calendario con que se guardó cada operación (la misma conversión
 * que Compras): una compra de hoy a las 23:59 cuenta hoy y una de ayer no.
 * [ventas] null = fuente no conectada; lista vacía = conectada y sin ventas hoy (entonces sí es 0).
 */
fun indicadoresDelDia(
    lotes: List<LoteConSublotes>,
    compras: List<CompraEntity>,
    ventas: List<VentaRegistrada>?,
    hoy: Fecha,
): IndicadoresDelDia {
    val deHoy = compras.distinctBy { it.id }.filter { milisAFecha(it.fecha) == hoy }
    return IndicadoresDelDia(
        compras = resumirDia(lotes, deHoy, hoy),
        pagadoComprasPesos = deHoy.sumOf { it.totalPagado },
        ventas = ventas?.filter { it.fecha == hoy }?.let { v ->
            VentasDelDia(
                ventas = v.size,
                kgVendidos = redondearKg(v.sumOf { it.kgVendidos }),
                ingresosPesos = v.sumOf { calcularUtilidad(it.kgVendidos, it.precioVentaKg, it.precioCompraKg, it.gastos).ingresosPesos },
            )
        },
    )
}

/**
 * Las compras como las ve el Historial: fecha, total pagado, productor ("Sin registrar" si no tiene) y el detalle
 * del café ("Café verde · 120 kg"; varios cafés: "2 cafés · 200 kg").
 */
fun comprasRegistradas(
    compras: List<CompraEntity>,
    lotes: List<LoteConSublotes>,
    nombresProductores: Map<Long, String>,
): List<CompraRegistrada> {
    val porCompra = lotes.flatMap { g -> g.sublotes.map { g.lote to it } }.groupBy { it.second.compraId }
    return compras.distinctBy { it.id }.sortedByDescending { it.id }.map { compra ->
        val cafes = porCompra[compra.id].orEmpty()
        val kg = redondearKg(cafes.sumOf { it.second.pesoInicialKg })
        val kgTexto = (if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString().replace('.', ',')) + " kg"
        val detalle = when (cafes.size) {
            0 -> kgTexto
            1 -> "Café ${cafes[0].first.tipoCafe.etiqueta.lowercase()} · $kgTexto"
            else -> "${cafes.size} cafés · $kgTexto"
        }
        CompraRegistrada(
            fecha = milisAFecha(compra.fecha),
            totalPesos = compra.totalPagado,
            productor = compra.productorId?.let { nombresProductores[it] ?: "Productor no disponible" } ?: "Sin registrar",
            detalle = detalle,
        )
    }
}

/**
 * "Movimientos recientes": compras y ventas, la más reciente primero, sin repetir. Sale de armarMovimientos (mismo
 * orden y textos que el Historial); con la misma fecha queda primero la compra más nueva. Sin ventas conectadas,
 * solo compras. Los gastos no entran (Inicio ya tiene "Ver gastos").
 */
fun movimientosRecientes(
    compras: List<CompraRegistrada>,
    ventas: List<VentaRegistrada>?,
    cuantos: Int = MOVIMIENTOS_EN_INICIO,
): List<MovimientoFinanciero> =
    armarMovimientos(ventas.orEmpty(), compras, emptyList())
        .filter { it.tipo == TipoMovimiento.COMPRA || it.tipo == TipoMovimiento.VENTA }
        .take(cuantos)
