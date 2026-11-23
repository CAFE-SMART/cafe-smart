package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.inventario.InventarioFalso
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.negocio.NegocioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ── Bultos según el negocio, "Sin dato" y sublote agotado ──

    private class NegociosFalsos(inicial: NegocioEntity? = null) : NegocioRepository {
        val datos = MutableStateFlow(listOfNotNull(inicial).associateBy { it.uid })
        override suspend fun guardar(negocio: NegocioEntity) { datos.value = datos.value + (negocio.uid to negocio) }
        override fun observar(uid: String): Flow<NegocioEntity?> = datos.map { it[uid] }
    }

    private fun negocio(pesoBultoKg: Double) = NegocioEntity(
        uid = "u1", nombre = "Finca", tipo = "Finca", telefono = "3000000000",
        pesoBultoKg = pesoBultoKg, creadoEn = 0L,
    )

    @Test fun sinNegocioLosBultosUsanElPesoPorDefecto() {
        val d = DetalleSubloteViewModel(InventarioFalso(inventario(disponible = 81.4)), "7").uiState.value.detalle!!
        assertEquals("2,0 bultos", d.pesoActualBultos) // 81,4 kg / 40,7 kg por bulto
    }

    @Test fun losBultosUsanElPesoDeBultoConfiguradoEnElNegocio() {
        val negocios = NegociosFalsos(negocio(pesoBultoKg = 50.0))
        val vm = DetalleSubloteViewModel(InventarioFalso(inventario(disponible = 100.0)), "7", negocios, "u1")
        assertEquals("2,0 bultos", vm.uiState.value.detalle!!.pesoActualBultos) // 100 kg / 50 kg por bulto
        // Si se cambia el peso del bulto en Ajustes, el detalle se actualiza solo.
        negocios.datos.value = mapOf("u1" to negocio(pesoBultoKg = 25.0))
        assertEquals("4,0 bultos", vm.uiState.value.detalle!!.pesoActualBultos)
    }

    @Test fun sinNegocioGuardadoSeUsaElPesoPorDefecto() {
        val vm = DetalleSubloteViewModel(InventarioFalso(inventario(disponible = 81.4)), "7", NegociosFalsos(), "u1")
        assertEquals("2,0 bultos", vm.uiState.value.detalle!!.pesoActualBultos)
    }

    @Test fun elTextoDeAyudaDeCalidadDependeDeSiHayDato() {
        assertEquals("Sin dato · entre 8 y 15 %", subtituloDatoCalidad("Entre 8 y 15 %", conDato = false))
        assertEquals("Sin dato · entre 70 y 140", subtituloDatoCalidad("Entre 70 y 140", conDato = false))
        assertEquals("Entre 8 y 15 %", subtituloDatoCalidad("Entre 8 y 15 %", conDato = true))
    }

    @Test fun losIndicadoresDeDatoSiguenALosValoresOpcionales() {
        val sin = armarDetalle(item(), hoy = Fecha(3, 6, 2026))
        assertFalse(sin.humedadConDato)
        assertFalse(sin.factorConDato)
        val con = armarDetalle(item(humedad = 12.0, factor = 95), hoy = Fecha(3, 6, 2026))
        assertTrue(con.humedadConDato)
        assertTrue(con.factorConDato)
    }
}
