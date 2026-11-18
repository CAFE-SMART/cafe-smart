package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

// Un total de 5 kg armado con decimales (0.1 + 4.1 + 0.8) cumple el minimo y deja continuar el paso 2.
// Sublotes de ejemplo: 5 y 6 son de calidad buena, 8 regular.
class TotalCincoKgAceptadoVentasTest {

    @Test
    fun cincoKgConDecimalesEnTresSublotes_conPreciosValidos_puedeContinuar() {
        val vm = SaleViewModel()
        vm.elegir(5, 6, 8)
        vm.cambiarCantidad(5, 0.1)
        vm.cambiarCantidad(6, 4.1)
        vm.cambiarCantidad(8, 0.8)
        vm.cambiarPrecio(Calidad.BUENA, "5000")
        vm.cambiarPrecio(Calidad.REGULAR, "4000")
        assertTrue(vm.uiState.value.puedeContinuar)
    }
}
