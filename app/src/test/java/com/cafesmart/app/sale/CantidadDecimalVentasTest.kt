package com.cafesmart.app.sale

import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// El campo "Cantidad a vender" debe mostrar lo que la persona escribe: coma para decimales y punto para miles.
class CantidadDecimalVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    // Lo que muestra ahora el campo del sublote.
    private fun SaleViewModel.campoDe(id: Long): String =
        uiState.value.grupos.flatMap { it.sublotes }.first { it.sublote.sublote.id == id }.cantidadCampo

    // Escribe como el telefono: cada tecla se agrega a lo que el campo muestra en ese momento.
    private fun SaleViewModel.teclear(id: Long, teclas: String) {
        teclas.forEach { escribirCantidad(id, campoDe(id) + it) }
    }

    @Test
    fun alTeclearDecimales_elCampoMuestraLaComaYNoPierdeElDecimal() {
        val vm = viewModelDeVentasDePrueba()
        vm.elegir(5)
        vm.escribirCantidad(5, "")
        vm.teclear(5, "25,5")
        assertEquals("25,5", vm.campoDe(5))
    }

    @Test
    fun alTeclearMiles_elCampoMuestraElPuntoDeMiles() {
        val vm = viewModelDeVentasDePrueba()
        vm.elegir(5)
        vm.escribirCantidad(5, "")
        vm.teclear(5, "1500")
        assertEquals("1.500", vm.campoDe(5))
    }
}
