package com.cafesmart.app.data.inventario

import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.purchase.SubloteDaoFalso
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// OrigenOcupacionRoom con un DAO falso: suma el peso disponible de los sublotes con existencia.
class OrigenOcupacionRoomTest {

    private val dao = SubloteDaoFalso()
    private val origen = OrigenOcupacionRoom(dao)

    private fun sublote(disponibleKg: Double) = SubloteEntity(
        loteId = 1, compraId = 1,
        pesoInicialKg = 1_000.0, pesoDisponibleKg = disponibleKg,
        costoKg = 14_000, fechaIngreso = 0L,
    )

    private suspend fun guardar(vararg disponibles: Double) = disponibles.forEach { dao.insertar(sublote(it)) }

    @Test fun sinSublotesLaBodegaEstaVacia() = runBlocking {
        assertEquals(0.0, origen.kgOcupados(), 0.0)
    }

    @Test fun sumaElPesoDisponibleDeTodosLosSublotes() = runBlocking {
        guardar(500.0, 334.0)
        assertEquals(834.0, origen.kgOcupados(), 0.0)
    }

    @Test fun losSublotesAgotadosNoSuman() = runBlocking {
        // 0 kg y 0,0004 kg (un resto de decimales) cuentan como agotados.
        guardar(500.0, 334.0, 0.0, 0.0004)
        assertEquals(834.0, origen.kgOcupados(), 0.0)
    }

    @Test fun cuentaElPesoQueQuedaNoElInicial() = runBlocking {
        guardar(120.5)   // entró con 1.000 kg y quedan 120,5
        assertEquals(120.5, origen.kgOcupados(), 0.0)
    }
}
