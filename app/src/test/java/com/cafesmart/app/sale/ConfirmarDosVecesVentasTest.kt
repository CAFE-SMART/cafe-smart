package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// confirmarVenta() avisa si esta llamada registro la venta. Asi la pantalla solo navega una vez aunque
// la persona toque "Confirmar" dos veces seguidas.
class ConfirmarDosVecesVentasTest {

    // Una venta lista para confirmar: cliente nuevo valido, el sublote 5 (calidad buena) y su precio.
    private fun SaleViewModel.dejarListaParaConfirmar() {
        escribirClienteValido()
        elegir(5)
        cambiarPrecio(Calidad.BUENA, "5000")
    }

    @Test
    fun laPrimeraConfirmacionRegistraYDevuelveTrue() {
        val vm = SaleViewModel()
        vm.dejarListaParaConfirmar()
        assertTrue(vm.confirmarVenta())
        assertTrue(vm.uiState.value.ventaRegistrada)
    }

    @Test
    fun laSegundaConfirmacionNoHaceNadaYDevuelveFalse() {
        val vm = SaleViewModel()
        vm.dejarListaParaConfirmar()
        vm.confirmarVenta()
        val inventarioDespuesDeLaPrimera = vm.uiState.value.inventario
        assertFalse(vm.confirmarVenta())
        // No se descuenta dos veces del inventario y la venta sigue registrada.
        assertEquals(inventarioDespuesDeLaPrimera, vm.uiState.value.inventario)
        assertTrue(vm.uiState.value.ventaRegistrada)
    }

    @Test
    fun confirmarSinPoderConfirmar_devuelveFalseYNoRegistra() {
        val vm = SaleViewModel()
        assertFalse(vm.confirmarVenta())
        assertFalse(vm.uiState.value.ventaRegistrada)
    }
}
