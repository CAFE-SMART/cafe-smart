package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.inventario.InventarioFalso
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sample.lotesEjemplo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// El estado de Inventario con un repositorio falso (sin Room): el resumen, el estado vacío, el estado "cargando"
// y que una compra nueva aparezca sola. UnconfinedTestDispatcher hace que lo que emite el repositorio llegue al
// estado al instante, igual que en las demás pruebas de ViewModel.
class InventoryViewModelTest {

    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    // Una compra nueva: un lote con un sublote de 50 kg que entró el 15 de noviembre (por defecto Verde · Buena).
    private fun loteNuevo(id: Long = 50, tipo: TipoCafe = TipoCafe.VERDE, calidad: Calidad = Calidad.BUENA) =
        LoteConSublotes(
            Lote(id = id, tipoCafe = tipo, calidad = calidad),
            listOf(
                Sublote(
                    id = id, loteId = id, compraId = id, pesoInicialKg = 50.0, pesoDisponibleKg = 50.0,
                    costoKg = 15_000, fechaIngreso = Fecha(15, 11, 2026),
                )
            ),
        )

    // ── Resumen ────────────────────────────────────────────

    @Test
    fun alEmpezarMuestraElResumenDelInventarioMasReciente() {
        val vm = InventoryViewModel(InventarioFalso(lotesEjemplo()))
        assertEquals(OrdenInventario.MAS_RECIENTE, vm.uiState.value.orden)
        assertEquals(834.0, vm.uiState.value.resumen.totalKg, 1e-9)
        assertEquals(TipoCafe.VERDE, vm.uiState.value.resumen.tipos.first().tipo)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun cambiarElOrdenReordenaLaLista() {
        val vm = InventoryViewModel(InventarioFalso(lotesEjemplo()))
        vm.cambiarOrden(OrdenInventario.MAS_ANTIGUO)
        assertEquals(OrdenInventario.MAS_ANTIGUO, vm.uiState.value.orden)
        assertEquals(TipoCafe.TRILLADO, vm.uiState.value.resumen.tipos.first().tipo)
        // El total no cambia al reordenar.
        assertEquals(834.0, vm.uiState.value.resumen.totalKg, 1e-9)
    }

    @Test
    fun elEstadoGuardaLosLotesParaLasOtrasDosVistas() {
        val vm = InventoryViewModel(InventarioFalso(lotesEjemplo()))
        assertEquals(lotesEjemplo(), vm.uiState.value.lotes)
        // Lista por tipo: las tres calidades de Verde, la más reciente primero.
        assertEquals(
            listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA),
            vm.uiState.value.calidadesDe(TipoCafe.VERDE).map { it.calidad },
        )
        // Sublotes del grupo: los dos de Verde · Regular (lote 2), el más reciente primero.
        assertEquals(listOf(2L, 3L), vm.uiState.value.sublotesDe(loteId = 2).map { it.sublote.id })
    }

    // ── Estado vacío y estado cargando ─────────────────────

    @Test
    fun sinSublotesEnRoomMuestraElEstadoVacio() {
        val vm = InventoryViewModel(InventarioFalso(emptyList()))
        assertTrue(vm.uiState.value.resumen.estaVacio)
        assertEquals(0.0, vm.uiState.value.resumen.totalKg, 0.0)
        // Ya se leyó Room y no hay café: aquí sí se puede mostrar "vacío".
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun unLoteSinSublotesConExistenciaTambienEsVacio() {
        // Un lote que ya no tiene café (todo vendido) no cuenta: el inventario se ve vacío.
        val agotado = LoteConSublotes(
            Lote(1, TipoCafe.SECO, Calidad.BUENA),
            listOf(
                Sublote(
                    id = 1, loteId = 1, compraId = 1, pesoInicialKg = 40.0, pesoDisponibleKg = 0.0,
                    costoKg = 20_000, fechaIngreso = Fecha(1, 11, 2026),
                )
            ),
        )
        val vm = InventoryViewModel(InventarioFalso(listOf(agotado)))
        assertTrue(vm.uiState.value.resumen.estaVacio)
    }

    @Test
    fun hastaLaPrimeraLecturaDeRoomEstaCargandoYNoMuestraNingunaCifra() {
        val flujo = MutableSharedFlow<List<LoteConSublotes>>(extraBufferCapacity = 1)
        val vm = InventoryViewModel(object : InventoryRepository {
            override fun observarLotes(): Flow<List<LoteConSublotes>> = flujo
        })
        assertTrue(vm.uiState.value.cargando)

        flujo.tryEmit(lotesEjemplo())
        assertFalse(vm.uiState.value.cargando)
        assertEquals(834.0, vm.uiState.value.resumen.totalKg, 1e-9)
    }

    // ── Cambios que llegan con la pantalla abierta ─────────

    @Test
    fun unaCompraNuevaApareceSinReiniciar() {
        val repositorio = InventarioFalso(emptyList())
        val vm = InventoryViewModel(repositorio)
        assertTrue(vm.uiState.value.resumen.estaVacio)

        // Compras guarda un lote con un sublote en Room → Room vuelve a emitir.
        repositorio.lotes.value = listOf(loteNuevo())

        assertFalse(vm.uiState.value.resumen.estaVacio)
        assertEquals(50.0, vm.uiState.value.resumen.totalKg, 1e-9)
        assertEquals(1, vm.uiState.value.resumen.tipos.first { it.tipo == TipoCafe.VERDE }.cantidadSublotes)
    }

    @Test
    fun alCambiarElInventarioSeConservaElOrdenElegido() {
        val repositorio = InventarioFalso(lotesEjemplo())
        val vm = InventoryViewModel(repositorio)
        vm.cambiarOrden(OrdenInventario.MAS_ANTIGUO)

        repositorio.lotes.value = lotesEjemplo() + loteNuevo(id = 51, tipo = TipoCafe.PASILLA, calidad = Calidad.MALA)

        assertEquals(OrdenInventario.MAS_ANTIGUO, vm.uiState.value.orden)
        assertEquals(TipoCafe.TRILLADO, vm.uiState.value.resumen.tipos.first().tipo)
    }

    @Test
    fun siSeVendeTodoElCafeVuelveElEstadoVacio() {
        val repositorio = InventarioFalso(listOf(loteNuevo()))
        val vm = InventoryViewModel(repositorio)
        assertFalse(vm.uiState.value.resumen.estaVacio)

        repositorio.lotes.value = emptyList()

        assertTrue(vm.uiState.value.resumen.estaVacio)
    }
}
