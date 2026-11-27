package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.finanzas.FinanzasLocal
import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.finanzas.OrigenFinanciero
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoGasto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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

// Resumen financiero rediseñado (ciclo 3): distribución de gastos (GD) y balance semanal (GB) con datos en memoria.
class GraficasFinancierasViewModelTest {
    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private val hoy = Fecha(27, 11, 2026)
    private fun gasto(f: Fecha, pesos: Long, tipo: TipoGasto) = GastoRegistrado(f, pesos, tipo = tipo)
    private fun venta(f: Fecha, kg: Double, venta: Double, compra: Double, gastos: Double = 0.0) = VentaRegistrada(f, kg, venta, compra, gastos)

    private class Origen(
        var g: List<GastoRegistrado> = emptyList(),
        var v: List<VentaRegistrada> = emptyList(),
        val conectadas: Boolean = true,
        var falla: Boolean = false,
    ) : OrigenFinanciero {
        override val ventasConectadas: Boolean get() = conectadas
        override suspend fun gastos(): List<GastoRegistrado> { if (falla) error("Room"); return g }
        override suspend fun ventas() = v
    }

    // ── GD: distribución de gastos ──
    @Test fun gd01_hoySoloLosGastosDelDia() {
        val d = distribucionGastos(listOf(gasto(hoy, 50_000, TipoGasto.TRANSPORTE), gasto(Fecha(26, 11, 2026), 90_000, TipoGasto.CARGUE)), PeriodoFinanciero.HOY, hoy)
        assertEquals(50_000L, d.totalPesos)
        assertEquals(listOf(TipoGasto.TRANSPORTE), d.categorias.map { it.tipo })
    }

    @Test fun gd02_semanaSonLosUltimosSieteDias() {
        val g = listOf(gasto(hoy, 10_000, TipoGasto.OTROS), gasto(Fecha(21, 11, 2026), 20_000, TipoGasto.OTROS), gasto(Fecha(20, 11, 2026), 999_000, TipoGasto.OTROS))
        assertEquals(30_000L, distribucionGastos(g, PeriodoFinanciero.SEMANA, hoy).totalPesos)
    }

    @Test fun gd04_sinGastosVaciaSinDividirPorCero() {
        val d = distribucionGastos(emptyList(), PeriodoFinanciero.SEMANA, hoy)
        assertTrue(d.vacia); assertTrue(d.categorias.isEmpty())
    }

    @Test fun gd05_unaCategoriaEsCienPorCiento() =
        assertEquals(100, distribucionGastos(listOf(gasto(hoy, 7, TipoGasto.SECADO)), PeriodoFinanciero.HOY, hoy).categorias.single().porcentaje)

    @Test fun gd06_variasCategoriasProporcionesQueSumanCien() {
        val g = listOf(gasto(hoy, 100_000, TipoGasto.TRANSPORTE), gasto(hoy, 100_000, TipoGasto.CARGUE), gasto(hoy, 100_000, TipoGasto.DESCARGUE), gasto(hoy, 50_000, TipoGasto.TRANSPORTE))
        val d = distribucionGastos(g, PeriodoFinanciero.HOY, hoy)
        assertEquals(listOf(TipoGasto.TRANSPORTE, TipoGasto.CARGUE, TipoGasto.DESCARGUE), d.categorias.map { it.tipo })
        assertEquals(listOf(150_000L, 100_000L, 100_000L), d.categorias.map { it.pesos })
        assertEquals(100, d.categorias.sumOf { it.porcentaje })
        assertEquals(listOf(43, 29, 28), d.categorias.map { it.porcentaje })
    }

    @Test fun gd03_registrarUnGastoActualizaAlRecargar() {
        val origen = Origen(g = listOf(gasto(hoy, 10_000, TipoGasto.OTROS)))
        val vm = GraficasFinancierasViewModel(origen) { hoy }
        assertEquals(10_000L, vm.estado.value.distribucion!!.totalPesos)
        origen.g = origen.g + gasto(hoy, 5_000, TipoGasto.COMIDA)
        vm.cargar()
        assertEquals(15_000L, vm.estado.value.distribucion!!.totalPesos)
        assertEquals(2, vm.estado.value.distribucion!!.categorias.size)
    }

    @Test fun gd07_errorDeConsultaEsDistintoDelVacio() {
        val vm = GraficasFinancierasViewModel(Origen(falla = true)) { hoy }
        assertEquals(ERROR_GRAFICAS, vm.estado.value.error)
        assertNull(vm.estado.value.distribucion)
    }

    // ── GB: balance semanal ──
    @Test fun gb01_gb02_balanceSoloEnEstaSemana() {
        val vm = GraficasFinancierasViewModel(Origen(v = listOf(venta(hoy, 10.0, 15_000.0, 10_000.0)))) { hoy }
        assertFalse(vm.estado.value.muestraBalance); assertNull(vm.estado.value.balance)
        vm.cambiarPeriodo(PeriodoFinanciero.SEMANA)
        assertTrue(vm.estado.value.muestraBalance)
        assertEquals(7, vm.estado.value.balance!!.size)
        assertEquals(150_000L, vm.estado.value.balance!!.last().ingresosPesos)
    }

