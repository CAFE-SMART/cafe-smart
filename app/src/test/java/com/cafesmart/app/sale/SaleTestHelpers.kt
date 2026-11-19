package com.cafesmart.app.sale

import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.viewmodel.sale.SaleViewModel

// Ayudas que comparten las pruebas de Ventas.

// Deja en "Cliente nuevo" un formulario valido: cedula, nombre y celular.
fun SaleViewModel.escribirClienteValido() {
    elegirModo(ModoCliente.NUEVO)
    cambiarDocumento("1114059684")
    cambiarNombre("Juan Pérez")
    cambiarTelefono("3001234567")
}
