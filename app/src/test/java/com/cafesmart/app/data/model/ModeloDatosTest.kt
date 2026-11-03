package com.cafesmart.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de los cálculos del modelo de datos (valores derivados que no se guardan).
class ModeloDatosTest {

    private val hoy = Fecha(3, 11, 2026)

    private fun sublote(id: Long, kg: Double, disponible: Double = kg, loteId: Long = 1) = Sublote(
        id = id, loteId = loteId, compraId = 1,
        pesoInicialKg = kg, pesoDisponibleKg = disponible,
        costoKg = 14_000, fechaIngreso = hoy
    )

    // ── Secado y merma ───────────────────────────────────────

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
    fun mermaSinKgEnviadosEsCero() {
        val detalle = SecadoConDetalle(
            secado = Secado(id = 1, fechaInicio = hoy),
            enviados = emptyList(),
            resultantes = emptyList()
        )
        assertEquals(0.0, detalle.mermaPorcentaje, 1e-9)
        assertEquals(0L, detalle.valorMerma)
    }

    @Test
    fun valorDeMermaUsaCostoPromedioPonderado() {
        // 100 kg a $10.000 y 300 kg a $14.000 -> costo promedio $13.000/kg. Merma: 400 - 380 = 20 kg.
        val barato = sublote(1, 100.0).copy(costoKg = 10_000)
        val caro = sublote(2, 300.0).copy(costoKg = 14_000)
        val seco = sublote(3, 380.0).copy(compraId = null, secadoId = 1)
        val detalle = SecadoConDetalle(
            secado = Secado(id = 1, fechaInicio = hoy),
            enviados = listOf(
                EnvioSecado(SecadoSublote(1, 1, 100.0), barato),
                EnvioSecado(SecadoSublote(1, 2, 300.0), caro)
            ),
            resultantes = listOf(seco)
        )
        assertEquals(260_000L, detalle.valorMerma)
    }

    // ── Sublote y Lote ───────────────────────────────────────

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
    fun loteSoloCuentaSublotesConExistencia() {
        val lote = Lote(id = 1, tipoCafe = TipoCafe.SECO, calidad = Calidad.BUENA)
        val conSublotes = LoteConSublotes(
            lote = lote,
            sublotes = listOf(
                sublote(1, 100.0),
                sublote(2, 180.0),
                sublote(3, 50.0, disponible = 0.0)   // agotado: no cuenta
            )
        )
        assertEquals(2, conSublotes.cantidadSublotes)
        assertEquals(280.0, conSublotes.pesoDisponibleKg, 1e-9)
    }

    @Test
    fun subloteConLoteExponeTipoYCalidad() {
        val lote = Lote(id = 1, tipoCafe = TipoCafe.PASILLA, calidad = Calidad.REGULAR)
        val conLote = SubloteConLote(sublote(1, 10.0), lote)
        assertEquals(TipoCafe.PASILLA, conLote.tipoCafe)
        assertEquals(Calidad.REGULAR, conLote.calidad)
    }

    // ── Compra y Venta ───────────────────────────────────────

    @Test
    fun compraSumaLosKgIniciales() {
        val compra = CompraConSublotes(
            compra = Compra(id = 1, fecha = hoy, totalPagado = 2_500_000),
            productor = null,   // compra "Sin registrar"
            sublotes = listOf(sublote(1, 100.0), sublote(2, 80.0))
        )
        assertEquals(180.0, compra.totalKg, 1e-9)
        assertNull(compra.productor)
    }

    @Test
    fun subtotalDeVenta() {
        assertEquals(500_000L, VentaSublote(1, 1, 50.0, 10_000).subtotal)
    }

    @Test
    fun ventaSumaKgYSubtotales() {
        val detalle = VentaConDetalle(
            venta = Venta(id = 1, fecha = hoy, totalRecibido = 3_500_000),
            cliente = null,   // venta "Sin registrar"
            items = listOf(
                VentaSublote(1, 1, 150.0, 20_000),   // 3.000.000
                VentaSublote(1, 2, 25.0, 20_000)     //   500.000
            )
        )
        assertEquals(175.0, detalle.totalKg, 1e-9)
        assertEquals(3_500_000L, detalle.totalCalculado)
        assertEquals(detalle.venta.totalRecibido, detalle.totalCalculado)
    }

    // ── Conversión de dinero ─────────────────────────────────

    @Test
    fun aPesosRedondeaAlPesoMasCercano() {
        assertEquals(14_000L, 14_000.0.aPesos())
        assertEquals(14_001L, 14_000.5.aPesos())
        assertEquals(14_000L, 14_000.49.aPesos())
    }

    // ── Enums ────────────────────────────────────────────────

    @Test
    fun tipoCafeSeConvierteDesdeLaEtiqueta() {
        assertEquals(TipoCafe.VERDE, TipoCafe.desdeEtiqueta("Verde"))
        assertEquals(TipoCafe.TRILLADO, TipoCafe.desdeEtiqueta("Trillado"))
        assertNull(TipoCafe.desdeEtiqueta("Desconocido"))
    }
}