    @Test fun gb_costosSonCompraDelCafeVendidoMasSusGastos() {
        val dias = balanceSemanal(listOf(venta(hoy, 10.0, 15_000.0, 10_000.0, gastos = 8_000.0), venta(hoy, 5.0, 16_000.0, 9_000.0)), hoy)
        assertEquals(150_000L + 80_000L, dias.last().ingresosPesos)
        assertEquals(100_000L + 8_000L + 45_000L, dias.last().costosPesos)
        // Coincide con el Resumen (misma calcularUtilidad): ingresos − costos = utilidad.
        val resumen = runBlocking { FinanzasLocal(Origen(v = listOf(venta(hoy, 10.0, 15_000.0, 10_000.0, 8_000.0), venta(hoy, 5.0, 16_000.0, 9_000.0)))) { hoy }.resumen(PeriodoFinanciero.HOY) }
        assertEquals(resumen.utilidad, dias.last().ingresosPesos - dias.last().costosPesos)
    }

    @Test fun gb08_diasSinMovimientosEnCeroYSinSaltos() {
        val dias = balanceSemanal(listOf(venta(Fecha(24, 11, 2026), 1.0, 1_000.0, 500.0)), hoy)
        assertEquals((21..27).toList(), dias.map { it.fecha.dia })
        assertEquals(listOf(0L, 0L, 0L, 1_000L, 0L, 0L, 0L), dias.map { it.ingresosPesos })
    }

    @Test fun gb09_semanaEntreDosMeses() =
        assertEquals(listOf("Jue 26", "Vie 27", "Sáb 28", "Dom 29", "Lun 30", "Mar 1", "Mié 2"),
            diasDeLaSemana(Fecha(2, 12, 2026)).map { etiquetaDia(it) })

    @Test fun gb10_semanaEntreDosAnios() {
        val dias = diasDeLaSemana(Fecha(2, 1, 2027))
        assertEquals(Fecha(27, 12, 2026), dias.first())
        assertEquals(listOf("Dom 27", "Lun 28", "Mar 29", "Mié 30", "Jue 31", "Vie 1", "Sáb 2"), dias.map { etiquetaDia(it) })
        assertEquals("viernes 1 de enero", fechaParaLeer(Fecha(1, 1, 2027)))
    }

    @Test fun gb07_escalaConValoresElevados() {
        val e = escalaMonetaria(1_000_000)
        assertEquals(listOf(0L, 250_000L, 500_000L, 750_000L, 1_000_000L), e.marcas)
        assertEquals(listOf("$0", "$250 mil", "$500 mil", "$750 mil", "$1 M"), e.marcas.map { pesosCortos(it) })
        val grande = escalaMonetaria(37_400_000)
        assertTrue(grande.tope >= 37_400_000); assertEquals(grande.paso * 4, grande.tope)
        assertEquals("$1,5 M", pesosCortos(1_500_000))
    }

    @Test fun gb13_todoEnCeroNoDividePorCero() {
        val e = escalaMonetaria(0)
        assertTrue(e.tope > 0)
        assertTrue(balanceSemanal(emptyList(), hoy).all { it.ingresosPesos == 0L && it.costosPesos == 0L })
    }

    @Test fun gb12_rf07_sinVentasConectadasNoSeInventaNada() {
        val origen = Origen(conectadas = false)
        val vm = GraficasFinancierasViewModel(origen) { hoy }
        vm.cambiarPeriodo(PeriodoFinanciero.SEMANA)
        assertFalse(vm.estado.value.ventasConectadas)
        assertNull(vm.estado.value.balance)
        assertEquals(7, vm.estado.value.semana.size)
        val r = runBlocking { FinanzasLocal(origen) { hoy }.resumen(PeriodoFinanciero.SEMANA) }
        assertFalse(r.ventasDisponibles)
    }

    @Test fun rf03_cambiarPeriodoLimpiaLosDatosAnteriores() {
        val vm = GraficasFinancierasViewModel(Origen(g = listOf(gasto(Fecha(25, 11, 2026), 40_000, TipoGasto.OTROS)))) { hoy }
        assertTrue(vm.estado.value.distribucion!!.vacia)       // hoy no hubo gastos
        vm.cambiarPeriodo(PeriodoFinanciero.SEMANA)
        assertEquals(40_000L, vm.estado.value.distribucion!!.totalPesos)
        vm.cambiarPeriodo(PeriodoFinanciero.HOY)
        assertTrue(vm.estado.value.distribucion!!.vacia)
        assertNull(vm.estado.value.balance)
    }

    @Test fun porcentajesEnterosSiempreSumanCien() {
        assertEquals(100, porcentajesEnteros(listOf(1, 1, 1), 3).sum())
        assertEquals(listOf(0, 0), porcentajesEnteros(listOf(0, 0), 0))
    }
}
