package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// confirmarVenta() guarda en segundo plano y no hace nada si ya se esta guardando o ya se registro.
// Asi un segundo toque en "Confirmar" no registra otra venta (la pantalla navega al ver ventaRegistrada).
class ConfirmarDosVecesVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    // Se guarda el repositorio para comprobar cuantas ventas llegaron a registrarse.
    private val repositorio = RepositorioDeVentasFalso(clientesDeEjemplo)

    // Una venta lista para confirmar: cliente nuevo valido, el sublote 5 (calidad buena) y su precio.
    private fun SaleViewModel.dejarListaParaConfirmar() {
        escribirClienteValido()
        elegir(5)
        cambiarPrecio(Calidad.BUENA, "5000")
    }

    @Test
    fun laPrimeraConfirmacionRegistra() {
        val vm = viewModelDeVentasDePrueba(repositorio)
        vm.dejarListaParaConfirmar()
        vm.confirmarVenta()
        assertTrue(vm.uiState.value.ventaRegistrada)
        assertEquals(1, repositorio.ventas.size)
    }

    @Test
    fun laSegundaConfirmacionNoHaceNada() {
        val vm = viewModelDeVentasDePrueba(repositorio)
        vm.dejarListaParaConfirmar()
        vm.confirmarVenta()
        val inventarioDespuesDeLaPrimera = vm.uiState.value.inventario
        vm.confirmarVenta()
        // No se descuenta dos veces del inventario y la venta sigue registrada.
        assertEquals(inventarioDespuesDeLaPrimera, vm.uiState.value.inventario)
        assertTrue(vm.uiState.value.ventaRegistrada)
        // Y en el repositorio queda UNA sola venta: la segunda ni llego.
        assertEquals(1, repositorio.ventas.size)
        assertEquals(1, repositorio.intentosDeRegistrar)
    }

    @Test
    fun confirmarSinPoderConfirmar_noRegistra() {
        val vm = viewModelDeVentasDePrueba(repositorio)
        vm.confirmarVenta()
        assertFalse(vm.uiState.value.ventaRegistrada)
        assertEquals(0, repositorio.intentosDeRegistrar)
        assertTrue(repositorio.ventas.isEmpty())
    }
}
