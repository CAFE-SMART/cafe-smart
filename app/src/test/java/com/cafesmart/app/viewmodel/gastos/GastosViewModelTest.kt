package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.DemoGastosRepository
import com.cafesmart.app.data.model.AplicaGasto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModelTest {
    @Before fun preparar() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun limpiar() { Dispatchers.resetMain() }

    @Test fun seleccionMultipleConservaCamposYMontosDeLosOtrosSublotes() = runTest {
        val vm = GastosViewModel()
        vm.editar { it.copy(concepto = "Transporte", descripcion = "Camión", monto = "12000") }
        vm.alternarSublote(1)
        vm.editar { it.copy(asignaciones = it.asignaciones + (1L to "5000")) }
        vm.alternarSublote(2)
        vm.editar { it.copy(asignaciones = it.asignaciones + (2L to "7000")) }
        assertEquals(2, vm.uiState.value.formulario.asignaciones.size)
        vm.alternarSublote(2)
        assertEquals(mapOf(1L to "5000"), vm.uiState.value.formulario.asignaciones)
        vm.alternarSublote(2)
        vm.editar { it.copy(asignaciones = it.asignaciones + (2L to "7000")) }
        val form = vm.uiState.value.formulario
        assertEquals("Transporte", form.concepto)
        assertEquals("Camión", form.descripcion)
        assertEquals("12000", form.monto)
        assertEquals(AplicaGasto.SUBLOTES, form.aplicaA)
        vm.solicitarConfirmacion()
        assertEquals(ResultadoGasto.CONFIRMACION, vm.uiState.value.resultado)
        vm.volverAEditar()
        assertEquals(form, vm.uiState.value.formulario)
    }

    @Test fun mantieneValidacionDeLaSumaYRechazaIdsDesconocidos() = runTest {
        val vm = GastosViewModel()
        vm.alternarSublote(9999)
        assertTrue(vm.uiState.value.formulario.asignaciones.isEmpty())
        vm.editar { it.copy(concepto = "Cargue", monto = "10000") }
        vm.alternarSublote(1)
        vm.editar { it.copy(asignaciones = mapOf(1L to "9000")) }
        vm.solicitarConfirmacion()
        assertNotNull(vm.uiState.value.errores.sublotes)
        assertEquals(ResultadoGasto.EDICION, vm.uiState.value.resultado)
    }

    @Test fun ejemplosSonOpcionalesNoDuplicanNiBorranElBorrador() = runTest {
        val vm = GastosViewModel()
        assertTrue(vm.uiState.value.gastos.isEmpty())
        vm.editar { it.copy(concepto = "Mi gasto", monto = "1000") }
        val form = vm.uiState.value.formulario
        vm.cargarEjemplos()
        val gastos = vm.uiState.value.gastos
        assertEquals(2, gastos.size)
        assertEquals(2, vm.uiState.value.visibles.size)
        assertEquals(form, vm.uiState.value.formulario)
        vm.cargarEjemplos()
        assertEquals(gastos, vm.uiState.value.gastos)
        vm.solicitarConfirmacion()
        vm.confirmar()
        vm.uiState.first { it.resultado == ResultadoGasto.EXITO }
        runCurrent()
        assertEquals(3, vm.uiState.value.gastos.size)
        assertEquals(3, vm.uiState.value.gastos.map { it.id }.distinct().size)
    }

    @Test fun ejemplosNoReemplazanUnGastoRegistrado() = runTest {
        val repo = DemoGastosRepository()
        val form = GastoFormulario(concepto = "Propio", monto = "2500")
        val (gasto, reparto) = GastosValidation.convertir(form, 1, emptyList())
        repo.guardar(gasto, reparto)
        repo.cargarEjemplos()
        assertEquals(listOf(gasto), repo.gastos.value)
    }

    @Test fun noPermiteCambiarSeleccionMientrasConfirma() = runTest {
        val vm = GastosViewModel()
        vm.editar { it.copy(concepto = "Transporte", monto = "1000") }
        vm.solicitarConfirmacion()
        vm.alternarSublote(1)
        assertTrue(vm.uiState.value.formulario.asignaciones.isEmpty())
    }

    @Test fun muestraRegistrosExistentesAntesDeIniciarElColector() = runTest {
        val repo = DemoGastosRepository()
        repo.cargarEjemplos()
        val vm = GastosViewModel(repository = repo)

        assertEquals(repo.gastos.value, vm.uiState.value.gastos)
        assertEquals(1, vm.uiState.value.visibles.size)
        vm.filtrar(FiltroGasto.TODOS)
        assertEquals(2, vm.uiState.value.visibles.size)
    }

    @Test fun deseleccionarTodosConservaElBorradorYExigeNuevaSeleccion() = runTest {
        val vm = GastosViewModel()
        vm.editar { it.copy(concepto = "Transporte", descripcion = "Entrega", monto = "12000") }
        vm.alternarSublote(1)
        vm.alternarSublote(2)
        val borrador = vm.uiState.value.formulario

        vm.alternarSublote(1)
        vm.alternarSublote(2)
        assertEquals(borrador.copy(asignaciones = emptyMap()), vm.uiState.value.formulario)
        vm.solicitarConfirmacion()
        assertEquals(ResultadoGasto.EDICION, vm.uiState.value.resultado)
        assertNotNull(vm.uiState.value.errores.sublotes)

        vm.editar { it.copy(aplicaA = AplicaGasto.GENERAL) }
        vm.solicitarConfirmacion()
        assertEquals(ResultadoGasto.CONFIRMACION, vm.uiState.value.resultado)
    }
}
