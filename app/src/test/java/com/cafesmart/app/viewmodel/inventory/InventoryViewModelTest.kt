package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.model.TipoCafe
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryViewModelTest {

    @Test
    fun alEmpezarMuestraElResumenDeEjemploMasReciente() {
        val vm = InventoryViewModel()
        assertEquals(OrdenInventario.MAS_RECIENTE, vm.uiState.value.orden)
        assertEquals(834.0, vm.uiState.value.resumen.totalKg, 1e-9)
        assertEquals(TipoCafe.VERDE, vm.uiState.value.resumen.tipos.first().tipo)
    }

    @Test
    fun cambiarElOrdenReordenaLaLista() {
        val vm = InventoryViewModel()
        vm.cambiarOrden(OrdenInventario.MAS_ANTIGUO)
        assertEquals(OrdenInventario.MAS_ANTIGUO, vm.uiState.value.orden)
        assertEquals(TipoCafe.TRILLADO, vm.uiState.value.resumen.tipos.first().tipo)
        // El total no cambia al reordenar.
        assertEquals(834.0, vm.uiState.value.resumen.totalKg, 1e-9)
    }
}
