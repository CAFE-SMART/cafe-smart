package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.finanzas.diasEntre
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.aPesos
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoKg

// "Inventario · Detalle · Lista de sublotes" (Figma): un grupo (lote = tipo + calidad) con sus sublotes con café.
// Funciones puras sobre lo que entrega InventoryRepository, para probarlas con JUnit. Todo sale de Room: no hay
// datos de ejemplo ni ids fijos.

/** Una tarjeta de sublote de la lista, con los textos ya armados. */
data class FilaSubloteGrupo(
    val id: Long,
    val titulo: String,     // "Sublote #3"
    val antiguedad: String, // "Hoy", "Hace 1 día", "Hace 12 días"
    val peso: String,       // "120 kg"
    val bultos: String,     // "2,9 bultos"
    val costoKg: String,    // "$ 14.000"
    val valor: String,      // "$ 1.680.000"
    val humedad: String,    // "Humedad: sin dato" / "Humedad: 12 %"
    val factor: String,     // "Factor: sin dato" / "Factor: 95"
)

/** Encabezado y lista del grupo. */
data class GrupoSublotes(
    val loteId: Long,
    val tipo: TipoCafe,
    val calidad: Calidad,
    val pesoTotal: String,       // "200 kg"
    val bultosTotal: String,     // "4,9 bultos"
    val cantidad: Int,           // sublotes con café
    val valorTotal: String,      // "$ 2.800.000"
    val costoPromedio: String,   // "$ 14.000 por kg" o "Sin dato" si no queda café
    val masAntiguo: String,      // "Hace 2 días" o "Sin dato"
    val humedadPromedio: String, // "12,5 %" o "Sin dato"
    val sublotes: List<FilaSubloteGrupo>,
)

// 0 -> "Hoy", 1 -> "Hace 1 día", n -> "Hace n días". Una fecha futura cuenta como "Hoy".
fun textoAntiguedad(ingreso: Fecha, hoy: Fecha): String = when (val dias = diasEntre(ingreso, hoy)) {
    in Int.MIN_VALUE..0 -> "Hoy"
    1 -> "Hace 1 día"
    else -> "Hace $dias días"
}

// 12.0 -> "12 %", 12.5 -> "12,5 %"
private fun porcentaje(valor: Double): String {
    val r = Math.round(valor * 10) / 10.0
    return (if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().replace('.', ',')) + " %"
}

/**
 * El grupo [loteId] con sus sublotes con café, en el [orden] pedido (por defecto el más reciente primero, como el
 * Figma). Null si el lote no existe. Si el lote existe pero ya no tiene café, devuelve el grupo con la lista vacía.
 */
fun armarGrupo(
    lotes: List<LoteConSublotes>,
    loteId: Long,
    hoy: Fecha,
    pesoBultoKg: Double = KG_POR_BULTO,
    orden: OrdenInventario = OrdenInventario.MAS_RECIENTE,
): GrupoSublotes? {
    val grupo = lotes.firstOrNull { it.lote.id == loteId } ?: return null
    val conCafe = sublotesDelLote(lotes, loteId, orden).map { it.sublote }
    val kg = redondearKg(conCafe.sumOf { it.pesoDisponibleKg })
    val valor = conCafe.sumOf { it.valorEnBodega }
    val humedades = conCafe.mapNotNull { it.humedadPorcentaje }
    val masViejo = conCafe.minByOrNull { it.fechaIngreso.clave() }
    return GrupoSublotes(
        loteId = loteId,
        tipo = grupo.lote.tipoCafe,
        calidad = grupo.lote.calidad,
        pesoTotal = formatoKg(kg),
        bultosTotal = formatoBultos(kg, pesoBultoKg),
        cantidad = conCafe.size,
        valorTotal = formatoDinero(valor.toDouble()),
        // Promedio ponderado por los kg que quedan: valor en bodega ÷ kg.
        costoPromedio = if (kg > 0) "${formatoDinero((valor / kg))} por kg" else "Sin dato",
        masAntiguo = masViejo?.let { textoAntiguedad(it.fechaIngreso, hoy) } ?: "Sin dato",
        // Solo con los sublotes que tienen el dato; si ninguno lo tiene, "Sin dato" (no se inventa).
        humedadPromedio = if (humedades.isEmpty()) "Sin dato" else porcentaje(humedades.average()),
        sublotes = conCafe.map { s ->
            FilaSubloteGrupo(
                id = s.id,
                titulo = "Sublote #${s.id}",
                antiguedad = textoAntiguedad(s.fechaIngreso, hoy),
                peso = formatoKg(s.pesoDisponibleKg),
                bultos = formatoBultos(s.pesoDisponibleKg, pesoBultoKg),
                costoKg = formatoDinero(s.costoKg.toDouble()),
                valor = formatoDinero(s.valorEnBodega.toDouble()),
                humedad = "Humedad: " + (s.humedadPorcentaje?.let { porcentaje(it) } ?: "sin dato"),
                factor = "Factor: " + (s.factorRendimiento?.toString() ?: "sin dato"),
            )
        },
    )
}

/** Valor total en pesos enteros de los sublotes con café del grupo (para pruebas y para Ventas). */
fun valorDelGrupo(lotes: List<LoteConSublotes>, loteId: Long): Long =
    sublotesDelLote(lotes, loteId).sumOf { (it.sublote.pesoDisponibleKg * it.sublote.costoKg).aPesos() }
