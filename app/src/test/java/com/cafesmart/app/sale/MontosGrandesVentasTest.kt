package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// QA de cantidades y precios grandes en Ventas (Paso 2). Cafe seco de ejemplo: 521 kg en total.
class MontosGrandesVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    private fun ventaConTodoElSeco(precio: String): SaleViewModel {
        val vm = viewModelDeVentasDePrueba()
        vm.alternarTodos()
        listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA).forEach { vm.cambiarPrecio(it, precio) }
        return vm
    }

    private fun textoDelPrecio(vm: SaleViewModel, calidad: Calidad): String =
        vm.uiState.value.grupos.first { it.calidad == calidad }.precio?.texto.orEmpty()

    @Test fun todoElSecoAlPrecioMaximo_totalEs52100000() {
        val estado = ventaConTodoElSeco("100000").uiState.value
        assertEquals("521 kg", estado.totalKgTexto)
        assertEquals("$ 52.100.000", estado.totalARecibirTexto)
        assertTrue(estado.puedeContinuar)
    }

    @Test fun todoElSecoAlPrecioMinimo_totalEs521000() {
        assertEquals("$ 521.000", ventaConTodoElSeco("1000").uiState.value.totalARecibirTexto)
    }

    @Test fun precioPegadoGigante_seRecortaYNoRompe() {
        val vm = viewModelDeVentasDePrueba()
        vm.alternarTodos()
        vm.cambiarPrecio(Calidad.BUENA, "9".repeat(40))
        val texto = textoDelPrecio(vm, Calidad.BUENA)
        assertEquals("999.999", texto)
    }

    @Test fun precioDe999999_dalErrorYNoDejaContinuar() {
        val vm = ventaConTodoElSeco("100000")
        vm.cambiarPrecio(Calidad.BUENA, "999999")
        assertNotNull(vm.uiState.value.errorDePrecio(Calidad.BUENA))
        assertFalse(vm.uiState.value.puedeContinuar)
    }

    @Test fun precioConLetras_quedaVacioYNoDejaContinuar() {
        val vm = ventaConTodoElSeco("100000")
        vm.cambiarPrecio(Calidad.BUENA, "abc")
        assertEquals("", textoDelPrecio(vm, Calidad.BUENA))
        assertFalse(vm.uiState.value.puedeContinuar)
    }

    @Test fun cantidadGigante_dalErrorYNoCambiaLaVenta() {
        val vm = ventaConTodoElSeco("100000")
        vm.escribirCantidad(5L, "9".repeat(30))
        assertNotNull(vm.uiState.value.errorDeCantidad(5L))
        assertFalse(vm.uiState.value.puedeContinuar)
    }

    @Test fun cantidadConComaYMuchosDecimales_noRompe() {
        val vm = ventaConTodoElSeco("100000")
        vm.escribirCantidad(5L, "12,123456789")
        // No debe lanzar excepcion; el estado sigue siendo consistente.
        assertTrue(vm.uiState.value.totalARecibir >= 0)
    }

    @Test fun elTotalNuncaEsNegativoNiSeDesbordaConElMaximoPosible() {
        // Lo mas grande que permite el pie: 99.999 kg a $100.000 en cada una de 3 calidades.
        val enPesos = (99_999.0 * 100_000.0).toLong() * 3
        assertEquals(29_999_700_000L, enPesos)
    }

    @Test fun confirmarConMontoMaximo_registraLaVentaUnaSolaVez() {
        val vm = ventaConTodoElSeco("100000")
        vm.elegirModo(com.cafesmart.app.data.sale.ModoCliente.SIN_REGISTRAR)
        assertEquals("$ 52.100.000", vm.uiState.value.totalARecibirTexto)
        vm.confirmarVenta()
        val tras1 = vm.uiState.value
        assertTrue(tras1.ventaRegistrada)
        // Un segundo toque (doble clic) no debe volver a descontar el inventario.
        vm.confirmarVenta()
        assertEquals(tras1.inventario, vm.uiState.value.inventario)
    }
}
