package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.GastoDelSublote
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoGasto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

// Tarea 16 según el Figma: chips Pagado / Pendiente, pestañas y "Registrar gasto" asociado al sublote.
class GastosSubloteFigmaTest {
    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private fun fila(id: Long, tipo: TipoGasto, estado: EstadoPago, global: Long, asignado: Long) = GastoDelSublote(
        Gasto(id, tipo.etiqueta, "", tipo, global, Fecha(3, 6, 2026), estado, AplicaGasto.SUBLOTES), 1, asignado)

    // El ejemplo del Figma: Descargue $ 15.000 pendiente, Cargue $ 20.000 y Transporte $ 45.000 pagados = $ 80.000.
    private val figma = GastosSubloteUiState(gastos = listOf(
        fila(1, TipoGasto.DESCARGUE, EstadoPago.PENDIENTE, 15_000, 15_000),
        fila(2, TipoGasto.CARGUE, EstadoPago.PAGADO, 20_000, 20_000),
        fila(3, TipoGasto.TRANSPORTE, EstadoPago.PAGADO, 90_000, 45_000), // gasto repartido: cuenta solo lo asignado
    ))

    @Test fun totalesPorEstadoComoElFigma() {
        assertEquals(BigInteger.valueOf(80_000), figma.total)
        assertEquals(BigInteger.valueOf(65_000), figma.totalPagado)
        assertEquals(BigInteger.valueOf(15_000), figma.totalPendiente)
        assertEquals(figma.total, figma.totalPagado + figma.totalPendiente)
    }

    @Test fun pestanasSoloFiltranLaLista() {
        assertEquals(listOf(1L), figma.visibles(FiltroGasto.PENDIENTES).map { it.gasto.id })
        assertEquals(listOf(2L, 3L), figma.visibles(FiltroGasto.PAGADOS).map { it.gasto.id })
        assertEquals(listOf(1L, 2L, 3L), figma.visibles(FiltroGasto.TODOS).map { it.gasto.id })
    }

    @Test fun estadosVaciosPorPestana() {
        val vacio = GastosSubloteUiState()
        assertTrue(vacio.visibles(FiltroGasto.TODOS).isEmpty())
        assertEquals(BigInteger.ZERO, vacio.totalPagado)
        assertEquals("Este sublote todavía no tiene gastos registrados.", mensajeSinGastos(FiltroGasto.TODOS))
        assertEquals("Este sublote no tiene gastos pendientes.", mensajeSinGastos(FiltroGasto.PENDIENTES))
        assertEquals("Este sublote no tiene gastos pagados.", mensajeSinGastos(FiltroGasto.PAGADOS))
        val soloPagados = GastosSubloteUiState(gastos = listOf(fila(2, TipoGasto.CARGUE, EstadoPago.PAGADO, 1_000, 1_000)))
        assertTrue(soloPagados.visibles(FiltroGasto.PENDIENTES).isEmpty())
    }

    @Test fun registrarDesdeElSubloteDejaElBorradorAsociadoASoloEseSublote() {
        val disponibles = listOf(Sublote(7, 1, 1, null, 50.0, 50.0, 14_000, Fecha(1, 6, 2026)),
            Sublote(8, 1, 1, null, 30.0, 30.0, 14_000, Fecha(1, 6, 2026)))
        val vm = GastosViewModel(sublotes = disponibles)
        vm.alternarSublote(8) // un borrador anterior con otro sublote no se arrastra
        assertTrue(vm.nuevoRegistroParaSublote(disponibles[0]))
        val form = vm.uiState.value.formulario
        assertEquals(AplicaGasto.SUBLOTES, form.aplicaA)
        assertEquals(setOf(7L), form.asignaciones.keys)
        assertEquals(ResultadoGasto.EDICION, vm.uiState.value.resultado)
    }

    // Si la lista de disponibles aún no llegó de Room, la asociación NO se pierde en silencio.
    @Test fun seAsociaAunqueLaListaDeDisponiblesNoHayaLlegado() {
        val vm = GastosViewModel(sublotes = emptyList())
        assertTrue(vm.nuevoRegistroParaSublote(Sublote(99, 1, 1, null, 10.0, 10.0, 14_000, Fecha(1, 6, 2026))))
        assertEquals(AplicaGasto.SUBLOTES, vm.uiState.value.formulario.aplicaA)
        assertEquals(setOf(99L), vm.uiState.value.formulario.asignaciones.keys)
    }

    // Un sublote agotado no admite gastos nuevos: no se cambia el borrador que hubiera.
    @Test fun subloteAgotadoNoCambiaElBorrador() {
        val disponibles = listOf(Sublote(8, 1, 1, null, 30.0, 30.0, 14_000, Fecha(1, 6, 2026)))
        val vm = GastosViewModel(sublotes = disponibles)
        vm.alternarSublote(8)
        val antes = vm.uiState.value.formulario
        assertEquals(false, vm.nuevoRegistroParaSublote(Sublote(9, 1, 1, null, 30.0, 0.0, 14_000, Fecha(1, 6, 2026))))
        assertEquals(antes, vm.uiState.value.formulario)
    }
}
