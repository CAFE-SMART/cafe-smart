package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Doble toque en "Continuar" (cliente nuevo) y en "Guardar cambios": mientras el primero guarda en Room,
// el segundo se ignora. El repositorio falso se queda esperando para simular un guardado lento.
class DobleToqueClienteVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    private val repositorio = RepositorioDeVentasFalso(clientesDeEjemplo)
    private lateinit var viewModel: SaleViewModel

    // El ViewModel se crea aqui y no al declarar el campo: la regla ya puso el Main de pruebas.
    @Before
    fun preparar() { viewModel = viewModelDeVentasDePrueba(repositorio) }

    @Test
    fun dosToquesEnContinuarGuardanUnSoloClienteYAvisanUnaVez() {
        repositorio.esperarAlGuardarCliente = CompletableDeferred()
        viewModel.escribirClienteValido()
        var avisos = 0

        viewModel.continuarPaso1 { avisos++ }
        viewModel.continuarPaso1 { avisos++ }
        repositorio.esperarAlGuardarCliente!!.complete(Unit)

        assertEquals(1, repositorio.intentosDeGuardarCliente)
        assertEquals(4, repositorio.clientes.size)
        assertEquals(1, avisos)
        assertEquals(4, viewModel.uiState.value.clientes.size)
    }

    @Test
    fun dosToquesEnGuardarCambiosModificanUnaSolaVez() {
        repositorio.esperarAlGuardarCliente = CompletableDeferred()
        val carlos = viewModel.uiState.value.clientes[1]
        viewModel.editarCliente(carlos)
        viewModel.cambiarNombreEdicion("Carlos Mejía Ruiz")

        viewModel.guardarEdicion()
        viewModel.guardarEdicion()
        repositorio.esperarAlGuardarCliente!!.complete(Unit)

        assertEquals(1, repositorio.intentosDeActualizarCliente)
        assertEquals("Carlos Mejía Ruiz", viewModel.uiState.value.clientes[1].nombre)
        assertEquals(3, viewModel.uiState.value.clientes.size)
    }

    @Test
    fun siElGuardadoFallaSePuedeVolverAIntentar() {
        repositorio.fallarAlGuardarCliente = true
        viewModel.escribirClienteValido()
        var avisos = 0
        viewModel.continuarPaso1 { avisos++ }
        assertEquals(0, avisos)
        assertTrue(viewModel.uiState.value.errorCliente != null)

        repositorio.fallarAlGuardarCliente = false
        viewModel.continuarPaso1 { avisos++ }

        assertEquals(2, repositorio.intentosDeGuardarCliente)
        assertEquals(1, avisos)
        assertEquals(4, repositorio.clientes.size)
    }

    @Test
    fun siElDocumentoYaExistiaEnRoomSePuedeVolverAIntentar() {
        // Otra pantalla guardo ese documento y la copia de este ViewModel no lo sabe: lo frena el repositorio.
        repositorio.clientes += Cliente(id = 9, nombre = "Otra persona", documento = "1114059684")
        viewModel.escribirClienteValido()
        var avisos = 0
        viewModel.continuarPaso1 { avisos++ }
        assertEquals(0, avisos)
        assertEquals(1, repositorio.intentosDeGuardarCliente)

        // Con otro documento el segundo intento llega al repositorio y guarda.
        viewModel.cambiarDocumento("80123456")
        viewModel.continuarPaso1 { avisos++ }
        assertEquals(2, repositorio.intentosDeGuardarCliente)
        assertEquals(1, avisos)
    }

    // guardandoCliente: true mientras guarda y false al terminar (bien, documento repetido, error o fallo).
    @Test
    fun guardandoClienteSoloEstaEncendidoMientrasGuarda() {
        repositorio.esperarAlGuardarCliente = CompletableDeferred()
        viewModel.escribirClienteValido()
        assertFalse(viewModel.uiState.value.guardandoCliente)

        viewModel.continuarPaso1()
        assertTrue(viewModel.uiState.value.guardandoCliente)
        repositorio.esperarAlGuardarCliente!!.complete(Unit)
        assertFalse(viewModel.uiState.value.guardandoCliente)
    }

    @Test
    fun guardandoClienteSeApagaTambienConErrorYConDocumentoRepetido() {
        repositorio.fallarAlGuardarCliente = true
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        assertFalse(viewModel.uiState.value.guardandoCliente)

        repositorio.fallarAlGuardarCliente = false
        repositorio.clientes += Cliente(id = 9, nombre = "Otra persona", documento = "1114059684")
        viewModel.continuarPaso1()
        assertFalse(viewModel.uiState.value.guardandoCliente)
    }

    @Test
    fun guardandoClienteSeApagaSiSeCancelaElGuardado() {
        repositorio.esperarAlGuardarCliente = CompletableDeferred()
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        assertTrue(viewModel.uiState.value.guardandoCliente)

        // Se cancela la espera (como cuando la pantalla se destruye): el finally lo apaga.
        repositorio.esperarAlGuardarCliente!!.cancel()
        assertFalse(viewModel.uiState.value.guardandoCliente)
    }

    @Test
    fun guardandoClienteTambienCubreGuardarCambios() {
        repositorio.esperarAlGuardarCliente = CompletableDeferred()
        viewModel.editarCliente(viewModel.uiState.value.clientes[1])
        viewModel.cambiarNombreEdicion("Carlos Mejía Ruiz")
        viewModel.guardarEdicion()
        assertTrue(viewModel.uiState.value.guardandoCliente)
        repositorio.esperarAlGuardarCliente!!.complete(Unit)
        assertFalse(viewModel.uiState.value.guardandoCliente)
    }
}
