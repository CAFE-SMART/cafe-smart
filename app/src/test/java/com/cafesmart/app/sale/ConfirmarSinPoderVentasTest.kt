package com.cafesmart.app.sale

import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Si la venta no se puede confirmar, confirmarVenta() no hace nada: no guarda, no marca la venta y el inventario queda igual.
class ConfirmarSinPoderVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    private val repositorio = RepositorioDeVentasFalso(clientesDeEjemplo)

    @Test
    fun clienteYSubloteSinPrecio_noGuardaNadaYNoCambiaElInventario() {
        val vm = viewModelDeVentasDePrueba(repositorio)
        vm.escribirClienteValido()
        vm.elegir(5)
        val inventarioAntes = vm.uiState.value.inventario
        assertFalse(vm.uiState.value.puedeConfirmar)
        vm.confirmarVenta()
        assertFalse(vm.uiState.value.guardando)
        assertEquals(inventarioAntes, vm.uiState.value.inventario)
        assertFalse(vm.uiState.value.ventaRegistrada)
        // No se registro nada: el repositorio ni se enteró.
        assertEquals(0, repositorio.intentosDeRegistrar)
        assertTrue(repositorio.ventas.isEmpty())
    }
}
