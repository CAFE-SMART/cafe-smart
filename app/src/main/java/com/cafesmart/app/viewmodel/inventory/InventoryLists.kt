package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe

// Lo que necesitan las otras dos vistas de Inventario, calculado igual que el resumen: funciones puras que
// reciben los lotes con sus sublotes (los que entrega InventoryRepository) y devuelven datos listos para dibujar.
//
//   Resumen (Todos)          → resumirInventario(...)           (InventorySummary.kt)
//   Lista por tipo           → resumirCalidades(...)            (aquí) ruta inventory_type/{tipo}
//   Sublotes del grupo       → sublotesDelLote(...)             (aquí) ruta inventory_list/{loteId}
//
// Los textos y totales de "Sublotes del grupo" se arman en SublotesGrupo.kt.
// "Grupo" = un lote, es decir, un tipo de café con una calidad (por ejemplo "Verde · Buena").

// Una fila de "Lista por tipo": una calidad de ese tipo con lo que hay en bodega.
// ultimoIngreso es la fecha del sublote más reciente (null si la calidad no tiene sublotes con café).
data class ResumenCalidad(
    val lote: Lote,
    val pesoKg: Double,
    val cantidadSublotes: Int,
    val ultimoIngreso: Fecha?
) {
    val calidad: Calidad get() = lote.calidad
}

// Las calidades que existen de un tipo de café (solo las que ya se compraron alguna vez: un lote se crea con
// la primera compra). Mismas reglas de orden que el resumen: por la fecha del último ingreso y las calidades
// sin café al final; si dos empatan se queda el orden de Calidad (Buena, Regular, Mala).
fun resumirCalidades(
    lotes: List<LoteConSublotes>,
    tipo: TipoCafe,
    orden: OrdenInventario = OrdenInventario.MAS_RECIENTE,
): List<ResumenCalidad> {
    val filas = lotes
        .filter { it.lote.tipoCafe == tipo }
        .sortedBy { it.lote.calidad.ordinal }
        .map { grupo ->
            val sublotes = grupo.sublotesConExistencia
            ResumenCalidad(
                lote = grupo.lote,
                pesoKg = redondearKg(sublotes.sumOf { it.pesoDisponibleKg }),
                cantidadSublotes = sublotes.size,
                ultimoIngreso = sublotes.map { it.fechaIngreso }.maxByOrNull { it.clave() }
            )
        }
    val (conCafe, sinCafe) = filas.partition { it.ultimoIngreso != null }
    val ordenadas = when (orden) {
        OrdenInventario.MAS_RECIENTE -> conCafe.sortedByDescending { it.ultimoIngreso?.clave() ?: 0 }
        OrdenInventario.MAS_ANTIGUO -> conCafe.sortedBy { it.ultimoIngreso?.clave() ?: 0 }
    }
    return ordenadas + sinCafe
}

// Los sublotes con café de un grupo (lote), cada uno con su lote para tener tipo y calidad a la mano.
// Orden por fecha de ingreso; si dos entraron el mismo día, el más nuevo (id mayor) cuenta como más reciente.
// Si el lote no existe devuelve una lista vacía.
fun sublotesDelLote(
    lotes: List<LoteConSublotes>,
    loteId: Long,
    orden: OrdenInventario = OrdenInventario.MAS_RECIENTE,
): List<SubloteConLote> {
    val grupo = lotes.firstOrNull { it.lote.id == loteId } ?: return emptyList()
    val sublotes = grupo.sublotesConExistencia.map { SubloteConLote(it, grupo.lote) }
    val porFechaYId = compareBy<SubloteConLote>({ it.sublote.fechaIngreso.clave() }, { it.sublote.id })
    return when (orden) {
        OrdenInventario.MAS_RECIENTE -> sublotes.sortedWith(porFechaYId.reversed())
        OrdenInventario.MAS_ANTIGUO -> sublotes.sortedWith(porFechaYId)
    }
}
