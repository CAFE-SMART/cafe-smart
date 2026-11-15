package com.cafesmart.app.data.inventario

import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.model.EPSILON_KG

// Kilos que hay hoy en la bodega, leídos de los sublotes guardados en Room.
// Reemplaza a OcupacionDeEjemplo en AppNavHost (Compras y Capacidad de bodega).

class OrigenOcupacionRoom(private val subloteDao: SubloteDao) : OrigenOcupacion {

    /** Suma del peso disponible de los sublotes con existencia; los agotados (EPSILON_KG o menos) no cuentan. */
    override suspend fun kgOcupados(): Double =
        subloteDao.listar()
            .filter { it.pesoDisponibleKg > EPSILON_KG }
            .sumOf { it.pesoDisponibleKg }
}
