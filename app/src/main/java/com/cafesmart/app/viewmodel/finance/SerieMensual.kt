package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.model.Fecha

// Lógica de las "Gráficas financieras" (ciclo 3, tarea de Amelia, orden 12): ingresos, gastos y utilidad de los
// últimos meses. Funciones puras, sin Compose ni Android, para probarlas con JUnit.
//
// BLOQUEADO (no se implementa la interfaz): falta el diseño del MVP de Andrés. Cuando exista, la gráfica (Compose
// Canvas, sin librerías externas) se dibuja dentro del Resumen financiero, que ya está protegido por la clave
// financiera: esta lógica no abre ni salta esa protección; solo recibe los datos que FinanzasRepository ya entrega.
//
// Mismas reglas que el resumen (resumirPeriodo): cada venta aporta sus ingresos y su utilidad calculados con
// calcularUtilidad; "gastos" son los gastos registrados ese mes. Todo en pesos enteros.

/** Un mes de la serie (mes 1 = enero). */
data class MesFinanciero(
    val anio: Int,
    val mes: Int,
    val ingresosPesos: Long,
    val gastosPesos: Long,
    val utilidadPesos: Long,
) {
    val sinDatos: Boolean get() = ingresosPesos == 0L && gastosPesos == 0L && utilidadPesos == 0L
}

private val NOMBRES_MES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

/** Etiqueta corta del eje: "Nov". Si la serie cruza de año, la del primer mes de cada año lleva el año: "Ene 2027". */
fun etiquetaMes(m: MesFinanciero, conAnio: Boolean = false): String =
    NOMBRES_MES[m.mes - 1] + if (conAnio) " ${m.anio}" else ""

/** Los [meses] meses que terminan en el de [hoy], del más antiguo al actual (cruza de año sin problema). */
fun ultimosMeses(hoy: Fecha, meses: Int): List<Pair<Int, Int>> {
    require(meses >= 1) { "meses debe ser al menos 1" }
    val indiceHoy = hoy.anio * 12 + (hoy.mes - 1)
    return (indiceHoy - meses + 1..indiceHoy).map { i -> (i / 12) to (i % 12 + 1) }
}

/**
 * Serie mensual de los últimos [meses] meses hasta el de [hoy]. Los meses sin movimientos salen en 0 (la gráfica
 * no salta meses). Lo que cae fuera de la ventana (antes o después) no cuenta.
 */
fun serieMensual(
    ventas: List<VentaRegistrada>,
    gastos: List<GastoRegistrado>,
    hoy: Fecha,
    meses: Int = 6,
): List<MesFinanciero> {
    val ventasPorMes = ventas.groupBy { it.fecha.anio to it.fecha.mes }
    val gastosPorMes = gastos.groupBy { it.fecha.anio to it.fecha.mes }
    return ultimosMeses(hoy, meses).map { clave ->
        val calculos = ventasPorMes[clave].orEmpty().map {
            calcularUtilidad(it.kgVendidos, it.precioVentaKg, it.precioCompraKg, it.gastos)
        }
        MesFinanciero(
            anio = clave.first,
            mes = clave.second,
            ingresosPesos = calculos.sumOf { it.ingresosPesos },
            gastosPesos = gastosPorMes[clave].orEmpty().sumOf { it.montoPesos },
            utilidadPesos = calculos.sumOf { it.utilidadPesos },
        )
    }
}

/** Escala del eje vertical: el mayor valor absoluto de la serie (mínimo 1 para no dividir por 0 sin datos). */
fun maximoDeLaSerie(serie: List<MesFinanciero>): Long =
    serie.maxOfOrNull { maxOf(it.ingresosPesos, it.gastosPesos, kotlin.math.abs(it.utilidadPesos)) }?.coerceAtLeast(1L) ?: 1L
