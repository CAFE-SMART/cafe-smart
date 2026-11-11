package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.finanzas.FinanzasLocal
import com.cafesmart.app.data.finanzas.OrigenVentas
import com.cafesmart.app.data.finanzas.SinVentasRegistradas
import com.cafesmart.app.data.finanzas.FinanzasRepository
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.finanzas.ResumenFinanciero
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.finanzas.diasEntre
import com.cafesmart.app.data.finanzas.enElPeriodo
import com.cafesmart.app.data.finanzas.restarDias
import com.cafesmart.app.data.finanzas.resumirVentas
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.TipoAviso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class ResumenFinancieroTest {

    private val hoy = Fecha(10, 11, 2026)

    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    // ── Fechas ─────────────────────────────────────────────

    @Test
    fun diasEntreFechasCuentaBienAunqueCambieElMes() {
        assertEquals(0, diasEntre(hoy, hoy))
        assertEquals(3, diasEntre(Fecha(7, 11, 2026), hoy))
        assertEquals(10, diasEntre(Fecha(31, 10, 2026), hoy))
        assertEquals(365, diasEntre(Fecha(10, 11, 2025), hoy))
    }

    @Test
    fun restarDiasCruzaMesesYAnios() {
        assertEquals(Fecha(31, 10, 2026), restarDias(hoy, 10))
        assertEquals(Fecha(31, 12, 2025), restarDias(Fecha(2, 1, 2026), 2))
        assertEquals(Fecha(29, 2, 2028), restarDias(Fecha(1, 3, 2028), 1))
    }

    @Test
    fun elPeriodoHoyEsSoloEseDiaYLaSemanaSonSieteDias() {
        assertTrue(enElPeriodo(hoy, PeriodoFinanciero.HOY, hoy))
        assertFalse(enElPeriodo(restarDias(hoy, 1), PeriodoFinanciero.HOY, hoy))
        assertTrue(enElPeriodo(restarDias(hoy, 6), PeriodoFinanciero.SEMANA, hoy))
        assertFalse(enElPeriodo(restarDias(hoy, 7), PeriodoFinanciero.SEMANA, hoy))
        // Una fecha futura no cuenta.
        assertFalse(enElPeriodo(Fecha(11, 11, 2026), PeriodoFinanciero.SEMANA, hoy))
    }

    // ── Cálculo ────────────────────────────────────────────

    @Test
    fun laUtilidadEsIngresosMenosCostoMenosGastos() {
        val ventas = listOf(VentaRegistrada(hoy, 200.0, 12_000.0, 9_000.0, 45_000.0, mermaKg = 12.0))
        val r = resumirVentas(ventas, PeriodoFinanciero.HOY, hoy)
        assertEquals(2_400_000, r.ingresos)
        assertEquals(1_800_000, r.costoCafe)
        assertEquals(45_000, r.gastos)
        assertEquals(555_000, r.utilidad)
        assertEquals(1_845_000, r.salio)
        assertEquals(12.0, r.mermaKg, 0.0)
    }

    @Test
    fun unaVentaConPerdidaDaUtilidadNegativa() {
        val ventas = listOf(VentaRegistrada(hoy, 100.0, 9_000.0, 10_000.0, 10_000.0))
        assertEquals(-110_000, resumirVentas(ventas, PeriodoFinanciero.HOY, hoy).utilidad)
    }

    @Test
    fun sumaLasVentasDelPeriodoYDejaFueraLasDeOtrosDias() {
        val ventas = listOf(
            VentaRegistrada(hoy, 10.0, 10_000.0, 8_000.0, 0.0),
            VentaRegistrada(restarDias(hoy, 2), 10.0, 10_000.0, 8_000.0, 0.0),
            VentaRegistrada(restarDias(hoy, 9), 1000.0, 10_000.0, 8_000.0, 0.0),
        )
        assertEquals(20_000, resumirVentas(ventas, PeriodoFinanciero.HOY, hoy).utilidad)
        assertEquals(40_000, resumirVentas(ventas, PeriodoFinanciero.SEMANA, hoy).utilidad)
    }

    @Test
    fun sinVentasTodoEsCero() {
        val r = resumirVentas(emptyList(), PeriodoFinanciero.HOY, hoy)
        assertEquals(ResumenFinanciero(0, 0, 0, 0, 0.0, ventas = 0), r)
    }

    @Test
    fun sinVentasRegistradasLasMetricasSonCeroNoInventadas() = kotlinx.coroutines.test.runTest {
        val repo = FinanzasLocal(SinVentasRegistradas) { hoy }
        PeriodoFinanciero.entries.forEach {
            assertEquals(ResumenFinanciero(0, 0, 0, 0, 0.0, ventas = 0), repo.resumen(it))
        }
    }

    @Test
    fun elRepositorioCalculaSobreLasVentasQueEntregaElOrigen() = kotlinx.coroutines.test.runTest {
        val origen = OrigenVentas { listOf(VentaRegistrada(hoy, 10.0, 12_000.0, 9_000.0, 5_000.0, mermaKg = 1.0)) }
        val r = FinanzasLocal(origen) { hoy }.resumen(PeriodoFinanciero.HOY)
        assertEquals(1, r.ventas)
        assertEquals(120_000, r.ingresos)
        assertEquals(25_000, r.utilidad)
    }

    @Test
    fun elResumenCuentaCuantasVentasHay() {
        val ventas = listOf(
            VentaRegistrada(hoy, 1.0, 10.0, 5.0, 0.0),
            VentaRegistrada(hoy, 1.0, 10.0, 5.0, 0.0),
            VentaRegistrada(restarDias(hoy, 2), 1.0, 10.0, 5.0, 0.0),
        )
        assertEquals(2, resumirVentas(ventas, PeriodoFinanciero.HOY, hoy).ventas)
        assertEquals(3, resumirVentas(ventas, PeriodoFinanciero.SEMANA, hoy).ventas)
    }

    // ── Mensajes ───────────────────────────────────────────

    @Test
    fun elMensajeDiceSiGanaPierdeQuedaEnCeroOTodaviaNoHayVentas() {
        assertEquals("Hoy ganas más de lo que gastas.", mensajeDeUtilidad(1, PeriodoFinanciero.HOY, ventas = 1))
        assertEquals("Esta semana gastas más de lo que ganas.", mensajeDeUtilidad(-1, PeriodoFinanciero.SEMANA, ventas = 2))
        assertEquals("Hoy no ganas ni pierdes.", mensajeDeUtilidad(0, PeriodoFinanciero.HOY, ventas = 1))
        assertEquals("Hoy todavía no registras ventas.", mensajeDeUtilidad(0, PeriodoFinanciero.HOY, ventas = 0))
    }

    // ── ViewModel ──────────────────────────────────────────

    private class RepoFalso(var resumen: ResumenFinanciero = ResumenFinanciero(100, 40, 10, 50, 1.0), var falla: Boolean = false) : FinanzasRepository {
        val periodos = mutableListOf<PeriodoFinanciero>()
        override suspend fun resumen(periodo: PeriodoFinanciero): ResumenFinanciero {
            periodos += periodo
            if (falla) throw IllegalStateException("sin datos")
            return resumen
        }
    }

    @Test
    fun alAbrirCargaElResumenDeHoy() {
        val repo = RepoFalso()
        val vm = ResumenFinancieroViewModel(repo, pausaMinimaMs = 0)
        assertEquals(PeriodoFinanciero.HOY, vm.estado.value.periodo)
        assertNotNull(vm.estado.value.resumen)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun cambiarElPeriodoVuelveACargar() {
        val repo = RepoFalso()
        val vm = ResumenFinancieroViewModel(repo, pausaMinimaMs = 0)
        vm.cambiarPeriodo(PeriodoFinanciero.SEMANA)
        assertEquals(listOf(PeriodoFinanciero.HOY, PeriodoFinanciero.SEMANA), repo.periodos)
        assertEquals(PeriodoFinanciero.SEMANA, vm.estado.value.periodo)
        // Elegir el mismo periodo no vuelve a cargar.
        vm.cambiarPeriodo(PeriodoFinanciero.SEMANA)
        assertEquals(2, repo.periodos.size)
    }

    @Test
    fun siFallaMuestraElErrorYReintentarLoResuelve() {
        val repo = RepoFalso(falla = true)
        val vm = ResumenFinancieroViewModel(repo, pausaMinimaMs = 0)
        assertEquals(ERROR_RESUMEN, vm.estado.value.error)
        assertNull(vm.estado.value.resumen)
        repo.falla = false
        vm.recargar()
        assertNull(vm.estado.value.error)
        assertNotNull(vm.estado.value.resumen)
    }

    @Test
    fun alRecargarSeActualizaYAparecenElAvisoVerde() {
        val vm = ResumenFinancieroViewModel(RepoFalso(), pausaMinimaMs = 0)
        assertNull(vm.estado.value.aviso)
        vm.recargar()
        assertFalse(vm.estado.value.actualizando)
        assertEquals(AvisoUi(AVISO_RESUMEN_ACTUALIZADO, TipoAviso.EXITO), vm.estado.value.aviso)
        vm.ocultarAviso()
        assertNull(vm.estado.value.aviso)
    }

    @Test
    fun siRecargarFallaApareceElAvisoRojoYSeConservaLoAnterior() {
        val repo = RepoFalso()
        val vm = ResumenFinancieroViewModel(repo, pausaMinimaMs = 0)
        val antes = vm.estado.value.resumen
        repo.falla = true
        vm.recargar()
        assertEquals(AvisoUi(ERROR_RESUMEN, TipoAviso.ERROR), vm.estado.value.aviso)
        assertEquals(antes, vm.estado.value.resumen)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun laCargaInicialNoMuestraAvisoDeActualizado() {
        val vm = ResumenFinancieroViewModel(RepoFalso(), pausaMinimaMs = 0)
        assertNull(vm.estado.value.aviso)
    }
}
