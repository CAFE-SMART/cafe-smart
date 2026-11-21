package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.GastoDelSublote
import com.cafesmart.app.data.gastos.GastosRepository
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.GastoSublote
import com.cafesmart.app.data.model.Sublote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El ViewModel de la app real no recibe una lista fija de sublotes: los lee del repositorio (Room). */
@OptIn(ExperimentalCoroutinesApi::class)
class GastosFuenteDeSublotesTest {
    @Before fun preparar() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun limpiar() { Dispatchers.resetMain() }

    private fun sublote(id: Long, disponibleKg: Double = 10.0) = Sublote(
        id = id, loteId = 1, pesoInicialKg = 10.0, pesoDisponibleKg = disponibleKg, costoKg = 1_000, fechaIngreso = Fecha(1, 11, 2026),
    )

    /** Repositorio falso: [disponibles] se puede cambiar para simular una compra nueva; [todos] incluye los agotados. */
    private class RepoConSublotes(disponibles: List<Sublote>, private val todos: List<Sublote> = disponibles) : GastosRepository {
        val disponibles = MutableStateFlow(disponibles)
        override val gastos = MutableStateFlow<List<Gasto>>(emptyList())
        override suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>) = Unit
        override fun observarGastosSublote(subloteId: Long): Flow<List<GastoDelSublote>> = flowOf(emptyList())
        override fun observarSublotesDisponibles(): Flow<List<Sublote>> = disponibles
        override suspend fun buscarSublote(id: Long): Sublote? = todos.find { it.id == id }
    }

    @Test fun conSublotesNullLosLeeDelRepositorioYSeActualizanSolos() = runTest {
        val repo = RepoConSublotes(listOf(sublote(1)))
        val vm = GastosViewModel(repo, sublotes = null)
        assertTrue(vm.uiState.value.sublotes.isEmpty())
        runCurrent()
        assertEquals(listOf(1L), vm.uiState.value.sublotes.map { it.id })
        repo.disponibles.value = listOf(sublote(1), sublote(2))
        runCurrent()
        assertEquals(listOf(1L, 2L), vm.uiState.value.sublotes.map { it.id })
    }

    @Test fun conUnaListaFijaNoSeLeeNadaDelRepositorio() = runTest {
        val vm = GastosViewModel(RepoConSublotes(listOf(sublote(1))), sublotes = listOf(sublote(7)))
        runCurrent()
        assertEquals(listOf(7L), vm.uiState.value.sublotes.map { it.id })
    }

    @Test fun soloSePuedenElegirSublotesQueLaFuenteEntrega() = runTest {
        val vm = GastosViewModel(RepoConSublotes(listOf(sublote(1))), sublotes = null)
        runCurrent()
        vm.alternarSublote(99)
        assertTrue(vm.uiState.value.formulario.asignaciones.isEmpty())
        vm.alternarSublote(1)
        assertEquals(setOf(1L), vm.uiState.value.formulario.asignaciones.keys)
    }

    @Test fun laPantallaDelSubloteEncuentraUnoAgotadoConsultandoLaFuente() = runTest {
        val agotado = sublote(5, disponibleKg = 0.0)
        val vm = GastosViewModel(RepoConSublotes(listOf(sublote(1)), todos = listOf(sublote(1), agotado)), sublotes = null)
        runCurrent()
        val estado = vm.observarGastosSublote("5").first { !it.cargando }
        assertNull(estado.error)
        assertEquals(5L, estado.sublote?.id)
    }

    @Test fun unSubloteQueNoExisteEnNingunaFuenteDaError() = runTest {
        val vm = GastosViewModel(RepoConSublotes(listOf(sublote(1))), sublotes = null)
        runCurrent()
        val estado = vm.observarGastosSublote("404").first()
        assertNotNull(estado.error)
        assertFalse(estado.recuperable)
        assertNull(estado.sublote)
    }

    @Test fun soloLaDemostracionEnMemoriaPermiteDatosDeEjemplo() {
        assertTrue(GastosViewModel().permiteEjemplos)
        assertFalse(GastosViewModel(RepoConSublotes(emptyList()), sublotes = null).permiteEjemplos)
    }

    @Test fun conUnaFuenteSinGastosNoSeInventanEjemplos() = runTest {
        val vm = GastosViewModel(RepoConSublotes(emptyList()), sublotes = null)
        vm.cargarEjemplos()
        runCurrent()
        assertTrue(vm.uiState.value.gastos.isEmpty())
    }
}
