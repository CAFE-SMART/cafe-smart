package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.finanzas.diasEntre
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.formatoKg
import java.util.Locale
import kotlin.math.roundToLong

// "Inventario · Sublotes del grupo": totales y filas ya con el texto listo. Funciones puras (se prueban sin Room).

data class FilaSublote(
    val id: Long,
    val nombre: String,       // "Sublote #12" (igual que en el detalle)
    val antiguedad: String,   // "Hoy", "Hace 2 días"
    val fechaEntrada: String, // "3 de junio de 2026"
    val peso: String,
    val bultos: String,
    val costoKg: String,
    val valor: String,
    val humedad: String,      // "12 %" o "sin dato"
    val factor: String,       // "95" o "sin dato"
)

data class GrupoSublotes(
    val tipo: TipoCafe,
    val calidad: Calidad,
    val pesoTotal: String,
    val bultos: String,
    val cantidad: Int,
    val valorTotal: String,
    val costoPromedio: String,  // ponderado por los kg disponibles
    val masAntiguo: String,
    val humedadPromedio: String, // promedio simple de los sublotes que tienen dato
    val filas: List<FilaSublote>,
)

fun textoAntiguedad(ingreso: Fecha, hoy: Fecha): String = when (val dias = diasEntre(ingreso, hoy)) {
    in Int.MIN_VALUE..0 -> "Hoy"
    1 -> "Hace 1 día"
    else -> "Hace $dias días"
}

// 12.0 -> "12 %", 12.25 -> "12,3 %"
private fun porcentajeHumedad(valor: Double): String =
    String.format(Locale.US, "%.1f", valor).removeSuffix(".0").replace('.', ',') + " %"

/** Devuelve null si el lote no existe o ya no tiene café: la pantalla muestra el estado vacío. */
fun armarGrupo(
    lotes: List<LoteConSublotes>,
    loteId: Long,
    orden: OrdenInventario,
    pesoBultoKg: Double,
    hoy: Fecha,
): GrupoSublotes? {
    val items = sublotesDelLote(lotes, loteId, orden)
    val lote = items.firstOrNull()?.lote ?: return null
    val sublotes = items.map { it.sublote }
    val kg = redondearKg(sublotes.sumOf { it.pesoDisponibleKg })
    val valor = sublotes.sumOf { it.valorEnBodega }
    val costoPromedio = (sublotes.sumOf { it.pesoDisponibleKg * it.costoKg } / kg).roundToLong()
    val humedades = sublotes.mapNotNull { it.humedadPorcentaje }
    return GrupoSublotes(
        tipo = lote.tipoCafe,
        calidad = lote.calidad,
        pesoTotal = formatoKg(kg),
        bultos = formatoBultos(kg, pesoBultoKg),
        cantidad = sublotes.size,
        valorTotal = formatoDinero(valor.toDouble()),
        costoPromedio = formatoDinero(costoPromedio.toDouble()) + " por kg",
        masAntiguo = textoAntiguedad(sublotes.minByOrNull { it.fechaIngreso.clave() }!!.fechaIngreso, hoy),
        humedadPromedio = if (humedades.isEmpty()) "Sin dato" else porcentajeHumedad(humedades.average()),
        filas = sublotes.map { filaDe(it, pesoBultoKg, hoy) },
    )
}

private fun filaDe(s: Sublote, pesoBultoKg: Double, hoy: Fecha): FilaSublote {
    val kg = redondearKg(s.pesoDisponibleKg)
    return FilaSublote(
        id = s.id,
        nombre = "Sublote #${s.id}",
        antiguedad = textoAntiguedad(s.fechaIngreso, hoy),
        fechaEntrada = formatoFecha(s.fechaIngreso),
        peso = formatoKg(kg),
        bultos = formatoBultos(kg, pesoBultoKg),
        costoKg = formatoDinero(s.costoKg.toDouble()),
        valor = formatoDinero(s.valorEnBodega.toDouble()),
        humedad = s.humedadPorcentaje?.let(::porcentajeHumedad) ?: "sin dato",
        factor = s.factorRendimiento?.toString() ?: "sin dato",
    )
}
