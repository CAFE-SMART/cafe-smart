package com.cafesmart.app.sale

import com.cafesmart.app.viewmodel.sale.SaleViewModel
import com.cafesmart.app.viewmodel.sale.errorTotalKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// La suma de kg con decimales no debe quedar en 4.999999... cuando es de 5 kg: ese error de la computadora
// haria rechazar una venta que cumple el minimo de 5 kg.
// Sublotes de ejemplo (cafe seco): 5 (200 kg), 6 (100 kg) y 8 (150 kg) tienen kg de sobra.
class TotalKgDecimalesVentasTest {

    @Test
    fun cincoKgExactosSumadosEnTresSublotes_dan5ExactoYNoErrorDeMinimo() {
        val vm = SaleViewModel()
        vm.elegir(5, 6, 8)
        vm.cambiarCantidad(5, 0.1)
        vm.cambiarCantidad(6, 4.1)
        vm.cambiarCantidad(8, 0.8)
        val estado = vm.uiState.value
        // 0.1 + 4.1 + 0.8 en Double da 4.999999999999999; con delta 0.0 se exige el 5.0 exacto.
        assertEquals(5.0, estado.totalKg, 0.0)
        assertNull(errorTotalKg(estado.totalKg, hayElegidos = true))
    }

    @Test
    fun cuatroKgConNueveDecimalesSiSigueSiendoError() {
        val vm = SaleViewModel()
        vm.elegir(5, 6)
        vm.cambiarCantidad(5, 0.5)
        vm.cambiarCantidad(6, 4.4)
        // 4.9 kg: menos del minimo, el error se mantiene.
        assertEquals("La cantidad total a vender debe ser mínimo 5 kg.", errorTotalKg(vm.uiState.value.totalKg, hayElegidos = true))
    }
}
