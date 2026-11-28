package com.cafesmart.app.viewmodel.home

import com.cafesmart.app.data.inventario.RoomInventoryRepository
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.purchase.BaseFalsa
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.viewmodel.inventory.EstadoBodega
import com.cafesmart.app.viewmodel.inventory.InventoryViewModel
import com.cafesmart.app.viewmodel.inventory.formatoBultos
import com.cafesmart.app.viewmodel.inventory.formatoPorcentaje
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Inicio con datos de la cuenta (repositorios reales sobre DAOs falsos) y su consistencia con Inventario.
class HomeViewModelTest {

    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private val uid = "cuenta-a"
    private val hoy = Fecha(23, 11, 2026)
    private val base = BaseFalsa()
    private val compras = base.repositorio()
    private val inventario = RoomInventoryRepository(base.lotes, base.sublotes)

    private class NegocioDeLaCuenta(inicial: NegocioEntity?) : NegocioRepository {
        val actual = MutableStateFlow(inicial)
        override suspend fun guardar(negocio: NegocioEntity) { actual.value = negocio }
        override fun observar(uid: String): Flow<NegocioEntity?> = actual
    }

    private fun negocio(capacidad: Double, bulto: Double = 40.0) =
        NegocioEntity(uid, "Café A", "COMPRAVENTA", "", capacidadBodegaKg = capacidad, pesoBultoKg = bulto, creadoEn = 0)

    private fun cafe(tipo: String, calidad: Calidad, kg: Double) = CafeValidado(tipo, calidad, kg, 14_000.0)
    private fun comprar(vararg cafes: CafeValidado, fecha: Fecha = hoy) =
        runBlocking { compras.registrarCompra(null, fecha, cafes.toList()) }

    private fun inicio(n: NegocioRepository?) =
        HomeViewModel(inventario, base.compras.observarTodas(), n, uid, hoy = { hoy })
    private fun inventarioVm(n: NegocioRepository?) = InventoryViewModel(inventario, negocios = n, uid = uid)

    @Test
    fun cuentaNuevaSinDatosMuestraTodoVacioYBodegaSinConfigurar() {
        val e = inicio(NegocioDeLaCuenta(null)).uiState.value
        assertEquals(false, e.cargando)
        assertNull(e.error)
        assertEquals(0.0, e.inventario.totalKg, 0.0)
        assertEquals(EstadoBodega.SIN_CAPACIDAD, e.inventario.ocupacion.estado)
        assertEquals(ComprasDelDia(0, 0.0, 0), e.delDia)
        assertTrue(e.sublotesAntiguos.isEmpty())
        assertEquals(0, e.cantidadSublotes)
    }

    @Test
    fun inicioEInventarioDanElMismoPorcentajeTotalYBultos() {
        // 834 kg en 3.000 kg: Inventario decía 28 % e Inicio 19 % (559 kg fijos). Ahora ambos 28 %.
        comprar(cafe("Verde", Calidad.BUENA, 290.0), cafe("Seco", Calidad.BUENA, 521.0), cafe("Trillado", Calidad.BUENA, 23.0))
        val n = NegocioDeLaCuenta(negocio(3_000.0))
        val h = inicio(n).uiState
        val i = inventarioVm(n).uiState

        assertEquals(834.0, h.value.inventario.totalKg, 0.0)
        assertEquals(i.value.resumen.ocupacion, h.value.inventario.ocupacion)
        assertEquals("28%", formatoPorcentaje(h.value.inventario.ocupacion.porcentaje))
        assertEquals(formatoBultos(i.value.resumen.totalKg, i.value.resumen.pesoBultoKg),
            formatoBultos(h.value.inventario.totalKg, h.value.inventario.pesoBultoKg))

        // Una compra nueva y un cambio de capacidad llegan a las dos pantallas.
        comprar(cafe("Verde", Calidad.REGULAR, 1_866.0))
        n.actual.value = negocio(2_700.0)
        assertEquals(i.value.resumen.ocupacion, h.value.inventario.ocupacion)
        assertEquals("100%", formatoPorcentaje(h.value.inventario.ocupacion.porcentaje))
        assertEquals(EstadoBodega.CRITICA, h.value.inventario.ocupacion.estado)
        assertEquals(4, h.value.cantidadSublotes)
    }

