package com.cafesmart.app.sale

import com.cafesmart.app.data.model.SubloteConLote
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

// El sublote de ejemplo con ese id (5 y 6 buena, 8 regular, 9 mala; todos de cafe seco).
fun SaleViewModel.sublote(id: Long): SubloteConLote = uiState.value.inventario.first { it.sublote.id == id }

// Marca los sublotes con esos ids.
fun SaleViewModel.elegir(vararg ids: Long) = ids.forEach { alternarSublote(sublote(it)) }
