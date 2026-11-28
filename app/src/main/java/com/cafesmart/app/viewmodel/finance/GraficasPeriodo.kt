package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.finanzas.enElPeriodo
import com.cafesmart.app.data.finanzas.restarDias
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoGasto
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

// Gráficas del Resumen financiero (ciclo 3, Amelia): funciones puras, sin Compose, para JUnit.
// - Distribución de gastos: los gastos OPERATIVOS registrados en el periodo (Hoy o Esta semana), por categoría.
// - Balance semanal: por cada uno de los últimos 7 días, lo que entró por ventas y lo que costó lo vendido
//   (compra del café + gastos asociados), con la misma calcularUtilidad del Resumen. No se suman las compras del
//   periodo: la ganancia sale de lo vendido. Así ninguno de los dos indicadores cuenta dos veces un gasto.
// Los periodos usan enElPeriodo, la regla original del Resumen (Hoy = ese día; Esta semana = hoy y 6 días antes).

/** Una categoría de la dona. [porcentaje] suma exactamente 100 entre todas (método del mayor residuo). */
data class CategoriaGastoPeriodo(val tipo: TipoGasto, val pesos: Long, val porcentaje: Int)

data class DistribucionGastos(val totalPesos: Long, val categorias: List<CategoriaGastoPeriodo>) {
    val vacia: Boolean get() = totalPesos <= 0L
}

/** Gastos del periodo agrupados por categoría, de mayor a menor. Sin gastos: vacía (sin dividir por cero). */
fun distribucionGastos(gastos: List<GastoRegistrado>, periodo: PeriodoFinanciero, hoy: Fecha): DistribucionGastos {
    val porTipo = gastos.filter { enElPeriodo(it.fecha, periodo, hoy) && it.montoPesos > 0 }
        .groupBy { it.tipo }.mapValues { (_, g) -> g.sumOf { it.montoPesos } }
    val total = porTipo.values.sum()
    if (total <= 0L) return DistribucionGastos(0, emptyList())
    val ordenados = porTipo.entries.sortedWith(compareByDescending<Map.Entry<TipoGasto, Long>> { it.value }.thenBy { it.key.ordinal })
    val porcentajes = porcentajesEnteros(ordenados.map { it.value }, total)
    return DistribucionGastos(total, ordenados.mapIndexed { i, e -> CategoriaGastoPeriodo(e.key, e.value, porcentajes[i]) })
}

/** Porcentajes enteros que suman 100 (mayor residuo). */
fun porcentajesEnteros(valores: List<Long>, total: Long): List<Int> {
    if (total <= 0L) return valores.map { 0 }
    val exactos = valores.map { it * 100.0 / total }
    val base = exactos.map { floor(it).toInt() }.toMutableList()
    var faltan = 100 - base.sum()
    exactos.indices.sortedByDescending { exactos[it] - base[it] }.forEach { if (faltan > 0) { base[it]++; faltan-- } }
    return base
}

/** Un día del balance semanal. */
data class DiaBalance(val fecha: Fecha, val ingresosPesos: Long, val costosPesos: Long)

/** Los 7 días de "Esta semana" (de hace 6 días a hoy, en orden). Cruza de mes y de año con el calendario. */
fun diasDeLaSemana(hoy: Fecha): List<Fecha> = (6 downTo 0).map { restarDias(hoy, it) }

/** Balance de los últimos 7 días. Días sin ventas quedan en 0 (no se saltan). */
fun balanceSemanal(ventas: List<VentaRegistrada>, hoy: Fecha): List<DiaBalance> {
    val porDia = ventas.groupBy { it.fecha }
    return diasDeLaSemana(hoy).map { dia ->
        val calculos = porDia[dia].orEmpty().map { calcularUtilidad(it.kgVendidos, it.precioVentaKg, it.precioCompraKg, it.gastos) }
        DiaBalance(dia, calculos.sumOf { it.ingresosPesos }, calculos.sumOf { it.costoCompraPesos + it.gastosPesos })
    }
}

private val DIAS_CORTOS = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
private val DIAS_LARGOS = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
private val MESES_LARGOS = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
    "septiembre", "octubre", "noviembre", "diciembre")

private fun diaDeLaSemana(f: Fecha): Int = GregorianCalendar(f.anio, f.mes - 1, f.dia).get(Calendar.DAY_OF_WEEK) - 1

/** Etiqueta del eje: "Vie 27". */
fun etiquetaDia(f: Fecha): String = "${DIAS_CORTOS[diaDeLaSemana(f)]} ${f.dia}"

/** Para TalkBack: "viernes 27 de noviembre". */
fun fechaParaLeer(f: Fecha): String = "${DIAS_LARGOS[diaDeLaSemana(f)]} ${f.dia} de ${MESES_LARGOS[f.mes - 1]}"

/** Escala del eje vertical: de 0 a [tope] en 4 marcas iguales y "redondas" (250 mil, 500 mil...). */
data class EscalaMonetaria(val tope: Long, val paso: Long) {
    val marcas: List<Long> get() = (0..4).map { it * paso }
}

/** Escala que cubre [maximo] con 4 pasos de 1, 2, 2,5 o 5 × 10ⁿ. Sin datos (0): de 0 a 100 mil. */
fun escalaMonetaria(maximo: Long): EscalaMonetaria {
    if (maximo <= 0L) return EscalaMonetaria(100_000, 25_000)
    val crudo = maximo / 4.0
    val magnitud = 10.0.pow(floor(log10(crudo)))
    val paso = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitud }.first { it * 4 >= maximo }
    val pasoEntero = maxOf(1L, kotlin.math.ceil(paso).toLong())
    return EscalaMonetaria(pasoEntero * 4, pasoEntero)
}

/** Formato corto del eje: "$0", "$500", "$250 mil", "$1 M", "$1,5 M". */
fun pesosCortos(valor: Long): String {
    fun decimal(v: Double): String {
        val r = Math.round(v * 100) / 100.0
        return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().trimEnd('0').replace('.', ',')
    }
    return when {
        valor >= 1_000_000L -> "$${decimal(valor / 1_000_000.0)} M"
        valor >= 1_000L -> "$${decimal(valor / 1_000.0)} mil"
        else -> "$$valor"
    }
}