    @Test
    fun sinNegocioNoSeInventaCapacidad() {
        comprar(cafe("Verde", Calidad.BUENA, 100.0))
        val e = inicio(NegocioDeLaCuenta(null)).uiState.value
        assertEquals(100.0, e.inventario.totalKg, 0.0)
        assertEquals(0.0, e.inventario.capacidadKg, 0.0)
        assertEquals(EstadoBodega.SIN_CAPACIDAD, e.inventario.ocupacion.estado)
    }

    @Test
    fun resumenDelDiaCuentaSoloLasComprasDeHoy() {
        comprar(cafe("Verde", Calidad.BUENA, 40.0), fecha = Fecha(20, 11, 2026))
        comprar(cafe("Verde", Calidad.BUENA, 100.0), cafe("Seco", Calidad.MALA, 25.5))
        comprar(cafe("Seco", Calidad.BUENA, 10.0))
        val e = inicio(NegocioDeLaCuenta(negocio(1_000.0))).uiState.value
        assertEquals(2, e.delDia.compras)
        assertEquals(135.5, e.delDia.kgComprados, 0.0)
        assertEquals(0, e.delDia.productores) // las dos sin productor registrado
    }

    @Test
    fun productoresDelDiaSonLosDistintosConCompraHoy() {
        val m = fechaAMilis(hoy)
        val lista = listOf(
            CompraEntity(1, 7, m, 0), CompraEntity(2, 7, m, 0), CompraEntity(3, 9, m, 0),
            CompraEntity(4, null, m, 0), CompraEntity(5, 11, fechaAMilis(Fecha(1, 11, 2026)), 0),
        )
        assertEquals(ComprasDelDia(4, 0.0, 2), resumirDia(emptyList(), lista, hoy))
    }

    @Test
    fun sublotesMasAntiguosSonLosTresConCafeQueMasLlevanEnBodega() {
        val lote = Lote(1, TipoCafe.SECO, Calidad.REGULAR)
        fun s(id: Long, fecha: Fecha, kg: Double) = Sublote(id, 1, 1, null, 50.0, kg, 1_000, fecha)
        val lotes = listOf(LoteConSublotes(lote, listOf(
            s(1, Fecha(1, 10, 2026), 0.0), // agotado: no cuenta
            s(2, Fecha(5, 11, 2026), 10.0),
            s(3, Fecha(2, 10, 2026), 20.0),
            s(4, Fecha(5, 11, 2026), 30.0),
            s(5, hoy, 40.0),
        )))
        val r = sublotesMasAntiguos(lotes, hoy)
        assertEquals(listOf(3L, 2L, 4L), r.map { it.id })
        assertEquals(52, r.first().diasEnBodega)
        assertEquals(TipoCafe.SECO, r.first().tipo)
    }

    @Test
    fun unaFallaDeRoomMuestraErrorYReintentarVuelveALeer() {
        var falla = true
        val repo = object : com.cafesmart.app.data.inventario.InventoryRepository {
            override fun observarLotes() = flow<List<LoteConSublotes>> {
                if (falla) throw IllegalStateException("Room") else emit(emptyList())
            }
        }
        val vm = HomeViewModel(repo, base.compras.observarTodas(), null, null, hoy = { hoy })
        assertEquals(ERROR_INICIO, vm.uiState.value.error)
        falla = false
        vm.reintentar()
        assertNull(vm.uiState.value.error)
        assertEquals(false, vm.uiState.value.cargando)
    }
}
