package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// El precio es por calidad: si ya no queda ningun sublote elegido de esa calidad, su precio no debe reaparecer.
// Sublotes de ejemplo (cafe seco): 5 y 6 son de calidad buena, 8 regular y 9 mala.
class PrecioAlDeseleccionarVentasTest {

    @Test
    fun alQuitarElUltimoSubloteDeUnaCalidad_suPrecioSeBorra() {
        val vm = SaleViewModel()
        vm.elegir(5)
        vm.cambiarPrecio(Calidad.BUENA, "5000")
        vm.alternarSublote(vm.sublote(5))
        assertNull(vm.uiState.value.precios[Calidad.BUENA])
    }

    @Test
    fun alQuitarUnSubloteSiQuedaOtroDeLaMismaCalidad_elPrecioSeConserva() {
        val vm = SaleViewModel()
        vm.elegir(5, 6)
        vm.cambiarPrecio(Calidad.BUENA, "5000")
        vm.alternarSublote(vm.sublote(5))
        assertEquals("5000", vm.uiState.value.precios[Calidad.BUENA])
    }

    @Test
    fun alQuitarSoloUnaCalidad_losPreciosDeLasOtrasSeConservan() {
        val vm = SaleViewModel()
        vm.elegir(5, 8)
        vm.cambiarPrecio(Calidad.BUENA, "5000")
        vm.cambiarPrecio(Calidad.REGULAR, "4000")
        vm.alternarSublote(vm.sublote(5))
        assertNull(vm.uiState.value.precios[Calidad.BUENA])
        assertEquals("4000", vm.uiState.value.precios[Calidad.REGULAR])
    }

    @Test
    fun alQuitarTodosConSeleccionarTodo_losPreciosSeBorran() {
        val vm = SaleViewModel()
        vm.alternarTodos()
        vm.cambiarPrecio(Calidad.BUENA, "5000")
        vm.cambiarPrecio(Calidad.REGULAR, "4000")
        vm.alternarTodos()
        assertEquals(emptyMap<Calidad, String>(), vm.uiState.value.precios)
    }
}
