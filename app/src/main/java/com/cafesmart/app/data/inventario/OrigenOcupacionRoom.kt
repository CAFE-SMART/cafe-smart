package com.cafesmart.app.data.inventario

import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.model.EPSILON_KG
import com.cafesmart.app.viewmodel.inventory.redondearKg

// Kilos que hay hoy en la bodega, leídos de los sublotes guardados en Room.
// Reemplaza a OcupacionDeEjemplo en AppNavHost (Compras y Capacidad de bodega).

class OrigenOcupacionRoom(private val subloteDao: SubloteDao) : OrigenOcupacion {

    /** Suma del peso disponible de los sublotes con existencia; los agotados (EPSILON_KG o menos) no cuentan. */
    /** El resultado se redondea a gramos para coincidir con Inventario y evitar ruido de punto flotante.*/
    override suspend fun kgOcupados(): Double =
        redondearKg(
        subloteDao.listar()
            .filter { it.pesoDisponibleKg > EPSILON_KG }
            .sumOf { it.pesoDisponibleKg }
        )
}
