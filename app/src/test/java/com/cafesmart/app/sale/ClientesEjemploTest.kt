package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.purchase.limpiarDocumento
import com.cafesmart.app.viewmodel.purchase.validarDocumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Los clientes de ejemplo deben cumplir las mismas reglas que un cliente escrito a mano, para que al pasarlos
// a Room (o al usarlos en el paso 1 de Ventas) no aparezca un dato que la app misma no dejaría guardar.
class ClientesEjemploTest {

    @Test
    fun hayClientesDeEjemplo() {
        assertTrue(clientesDeEjemplo.isNotEmpty())
    }

    @Test
    fun cadaDocumentoCumpleLaReglaDeCedulaONitSinRepetirse() {
        clientesDeEjemplo.forEach { cliente ->
            // Se compara contra los demás clientes (sin él mismo) con la misma regla de Compras.
            val otros = clientesDeEjemplo.filter { it !== cliente }
                .map { Productor(nombre = it.nombre, documento = it.documento, tipoDocumento = it.tipoDocumento) }
            assertNull("Documento inválido: ${cliente.nombre}", validarDocumento(cliente.documento, cliente.tipoDocumento, otros))
        }
    }

    @Test
    fun losDocumentosSeGuardanSoloConDigitos() {
        // Igual que en Compras: sin puntos, comas, guiones ni espacios.
        clientesDeEjemplo.forEach { assertEquals(limpiarDocumento(it.documento), it.documento) }
    }

    @Test
    fun noHayNombresRepetidosNiVacios() {
        assertEquals(clientesDeEjemplo.size, clientesDeEjemplo.map { it.nombre }.toSet().size)
        clientesDeEjemplo.forEach { assertTrue(it.nombre.isNotBlank()) }
    }
}
