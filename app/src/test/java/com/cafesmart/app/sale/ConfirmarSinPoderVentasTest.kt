package com.cafesmart.app.sale

import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

// Si la venta no se puede confirmar, confirmarVenta() devuelve false y el inventario queda igual.
class ConfirmarSinPoderVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    @Test
    fun clienteYSubloteSinPrecio_devuelveFalseYNoCambiaElInventario() {
        val vm = viewModelDeVentasDePrueba()
        vm.escribirClienteValido()
        vm.elegir(5)
        val inventarioAntes = vm.uiState.value.inventario
        assertFalse(vm.uiState.value.puedeConfirmar)
        assertFalse(vm.confirmarVenta())
        assertEquals(inventarioAntes, vm.uiState.value.inventario)
        assertFalse(vm.uiState.value.ventaRegistrada)
    }
}
