package com.cafesmart.app.data.model

import com.cafesmart.app.data.purchase.Fecha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

//Este test se proporciona con el fin de probar el funcionamiento del modelo de datos propuestos ante estos casos.
class ModeloDatosTest {

    private val hoy = Fecha(3, 11, 2026)

    private fun sublote(id: Long, kg: Double, disponible: Double = kg) = Sublote(
        id = id, loteId = 1, compraId = 1,
        pesoInicialKg = kg, pesoDisponibleKg = disponible,
        costoKg = 14_000, fechaIngreso = hoy
    )

    @Test
    fun mermaDelSecado() {
        val verde = sublote(1, 290.0)
        val seco = sublote(2, 270.0).copy(compraId = null, secadoId = 1)
        val detalle = SecadoConDetalle(
            secado = Secado(id = 1, fechaInicio = hoy),
            enviados = listOf(EnvioSecado(SecadoSublote(1, 1, 290.0), verde)),
            resultantes = listOf(seco)
        )
        assertEquals(20.0, detalle.mermaKg, 1e-9)
        assertEquals(6.9, detalle.mermaPorcentaje, 0.05)
        assertEquals(280_000L, detalle.valorMerma)
    }

    @Test
    fun subloteAgotado() {
        assertTrue(sublote(1, 100.0, disponible = 0.0001).estaAgotado)
        assertFalse(sublote(1, 100.0).estaAgotado)
    }

    @Test
    fun valorEnBodega() {
        assertEquals(1_400_000L, sublote(1, 100.0).valorEnBodega)
    }

    @Test
    fun subtotalDeVenta() {
        assertEquals(500_000L, VentaSublote(1, 1, 50.0, 10_000).subtotal)
    }
}