package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.*
import com.cafesmart.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class GastosSubloteTest {
    @Before fun preparar() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun limpiar() { Dispatchers.resetMain() }
    private fun gasto(id: Long = 1, monto: Long = 50000) = Gasto(id, "Transporte $id", "Entrega",
        TipoGasto.TRANSPORTE, monto, Fecha(9, 11, 2026), EstadoPago.PAGADO, AplicaGasto.SUBLOTES)

    @Test fun vacioNoInventaRegistros() = runTest {
        val estado = GastosViewModel().observarGastosSublote("1").first { !it.cargando }
        assertNull(estado.error)
        assertEquals(1L, estado.sublote?.id)
        assertTrue(estado.gastos.isEmpty())
        assertEquals(BigInteger.ZERO, estado.total)
    }

    @Test fun repartoUsaSoloElImporteAsignadoYSumaVariosGastos() = runTest {
        val repo = DemoGastosRepository()
        repo.guardar(gasto(), listOf(GastoSublote(1, 1, 30000), GastoSublote(1, 2, 20000)))
        repo.guardar(gasto(2, 7000), listOf(GastoSublote(2, 1, 7000)))
        repo.guardar(gasto(3, 9000), listOf(GastoSublote(3, 2, 9000)))
        val vm = GastosViewModel(repo)
        val a = vm.observarGastosSublote("1").first { !it.cargando }
        val b = vm.observarGastosSublote("2").first { !it.cargando }
        assertEquals(setOf(1L, 2L), a.gastos.map { it.gasto.id }.toSet())
        assertEquals(BigInteger.valueOf(37000), a.total)
        assertEquals(BigInteger.valueOf(29000), b.total)
        assertEquals(30000L, a.gastos.single { it.gasto.id == 1L }.montoAsignado)
        assertEquals(20000L, b.gastos.single { it.gasto.id == 1L }.montoAsignado)
    }

    @Test fun invalidoONoExistenteNoLanzaExcepcion() = runTest {
        val vm = GastosViewModel()
        listOf(null, "", "abc", "0", "-1", "9223372036854775808", "999999").forEach { id ->
            val estado = vm.observarGastosSublote(id).first()
            assertNotNull(estado.error)
            assertTrue(estado.gastos.isEmpty())
        }
    }

    @Test fun cambioObservableActualizaSinReabrirPantalla() = runTest {
        val repo = DemoGastosRepository()
        val vm = GastosViewModel(repo)
        val estados = mutableListOf<GastosSubloteUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.observarGastosSublote("1").toList(estados) }
        assertTrue(estados.last().gastos.isEmpty())
        repo.guardar(gasto(), listOf(GastoSublote(1, 1, 50000)))
        runCurrent()
        assertEquals(BigInteger.valueOf(50000), estados.last().total)
    }

    @Test fun reiniciarBorradorConservaLosGastosAsignados() = runTest {
        val repo = DemoGastosRepository()
        repo.guardar(gasto(), listOf(GastoSublote(1, 1, 30000), GastoSublote(1, 2, 20000)))
        val vm = GastosViewModel(repo)
        vm.editar { it.copy(concepto = "Borrador sin guardar", monto = "9000") }
        vm.alternarSublote(1)
        vm.nuevoRegistro()
        assertTrue(vm.uiState.value.formulario.concepto.isEmpty())
        assertTrue(vm.uiState.value.formulario.asignaciones.isEmpty())
        val estado = vm.observarGastosSublote("1").first { !it.cargando }
        assertEquals(1, estado.gastos.size)
        assertEquals(BigInteger.valueOf(30000), estado.total)
    }

    @Test fun errorDeLecturaEsRecuperableYUnaNuevaConsultaPuedeFuncionar() = runTest {
        var falla = true
        val repo = object : GastosRepository {
            override val gastos = MutableStateFlow<List<Gasto>>(emptyList())
            override suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>) = Unit
            override fun observarGastosSublote(subloteId: Long): Flow<List<GastoDelSublote>> = flow {
                if (falla) error("Fallo simulado") else emit(emptyList())
            }
        }
        val vm = GastosViewModel(repo)
        assertTrue(vm.observarGastosSublote("1").last().recuperable)
        falla = false
        assertNull(vm.observarGastosSublote("1").last().error)
    }

    @Test fun totalNoDesbordaLong() {
        val filas = listOf(GastoDelSublote(gasto(), 1, Long.MAX_VALUE), GastoDelSublote(gasto(2), 1, 1))
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE) + BigInteger.ONE, GastosSubloteUiState(gastos = filas).total)
    }
}
