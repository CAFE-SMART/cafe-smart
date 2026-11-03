package com.cafesmart.app.data.model

import com.cafesmart.app.data.model.Calidad

// LOTE: agrupación lógica de sublotes por tipo y calidad (ej. "café seco bueno", "café verde regular").
// La unidad real es el Sublote. Por eso aquí NO se guardan pesos ni totales: se calculan sumando los sublotes (ver LoteConSublotes) y así nunca quedan desactualizados.
//
// Regla: no puede haber dos lotes con el mismo (tipoCafe, calidad). Al registrar una compra se busca
// el lote de ese tipo y calidad, y si no existe se crea.
data class Lote(
    val id: Long = 0,
    val tipoCafe: TipoCafe,
    val calidad: Calidad
)

// Lote junto con sus sublotes: lo que muestra Inventario ("2 sublotes · 280 kg").
// Es una vista de lectura (no una tabla). Más adelante Room la llenará con @Relation.
data class LoteConSublotes(
    val lote: Lote,
    val sublotes: List<Sublote>
) {
    // Solo cuentan los sublotes que todavía tienen café.
    val sublotesConExistencia: List<Sublote> get() = sublotes.filterNot { it.estaAgotado }
    val cantidadSublotes: Int get() = sublotesConExistencia.size
    val pesoDisponibleKg: Double get() = sublotesConExistencia.sumOf { it.pesoDisponibleKg }
}
