package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.inventario.InventarioFalso
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Detalle del sublote con un repositorio falso (sin Room): que muestre los datos del sublote pedido,
// que se actualice solo y que un id inválido o inexistente dé "no encontrado".
class DetalleSubloteViewModelTest {

    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private fun sublote(id: Long, loteId: Long, inicial: Double, disponible: Double) = Sublote(
        id = id, loteId = loteId, compraId = id,
        pesoInicialKg = inicial, pesoDisponibleKg = disponible,
        costoKg = 14_000, fechaIngreso = Fecha(3, 6, 2026),
    )

    private fun inventario(disponible: Double = 120.5) = listOf(
        LoteConSublotes(
            Lote(1, TipoCafe.VERDE, Calidad.REGULAR),
            listOf(sublote(id = 7, loteId = 1, inicial = 1_000.0, disponible = disponible)),
        ),
        LoteConSublotes(
            Lote(2, TipoCafe.SECO, Calidad.BUENA),
            listOf(sublote(id = 8, loteId = 2, inicial = 50.0, disponible = 50.0)),
        ),
    )

    @Test fun muestraLosDatosDelSubloteDeLaRuta() {
        val vm = DetalleSubloteViewModel(InventarioFalso(inventario()), "7")
        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertFalse(estado.noEncontrado)
        val d = estado.detalle!!
        assertEquals("Sublote #7", d.codigo)
        assertEquals("Verde", d.tipo)
        assertEquals("Regular", d.calidad)
        assertEquals("1.000 kg", d.pesoInicial)
        assertEquals("120,5 kg", d.pesoActual)
        assertEquals("$ 14.000", d.precioPorKg)
        assertEquals("3 de junio de 2026", d.fecha)
        assertFalse(d.agotado)
    }

    @Test fun elValorEnBodegaEsKgDisponiblesPorPrecio() {
        val d = DetalleSubloteViewModel(InventarioFalso(inventario()), "7").uiState.value.detalle!!
        // 120,5 kg x 14.000 = 1.687.000
        assertEquals("$ 1.687.000", d.valorEnBodega)
    }

    @Test fun seActualizaSoloCuandoCambiaElInventario() {
        val falso = InventarioFalso(inventario(disponible = 120.5))
        val vm = DetalleSubloteViewModel(falso, "7")
        assertEquals("120,5 kg", vm.uiState.value.detalle!!.pesoActual)
        falso.lotes.value = inventario(disponible = 100.0)
        assertEquals("100 kg", vm.uiState.value.detalle!!.pesoActual)
    }

    @Test fun unSubloteAgotadoSigueAbriendoSuDetalle() {
        val vm = DetalleSubloteViewModel(InventarioFalso(inventario(disponible = 0.0)), "7")
        val d = vm.uiState.value.detalle!!
        assertTrue(d.agotado)
        assertEquals("0 kg", d.pesoActual)
    }

    @Test fun unIdQueNoExisteDaNoEncontrado() {
        val estado = DetalleSubloteViewModel(InventarioFalso(inventario()), "999").uiState.value
        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrado)
        assertNull(estado.detalle)
    }

    @Test fun unIdQueNoEsNumeroDaNoEncontrado() {
        val estado = DetalleSubloteViewModel(InventarioFalso(inventario()), "abc").uiState.value
        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrado)
    }

    @Test fun conInventarioVacioDaNoEncontrado() {
        val estado = DetalleSubloteViewModel(InventarioFalso(emptyList()), "1").uiState.value
        assertTrue(estado.noEncontrado)
        assertNotNull(estado)
    }

    // ── Campos del Figma: tiempo en bodega y datos de calidad ──

    private fun item(humedad: Double? = null, factor: Int? = null, compraId: Long? = 7, secadoId: Long? = null) =
        com.cafesmart.app.data.model.SubloteConLote(
            Sublote(
                id = 7, loteId = 1, compraId = compraId, secadoId = secadoId,
                pesoInicialKg = 100.0, pesoDisponibleKg = 100.0, costoKg = 14_000,
                fechaIngreso = Fecha(3, 6, 2026), humedadPorcentaje = humedad, factorRendimiento = factor,
            ),
            Lote(1, TipoCafe.VERDE, Calidad.REGULAR),
        )

    @Test fun tiempoEnBodegaDiceHoyUnDiaOVariosDias() {
        val ingreso = Fecha(3, 6, 2026)
        assertEquals("Hoy", textoTiempoEnBodega(ingreso, Fecha(3, 6, 2026)))
        assertEquals("1 día", textoTiempoEnBodega(ingreso, Fecha(4, 6, 2026)))
        assertEquals("12 días", textoTiempoEnBodega(ingreso, Fecha(15, 6, 2026)))
        // Una fecha de ingreso futura (reloj del celular atrasado) no muestra días negativos.
        assertEquals("Hoy", textoTiempoEnBodega(Fecha(10, 6, 2026), Fecha(3, 6, 2026)))
    }

    @Test fun sinHumedadNiFactorMuestraSinDatoYElRangoDelModelo() {
        val d = armarDetalle(item(), hoy = Fecha(3, 6, 2026))
        assertEquals("Sin dato", d.humedad)
        assertEquals("Sin dato", d.factorRendimiento)
        assertEquals("Entre 8 y 15 %", d.rangoHumedad)
        assertEquals("Entre 70 y 140", d.rangoFactor)
        assertEquals("Hoy", d.tiempoEnBodega)
    }

    @Test fun conHumedadYFactorLosMuestra() {
        val d = armarDetalle(item(humedad = 12.5, factor = 95), hoy = Fecha(3, 6, 2026))
        assertEquals("12,5 %", d.humedad)
        assertEquals("95", d.factorRendimiento)
        assertEquals("12 %", armarDetalle(item(humedad = 12.0), hoy = Fecha(3, 6, 2026)).humedad)
    }

    @Test fun laEtiquetaDeFechaDependeDeSiNacioDeUnaCompraODeUnSecado() {
        assertEquals("Fecha de compra", armarDetalle(item(), hoy = Fecha(3, 6, 2026)).etiquetaFecha)
        assertEquals(
            "Fecha de ingreso",
            armarDetalle(item(compraId = null, secadoId = 4), hoy = Fecha(3, 6, 2026)).etiquetaFecha,
        )
    }
}
