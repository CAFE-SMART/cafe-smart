package com.cafesmart.app.purchase

import com.cafesmart.app.data.purchase.Calidad
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.purchase.TipoDocumento
import com.cafesmart.app.data.purchase.productorSinRegistrar
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseViewModelTest {

    @Test
    fun alEmpezar_estaEnElPaso1SinOpcionElegida() {
        val viewModel = PurchaseViewModel()
        val estado = viewModel.uiState.value
        assertEquals(1, estado.paso)
        assertFalse(estado.registradoSeleccionado)
        assertFalse(estado.continuarPaso1Habilitado)
        assertEquals("Por favor, selecciona una opción para continuar el proceso.", estado.mensajePendientePaso1)
    }

    @Test
    fun sinRegistrar_pasaAlPaso2ConProductorSinRegistrar() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.SIN_REGISTRAR)
        assertTrue(viewModel.uiState.value.sinRegistrarSeleccionado)
        viewModel.continuarPaso1()
        val estado = viewModel.uiState.value
        assertEquals(2, estado.paso)
        assertEquals(productorSinRegistrar, estado.productorDeLaCompra)
    }

    @Test
    fun productorNuevo_seGuardaEnLaListaYQuedaElegido() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        viewModel.cambiarNombre("Pepito Pérez")
        viewModel.continuarPaso1()
        val estado = viewModel.uiState.value
        assertEquals(2, estado.paso)
        assertEquals(1, estado.productores.size)
        assertEquals("Pepito Pérez", estado.productorDeLaCompra?.nombre)
        assertEquals(NuevoProductor(), estado.nuevoProductor)
        // La lista ya muestra el texto listo para la vista
        assertEquals("Cédula 1114059684", estado.productoresEnPantalla[0].documento)
    }

    @Test
    fun continuarPaso1_noAvanzaSiFaltanDatos() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.REGISTRADO)
        viewModel.continuarPaso1()
        assertEquals(1, viewModel.uiState.value.paso)
        assertEquals("Elige un productor de la lista.", viewModel.uiState.value.mensajePendientePaso1)
    }

    @Test
    fun productorNuevo_limpiaLoEscrito() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarDocumento("12.345-678")
        viewModel.cambiarNombre("Ana2 Gómez!")
        viewModel.cambiarTelefono("300 123")
        val estado = viewModel.uiState.value
        assertTrue(estado.esEmpresa)
        assertEquals("12345678", estado.nuevoProductor.documento)
        assertEquals("Ana Gómez", estado.nuevoProductor.nombre)
        assertEquals("300123", estado.nuevoProductor.telefono)
    }

    @Test
    fun paso2_entregaTextosListosParaLaVista() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarTipoCafe(0, "Verde")
        viewModel.cambiarCalidad(0, Calidad.BUENA)
        viewModel.cambiarPeso(0, "25")
        viewModel.cambiarPrecio(0, "14.000")
        val estado = viewModel.uiState.value
        assertEquals("14000", estado.cafes[0].precio)
        assertEquals("14.000", estado.cafesEnPantalla[0].precio)
        assertEquals("25 kg", estado.totalKgTexto)
        assertEquals("$ 350.000", estado.totalEstimadoTexto)
        assertTrue(estado.cafesListos)
        assertNull(estado.mensajePendientePaso2)
    }

    @Test
    fun paso2_agregarYQuitarCafes() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarTipoCafe(0, "Verde")
        viewModel.cambiarCalidad(0, Calidad.BUENA)
        viewModel.cambiarPeso(0, "25")
        viewModel.cambiarPrecio(0, "14000")
        viewModel.agregarCafe()
        assertEquals(2, viewModel.uiState.value.cafesEnPantalla.size)
        assertTrue(viewModel.uiState.value.cafesEnPantalla[1].puedeQuitar)
        assertFalse(viewModel.uiState.value.cafesListos)

        viewModel.quitarCafe(1)
        assertEquals(1, viewModel.uiState.value.cafes.size)
        assertFalse(viewModel.uiState.value.cafesEnPantalla[0].puedeQuitar)
        assertEquals(1, viewModel.uiState.value.sublotes.size)
    }

    @Test
    fun cambiarFecha_desdeElCalendario() {
        val viewModel = PurchaseViewModel()
        // 15 de mayo de 2026 a medianoche UTC, como lo entrega el calendario
        viewModel.cambiarFecha(1778803200000L)
        val fecha = viewModel.uiState.value.fecha
        assertEquals(15, fecha.dia)
        assertEquals(5, fecha.mes)
        assertEquals(2026, fecha.anio)
    }
}
