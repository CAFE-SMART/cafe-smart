package com.cafesmart.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de las reglas que el modelo valida al construir sus objetos (init con require).
class ReglasModeloTest {

    private val hoy = Fecha(3, 11, 2026)

    private fun sublote(
        compraId: Long? = 1,
        secadoId: Long? = null,
        inicial: Double = 100.0,
        disponible: Double = inicial,
        humedad: Double? = null,
        factor: Int? = null
    ) = Sublote(
        id = 1, loteId = 1, compraId = compraId, secadoId = secadoId,
        pesoInicialKg = inicial, pesoDisponibleKg = disponible, costoKg = 14_000,
        fechaIngreso = hoy, humedadPorcentaje = humedad, factorRendimiento = factor
    )

    private fun gasto(
        monto: Long = 100_000,
        aplicaA: AplicaGasto = AplicaGasto.GENERAL,
        secadoId: Long? = null,
        concepto: String = "Pago de jornaleros"
    ) = Gasto(
        id = 1, concepto = concepto, tipo = TipoGasto.OTROS, monto = monto, fecha = hoy,
        estadoPago = EstadoPago.PAGADO, aplicaA = aplicaA, secadoId = secadoId
    )

    private fun falla(bloque: () -> Unit): Boolean =
        try { bloque(); false } catch (e: IllegalArgumentException) { true }

    // ── Sublote ──────────────────────────────────────────────

    @Test
    fun subloteDeCompraOSecadoEsValido() {
        sublote(compraId = 1, secadoId = null)
        sublote(compraId = null, secadoId = 2)
    }

    @Test
    fun subloteNoPuedeVenirDeCompraYDeSecado() {
        assertTrue(falla { sublote(compraId = 1, secadoId = 2) })
    }

    @Test
    fun subloteExigePesosCoherentes() {
        assertTrue(falla { sublote(inicial = 0.0) })
        assertTrue(falla { sublote(disponible = -1.0) })
        assertTrue(falla { sublote(inicial = 100.0, disponible = 101.0) })
        // El margen EPSILON_KG tolera errores de redondeo de Double.
        sublote(inicial = 100.0, disponible = 100.0 + EPSILON_KG / 2)
    }

    @Test
    fun humedadDebeEstarEntre8y15() {
        sublote(humedad = 8.0)
        sublote(humedad = 11.5)
        sublote(humedad = 15.0)
        assertTrue(falla { sublote(humedad = 7.9) })
        assertTrue(falla { sublote(humedad = 15.1) })
    }

    @Test
    fun factorDebeEstarEntre70y140() {
        sublote(factor = 70)
        sublote(factor = 100)
        sublote(factor = 140)
        assertTrue(falla { sublote(factor = 69) })
        assertTrue(falla { sublote(factor = 141) })
    }

    // ── Secado ───────────────────────────────────────────────

    @Test
    fun secadoEnCursoNoTieneFechaFin() {
        Secado(id = 1, fechaInicio = hoy)
        Secado(id = 1, fechaInicio = hoy, fechaFin = hoy, estado = EstadoSecado.FINALIZADO)
        assertTrue(falla { Secado(id = 1, fechaInicio = hoy, fechaFin = hoy) })
    }

    @Test
    fun factorDelSecadoDebeEstarEntre70y140() {
        Secado(id = 1, fechaInicio = hoy, factorRendimiento = 70)
        assertTrue(falla { Secado(id = 1, fechaInicio = hoy, factorRendimiento = 141) })
    }

    @Test
    fun secadoSubloteExigeKgPositivos() {
        assertTrue(falla { SecadoSublote(1, 1, 0.0) })
    }

    // ── Gasto ────────────────────────────────────────────────

    @Test
    fun gastoSecadoExigeSecadoId() {
        gasto(aplicaA = AplicaGasto.SECADO, secadoId = 5)
        assertTrue(falla { gasto(aplicaA = AplicaGasto.SECADO, secadoId = null) })
    }

    @Test
    fun gastoNoSecadoNoPuedeTenerSecadoId() {
        assertTrue(falla { gasto(aplicaA = AplicaGasto.GENERAL, secadoId = 5) })
        assertTrue(falla { gasto(aplicaA = AplicaGasto.SUBLOTES, secadoId = 5) })
    }

    @Test
    fun gastoExigeMontoYConcepto() {
        assertTrue(falla { gasto(monto = 0) })
        assertTrue(falla { gasto(monto = -1) })
        assertTrue(falla { gasto(concepto = "  ") })
    }

    @Test
    fun repartoDeGastoDebeSumarElMonto() {
        val g = gasto(monto = 100_000, aplicaA = AplicaGasto.SUBLOTES)
        val completo = listOf(GastoSublote(1, 1, 60_000), GastoSublote(1, 2, 40_000))
        val incompleto = listOf(GastoSublote(1, 1, 60_000), GastoSublote(1, 2, 30_000))
        val deOtroGasto = listOf(GastoSublote(9, 1, 100_000))

        assertTrue(g.repartoEsValido(completo))
        assertFalse(g.repartoEsValido(incompleto))
        assertFalse(g.repartoEsValido(deOtroGasto))
        assertFalse(g.repartoEsValido(emptyList()))
    }

    @Test
    fun gastoGeneralNoLlevaReparto() {
        val g = gasto(aplicaA = AplicaGasto.GENERAL)

        assertTrue(g.repartoEsValido(emptyList()))
        assertFalse(g.repartoEsValido(listOf(GastoSublote(1, 1, 100_000))))
    }

    // ── Compra y Venta ───────────────────────────────────────

    @Test
    fun totalesNoPuedenSerNegativos() {
        assertTrue(falla { Compra(fecha = hoy, totalPagado = -1) })
        assertTrue(falla { Venta(fecha = hoy, totalRecibido = -1) })
    }

    @Test
    fun ventaSubloteExigeKgYPrecioPositivos() {
        assertTrue(falla { VentaSublote(1, 1, 0.0, 10_000) })
        assertTrue(falla { VentaSublote(1, 1, 10.0, 0) })
        assertEquals(100_000L, VentaSublote(1, 1, 10.0, 10_000).subtotal)
    }
}
