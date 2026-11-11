package com.cafesmart.app.data.inventario

import com.cafesmart.app.data.sample.lotesEjemplo

// De dónde salen los kilos que hay hoy en la bodega (la ocupación). Lo usan Capacidad de bodega (para no dejar
// una capacidad menor al café guardado) y los avisos de Compras. Hoy son los de ejemplo; cuando Compras esté en
// Room, Carlos escribe OrigenOcupacionRoom (suma del peso disponible de los sublotes) y la cambia en AppNavHost:
// ni las reglas ni las pantallas cambian.

fun interface OrigenOcupacion {
    /** Kilos de café disponibles en este momento, sumando todos los sublotes. */
    suspend fun kgOcupados(): Double
}

/** Los kilos de los lotes de ejemplo, mientras no haya inventario real en Room. */
object OcupacionDeEjemplo : OrigenOcupacion {
    override suspend fun kgOcupados(): Double =
        lotesEjemplo().sumOf { it.sublotesConExistencia.sumOf { s -> s.pesoDisponibleKg } }
}
