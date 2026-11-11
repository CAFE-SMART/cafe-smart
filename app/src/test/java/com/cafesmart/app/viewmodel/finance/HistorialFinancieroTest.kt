package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.finanzas.CompraRegistrada
import com.cafesmart.app.data.finanzas.FiltroMovimiento
import com.cafesmart.app.data.finanzas.FinanzasLocal
import com.cafesmart.app.data.finanzas.FinanzasRepository
import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.finanzas.MovimientoFinanciero
import com.cafesmart.app.data.finanzas.OrigenFinanciero
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.finanzas.ResumenFinanciero
import com.cafesmart.app.data.finanzas.SinDatosFinancieros
import com.cafesmart.app.data.finanzas.TipoMovimiento
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.finanzas.agruparPorDia
import com.cafesmart.app.data.finanzas.armarMovimientos
import com.cafesmart.app.data.finanzas.etiquetaDeDia
import com.cafesmart.app.data.finanzas.filtrarMovimientos
import com.cafesmart.app.data.finanzas.mensajeHistorialVacio
import com.cafesmart.app.data.finanzas.restarDias
import com.cafesmart.app.data.model.Fecha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialFinancieroTest {

    private val hoy = Fecha(10, 11, 2026)
    private val ayer = restarDias(hoy, 1)

    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private val venta = VentaRegistrada(hoy, 200.0, 12_000.0, 9_000.0, 0.0, cliente = "Exportadora", detalle = "Café seco · 200 kg")
    private val compra = CompraRegistrada(hoy, 350_000, productor = "Carlos Mejía", detalle = "Café verde · 25 kg")
    private val gasto = GastoRegistrado(hoy, 45_000, concepto = "Transporte", detalle = "Gasto general")

    // ── Armado y orden ─────────────────────────────────────

    @Test
    fun laVentaSumaYLaCompraYElGastoRestan() {
        val m = armarMovimientos(listOf(venta), listOf(compra), listOf(gasto))
        assertEquals(listOf(2_400_000L, -350_000L, -45_000L), m.map { it.montoPesos })
        assertEquals(listOf("Venta · Exportadora", "Compra · Carlos Mejía", "Gasto · Transporte"), m.map { it.titulo })
        assertEquals("Café seco · 200 kg", m[0].detalle)
    }

    @Test
    fun ordenaDeLoMasRecienteALoMasAntiguoYConLaMismaFechaVentaCompraGasto() {
        val viejo = CompraRegistrada(restarDias(hoy, 40), 1_000, productor = "A")
        val deAyer = GastoRegistrado(ayer, 2_000)
        val m = armarMovimientos(listOf(venta), listOf(viejo, compra), listOf(deAyer, gasto))
        assertEquals(
            listOf(TipoMovimiento.VENTA, TipoMovimiento.COMPRA, TipoMovimiento.GASTO, TipoMovimiento.GASTO, TipoMovimiento.COMPRA),
            m.map { it.tipo }
        )
        assertEquals(listOf(hoy, hoy, hoy, ayer, restarDias(hoy, 40)), m.map { it.fecha })
    }

    @Test
    fun ordenaBienEntreMesesYAnios() {
        val m = armarMovimientos(
            emptyList(),
            listOf(CompraRegistrada(Fecha(31, 12, 2025), 1), CompraRegistrada(Fecha(1, 1, 2026), 1), CompraRegistrada(Fecha(15, 11, 2025), 1)),
            emptyList()
        )
        assertEquals(listOf(Fecha(1, 1, 2026), Fecha(31, 12, 2025), Fecha(15, 11, 2025)), m.map { it.fecha })
    }

    // ── Filtro ─────────────────────────────────────────────

    @Test
    fun elFiltroDejaSoloElTipoElegidoYTodosLoDejaTodo() {
        val m = armarMovimientos(listOf(venta), listOf(compra, compra), listOf(gasto))
        assertEquals(4, filtrarMovimientos(m, FiltroMovimiento.TODOS).size)
        assertEquals(2, filtrarMovimientos(m, FiltroMovimiento.COMPRAS).size)
        assertEquals(1, filtrarMovimientos(m, FiltroMovimiento.VENTAS).size)
        assertEquals(1, filtrarMovimientos(m, FiltroMovimiento.GASTOS).size)
        assertTrue(filtrarMovimientos(m, FiltroMovimiento.VENTAS).all { it.tipo == TipoMovimiento.VENTA })
    }

    // ── Días ───────────────────────────────────────────────

    @Test
    fun losDiasSeTitulanConHoyAyerOLaFecha() {
        assertEquals("Hoy · 10 de noviembre de 2026", etiquetaDeDia(hoy, hoy))
        assertEquals("Ayer · 9 de noviembre de 2026", etiquetaDeDia(ayer, hoy))
        assertEquals("1 de octubre de 2026", etiquetaDeDia(Fecha(1, 10, 2026), hoy))
    }

    @Test
    fun agrupaPorDiaSinCambiarElOrden() {
        val m = armarMovimientos(listOf(venta), listOf(compra, CompraRegistrada(ayer, 5)), listOf(gasto))
        val grupos = agruparPorDia(m, hoy)
        assertEquals(listOf("Hoy · 10 de noviembre de 2026", "Ayer · 9 de noviembre de 2026"), grupos.map { it.etiqueta })
        assertEquals(listOf(3, 1), grupos.map { it.movimientos.size })
    }

    @Test
    fun cadaFiltroSinResultadosTieneSuPropioMensaje() {
        assertEquals(FiltroMovimiento.entries.size, FiltroMovimiento.entries.map { mensajeHistorialVacio(it) }.toSet().size)
    }

    // ── Repositorio ────────────────────────────────────────

    @Test
    fun sinDatosElHistorialEstaVacio() = runTest {
        assertTrue(FinanzasLocal(SinDatosFinancieros) { hoy }.movimientos().isEmpty())
    }

    @Test
    fun elRepositorioMezclaLoQueEntregaElOrigen() = runTest {
        val origen = object : OrigenFinanciero {
            override suspend fun ventas() = listOf(venta)
            override suspend fun compras() = listOf(compra)
        }
        val m = FinanzasLocal(origen) { hoy }.movimientos()
        assertEquals(listOf(TipoMovimiento.VENTA, TipoMovimiento.COMPRA), m.map { it.tipo })
    }

    // ── ViewModel ──────────────────────────────────────────

    private class RepoFalso(
        var movimientos: List<MovimientoFinanciero> = emptyList(),
        var falla: Boolean = false,
    ) : FinanzasRepository {
        override suspend fun resumen(periodo: PeriodoFinanciero) = ResumenFinanciero(0, 0, 0, 0, 0.0)
        override suspend fun movimientos(): List<MovimientoFinanciero> {
            if (falla) throw IllegalStateException("sin datos")
            return movimientos
        }
    }

    private fun muchos(n: Int) = List(n) { MovimientoFinanciero(TipoMovimiento.COMPRA, hoy, "Compra · $it", "", -1) }

    @Test
    fun alAbrirCargaTodosLosMovimientos() {
        val vm = HistorialFinancieroViewModel(RepoFalso(armarMovimientos(listOf(venta), listOf(compra), emptyList()))) { hoy }
        val e = vm.estado.value
        assertFalse(e.cargando)
        assertEquals(FiltroMovimiento.TODOS, e.filtro)
        assertEquals(2, e.filtrados.size)
        assertEquals(1, e.grupos.size)
    }

    @Test
    fun cambiarElFiltroMuestraSoloEseTipo() {
        val vm = HistorialFinancieroViewModel(RepoFalso(armarMovimientos(listOf(venta), listOf(compra), listOf(gasto)))) { hoy }
        vm.cambiarFiltro(FiltroMovimiento.GASTOS)
        assertEquals(listOf(TipoMovimiento.GASTO), vm.estado.value.filtrados.map { it.tipo })
    }

    @Test
    fun muestraDeAVeinteYVerMasAgregaOtrosVeinte() {
        val vm = HistorialFinancieroViewModel(RepoFalso(muchos(45))) { hoy }
        assertEquals(20, vm.estado.value.grupos.sumOf { it.movimientos.size })
        assertTrue(vm.estado.value.hayMas)
        vm.verMas()
        assertEquals(40, vm.estado.value.grupos.sumOf { it.movimientos.size })
        vm.verMas()
        assertEquals(45, vm.estado.value.grupos.sumOf { it.movimientos.size })
        assertFalse(vm.estado.value.hayMas)
    }

    @Test
    fun cambiarElFiltroVuelveALaPrimeraPagina() {
        val vm = HistorialFinancieroViewModel(RepoFalso(muchos(45))) { hoy }
        vm.verMas()
        vm.cambiarFiltro(FiltroMovimiento.COMPRAS)
        assertEquals(MOVIMIENTOS_POR_PAGINA, vm.estado.value.limite)
    }

    @Test
    fun siFallaMuestraElErrorYReintentarLoResuelve() {
        val repo = RepoFalso(falla = true)
        val vm = HistorialFinancieroViewModel(repo) { hoy }
        assertEquals(ERROR_HISTORIAL, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
        repo.falla = false
        repo.movimientos = muchos(1)
        vm.reintentar()
        assertEquals(null, vm.estado.value.error)
        assertEquals(1, vm.estado.value.filtrados.size)
    }

    @Test
    fun sinMovimientosNoHayNadaQueMostrar() {
        val vm = HistorialFinancieroViewModel(RepoFalso()) { hoy }
        assertTrue(vm.estado.value.filtrados.isEmpty())
        assertTrue(vm.estado.value.grupos.isEmpty())
        assertFalse(vm.estado.value.hayMas)
    }
}
