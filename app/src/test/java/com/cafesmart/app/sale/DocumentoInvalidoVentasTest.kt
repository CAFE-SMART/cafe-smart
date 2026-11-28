package com.cafesmart.app.sale

import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertFalse
import org.junit.Test

// Un documento con formato invalido no deja avanzar al paso 2, aunque el nombre y el celular esten bien.
class DocumentoInvalidoVentasTest {

    @Test
    fun cedulaDeCincoNumeros_conNombreYCelularValidos_continuarSigueDeshabilitado() {
        val vm = SaleViewModel()
        vm.elegirModo(ModoCliente.NUEVO)
        vm.cambiarDocumento("12345")
        vm.cambiarNombre("Juan Pérez")
        vm.cambiarTelefono("3001234567")
        assertFalse(vm.uiState.value.continuarPaso1Habilitado)
    }
}
