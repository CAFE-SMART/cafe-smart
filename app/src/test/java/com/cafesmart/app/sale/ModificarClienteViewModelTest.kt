package com.cafesmart.app.sale

import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// "Modificar cliente" en la hoja "Clientes": igual que "Modificar productor" de Compras.
// Cada prueba es una accion de la vista (tocar "Editar", escribir, "Guardar cambios", "Cancelar").
class ModificarClienteViewModelTest {

    private val beneficiadora = clientesDeEjemplo[0]
    private val carlos = clientesDeEjemplo[1]

    // Un viewModel con "Editar" tocado en Carlos Mejía.
    private fun editandoACarlos() = SaleViewModel().apply { editarCliente(carlos) }

    @Test
    fun editar_cargaLosDatosDelClienteEnElFormulario() {
        val estado = editandoACarlos().uiState.value
        assertEquals(carlos, estado.clienteEnEdicion)
        assertEquals(carlos.nombre, estado.datosEdicion.nombre)
        assertEquals(carlos.documento, estado.datosEdicion.documento)
        assertEquals(TipoDocumento.CC, estado.datosEdicion.tipo)
        assertFalse(estado.edicionEsEmpresa)
    }

    @Test
    fun editar_conSusPropiosDatos_noMarcaErrorYSePuedeGuardar() {
        val estado = editandoACarlos().uiState.value
        // Su propio documento no cuenta como repetido.
        assertNull(estado.edicionDocumentoError)
        assertNull(estado.edicionNombreError)
        assertNull(estado.edicionTelefonoError)
        assertTrue(estado.guardarEdicionHabilitado)
    }

    @Test
    fun editar_unNit_marcaElFormularioComoEmpresa() {
        val viewModel = SaleViewModel().apply { editarCliente(beneficiadora) }
        assertTrue(viewModel.uiState.value.edicionEsEmpresa)
    }

    @Test
    fun cambiarTipo_aNit_marcaEmpresa() {
        val viewModel = editandoACarlos()
        viewModel.cambiarTipoEdicion(TipoDocumento.NIT)
        assertTrue(viewModel.uiState.value.edicionEsEmpresa)
    }

    @Test
    fun documentoDeOtroCliente_dalErrorYNoDejaGuardar() {
        val viewModel = editandoACarlos()
        viewModel.cambiarDocumentoEdicion(clientesDeEjemplo[2].documento)
        assertNotNull(viewModel.uiState.value.edicionDocumentoError)
        assertFalse(viewModel.uiState.value.guardarEdicionHabilitado)
    }

    @Test
    fun nombreIncompleto_dalErrorYNoDejaGuardar() {
        val viewModel = editandoACarlos()
        viewModel.cambiarNombreEdicion("Carlos")
        assertNotNull(viewModel.uiState.value.edicionNombreError)
        assertFalse(viewModel.uiState.value.guardarEdicionHabilitado)
    }

    @Test
    fun celularMalEscrito_dalErrorYNoDejaGuardar() {
        val viewModel = editandoACarlos()
        viewModel.cambiarTelefonoEdicion("12345")
        assertNotNull(viewModel.uiState.value.edicionTelefonoError)
        assertFalse(viewModel.uiState.value.guardarEdicionHabilitado)
    }

    @Test
    fun guardar_cambiaAlClienteEnLaListaSinMoverloDeLugar() {
        val viewModel = editandoACarlos()
        viewModel.cambiarNombreEdicion("Carlos Mejía Ruiz")
        viewModel.cambiarTelefonoEdicion("3001234567")
        viewModel.guardarEdicion()

        val estado = viewModel.uiState.value
        assertNull(estado.clienteEnEdicion)
        assertEquals(clientesDeEjemplo.size, estado.clientes.size)
        // Sigue en la misma posicion, con los datos nuevos y el mismo id.
        assertEquals("Carlos Mejía Ruiz", estado.clientes[1].nombre)
        assertEquals("3001234567", estado.clientes[1].telefono)
        assertEquals(carlos.id, estado.clientes[1].id)
        assertEquals(clientesDeEjemplo[0], estado.clientes[0])
        assertEquals(clientesDeEjemplo[2], estado.clientes[2])
    }

    @Test
    fun guardar_tambienCambiaAlClienteQueEstabaElegido() {
        val viewModel = SaleViewModel()
        viewModel.elegirCliente(carlos)
        viewModel.editarCliente(carlos)
        viewModel.cambiarNombreEdicion("Carlos Mejía Ruiz")
        viewModel.guardarEdicion()
        assertEquals("Carlos Mejía Ruiz", viewModel.uiState.value.cliente?.nombre)
    }

    @Test
    fun guardar_noTocaAOtroClienteElegido() {
        val viewModel = SaleViewModel()
        viewModel.elegirCliente(beneficiadora)
        viewModel.editarCliente(carlos)
        viewModel.cambiarNombreEdicion("Carlos Mejía Ruiz")
        viewModel.guardarEdicion()
        assertEquals(beneficiadora, viewModel.uiState.value.cliente)
    }

    @Test
    fun guardar_conErrores_noCambiaNada() {
        val viewModel = editandoACarlos()
        viewModel.cambiarNombreEdicion("Carlos")
        viewModel.guardarEdicion()

        val estado = viewModel.uiState.value
        // Sigue en el formulario y la lista quedo igual.
        assertEquals(carlos, estado.clienteEnEdicion)
        assertEquals(clientesDeEjemplo, estado.clientes)
    }

    @Test
    fun guardar_sinHaberTocadoEditar_noHaceNada() {
        val viewModel = SaleViewModel()
        viewModel.guardarEdicion()
        assertEquals(clientesDeEjemplo, viewModel.uiState.value.clientes)
    }

    @Test
    fun cancelar_vuelveALaListaSinGuardar() {
        val viewModel = editandoACarlos()
        viewModel.cambiarNombreEdicion("Otro Nombre")
        viewModel.cancelarEdicion()

        val estado = viewModel.uiState.value
        assertNull(estado.clienteEnEdicion)
        assertEquals(clientesDeEjemplo, estado.clientes)
    }

    @Test
    fun cerrarLaHoja_descartaLaModificacionQueQuedoAMedias() {
        val viewModel = editandoACarlos()
        viewModel.cerrarLista()
        assertNull(viewModel.uiState.value.clienteEnEdicion)
    }

    @Test
    fun editar_otraVez_cargaLosDatosDelNuevoClienteYNoLosDelAnterior() {
        val viewModel = editandoACarlos()
        viewModel.cambiarNombreEdicion("Otro Nombre")
        viewModel.cancelarEdicion()
        viewModel.editarCliente(beneficiadora)
        assertEquals(beneficiadora.nombre, viewModel.uiState.value.datosEdicion.nombre)
    }
}
