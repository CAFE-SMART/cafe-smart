package com.cafesmart.app.data.inventario

import com.cafesmart.app.data.sample.lotesEjemplo

// De dónde salen los kilos que hay hoy en la bodega (la ocupación). Lo usan Capacidad de bodega (para no dejar
// una capacidad menor al café guardado) y los avisos de Compras. La versión real es OrigenOcupacionRoom (suma del
// peso disponible de los sublotes en Room); OcupacionDeEjemplo queda para pruebas y vistas previas.

fun interface OrigenOcupacion {
    /** Kilos de café disponibles en este momento, sumando todos los sublotes. */
    suspend fun kgOcupados(): Double
}

/** Los kilos de los lotes de ejemplo (para pruebas y vistas previas; la app usa OrigenOcupacionRoom). */
object OcupacionDeEjemplo : OrigenOcupacion {
    override suspend fun kgOcupados(): Double =
        lotesEjemplo().sumOf { it.sublotesConExistencia.sumOf { s -> s.pesoDisponibleKg } }
}
