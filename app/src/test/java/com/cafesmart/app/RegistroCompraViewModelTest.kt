package com.cafesmart.app

import com.cafesmart.app.data.CafeFormulario
import com.cafesmart.app.data.Calidad
import com.cafesmart.app.data.ModoProductor
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.TipoDocumento
import com.cafesmart.app.data.productorSinRegistrar
import com.cafesmart.app.viewmodel.RegistroCompraViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroCompraViewModelTest {

    private val cafeCompleto = CafeFormulario(tipoCafe = "Verde", calidad = Calidad.BUENA, peso = "25", precio = "14000")

    @Test
    fun alEmpezar_estaEnElPaso1SinOpcionElegida() {
        val viewModel = RegistroCompraViewModel()
        val estado = viewModel.uiState.value
        assertEquals(1, estado.paso)
        assertEquals(null, estado.modo)
        assertFalse(estado.continuarPaso1Habilitado)
    }

    @Test
    fun sinRegistrar_pasaAlPaso2ConProductorSinRegistrar() {
        val viewModel = RegistroCompraViewModel()
        viewModel.elegirModo(ModoProductor.SIN_REGISTRAR)
        viewModel.continuarPaso1()
        val estado = viewModel.uiState.value
        assertEquals(2, estado.paso)
        assertEquals(productorSinRegistrar, estado.productorDeLaCompra)
    }

    @Test
    fun productorNuevo_seGuardaEnLaListaYQuedaElegido() {
        val viewModel = RegistroCompraViewModel()
        viewModel.elegirModo(ModoProductor.NUEVO)
        viewModel.cambiarNuevoProductor(NuevoProductor(TipoDocumento.CC, "1114059684", "Pepito Pérez", ""))
        viewModel.continuarPaso1()
        val estado = viewModel.uiState.value
        assertEquals(2, estado.paso)
        assertEquals(1, estado.productores.size)
        assertEquals("Pepito Pérez", estado.productorDeLaCompra?.nombre)
        assertEquals(NuevoProductor(), estado.nuevoProductor)
    }

    @Test
    fun continuarPaso1_noAvanzaSiFaltanDatos() {
        val viewModel = RegistroCompraViewModel()
        viewModel.elegirModo(ModoProductor.REGISTRADO)
        viewModel.continuarPaso1()
        assertEquals(1, viewModel.uiState.value.paso)
    }

    @Test
    fun cambiarNuevoProductor_limpiaLoEscrito() {
        val viewModel = RegistroCompraViewModel()
        viewModel.cambiarNuevoProductor(NuevoProductor(TipoDocumento.CC, "12.345-678", "Ana2 Gómez!", "300 123"))
        val datos = viewModel.uiState.value.nuevoProductor
        assertEquals("12345678", datos.documento)
        assertEquals("Ana Gómez", datos.nombre)
        assertEquals("300123", datos.telefono)
    }

    @Test
    fun paso2_cambiarAgregarYQuitarCafes() {
        val viewModel = RegistroCompraViewModel()
        viewModel.cambiarCafe(0, cafeCompleto.copy(precio = "14.000"))
        assertEquals("14000", viewModel.uiState.value.cafes[0].precio)
        assertTrue(viewModel.uiState.value.cafesListos)
        assertEquals(350000.0, viewModel.uiState.value.totalEstimado, 0.001)

        viewModel.agregarCafe()
        assertEquals(2, viewModel.uiState.value.cafes.size)
        assertFalse(viewModel.uiState.value.cafesListos)

        viewModel.quitarCafe(1)
        assertEquals(1, viewModel.uiState.value.cafes.size)
        assertEquals(1, viewModel.uiState.value.sublotes.size)
    }
}
