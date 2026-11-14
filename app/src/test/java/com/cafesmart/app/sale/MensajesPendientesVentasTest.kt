package com.cafesmart.app.sale

import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.sale.MENSAJE_REVISA_CAMPOS
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Texto sobre "Continuar" que dice que falta en el paso 1 de Ventas.
// Regla de oro: hay mensaje justo cuando el boton esta deshabilitado (salvo el minimo de kg, que ya se ve en rojo).
class MensajesPendientesVentasTest {

    private fun SaleViewModel.paso1() = uiState.value.mensajePendientePaso1

    // ---------- Paso 1 ----------

    @Test
    fun paso1_sinOpcionPideElegirUna() {
        assertEquals("Por favor, selecciona una opción para continuar el proceso.", SaleViewModel().paso1())
    }

    @Test
    fun paso1_clienteRegistradoSinElegirPideElegirUno() {
        val vm = SaleViewModel().apply { elegirModo(ModoCliente.REGISTRADO) }
        assertEquals("Elige un cliente de la lista.", vm.paso1())
    }

    @Test
    fun paso1_clienteRegistradoElegidoNoTieneMensaje() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.REGISTRADO)
            elegirCliente(clientesDeEjemplo.first())
        }
        assertNull(vm.paso1())
        assertTrue(vm.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun paso1_sinRegistrarNoTieneMensaje() {
        val vm = SaleViewModel().apply { elegirModo(ModoCliente.SIN_REGISTRAR) }
        assertNull(vm.paso1())
    }

    @Test
    fun paso1_clienteNuevoVacioPideElDocumento() {
        val vm = SaleViewModel().apply { elegirModo(ModoCliente.NUEVO) }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe el documento del cliente.", vm.uiState.value.documentoError)
    }

    @Test
    fun paso1_clienteNuevoConDocumentoCortoMuestraSuError() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("123")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("La cédula debe tener entre 6 y 10 números.", vm.uiState.value.documentoError)
    }

    @Test
    fun paso1_clienteNuevoConDocumentoBuenoPideElNombre() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe el nombre del cliente.", vm.uiState.value.nombreError)
    }

    @Test
    fun paso1_clienteNuevoEmpresaSinNombrePideElNombreDeLaEmpresa() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarTipoDocumento(TipoDocumento.NIT)
            cambiarDocumento("800987654")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe el nombre de la empresa.", vm.uiState.value.nombreError)
    }

    @Test
    fun paso1_clienteNuevoConSoloUnNombreMuestraSuError() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
            cambiarNombre("Juan")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe tu nombre y apellido.", vm.uiState.value.nombreError)
    }

    @Test
    fun paso1_clienteNuevoConCelularMalMuestraSuError() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
            cambiarNombre("Juan Pérez")
            cambiarTelefono("123")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Recuerda, tu celular debe tener 10 números y empezar por el 3.", vm.uiState.value.telefonoError)
    }

    @Test
    fun paso1_clienteNuevoCompletoNoTieneMensajeYElBotonSeHabilita() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
            cambiarNombre("Juan Pérez")
            cambiarTelefono("3101234567")
        }
        assertNull(vm.paso1())
        assertTrue(vm.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun paso1_clienteNuevoSinCelularEsValidoPorqueEsOpcional() {
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
            cambiarNombre("Juan Pérez")
        }
        assertNull(vm.paso1())
    }

    @Test
    fun paso1_documentoYaRegistradoAvisa() {
        val repetido = clientesDeEjemplo.first().documento
        val vm = SaleViewModel().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento(repetido)
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Este documento ya está registrado. Verifica en tu lista.", vm.uiState.value.documentoError)
    }

    @Test
    fun paso1_hayMensajeJustoCuandoElBotonEstaDeshabilitado() {
        val vm = SaleViewModel()
        val pasos = listOf<(SaleViewModel) -> Unit>(
            { },
            { it.elegirModo(ModoCliente.REGISTRADO) },
            { it.elegirCliente(clientesDeEjemplo.first()) },
            { it.elegirModo(ModoCliente.SIN_REGISTRAR) },
            { it.elegirModo(ModoCliente.NUEVO) },
            { it.cambiarDocumento("1112223334") },
            { it.cambiarNombre("Juan Pérez") },
            { it.cambiarTelefono("99") },
            { it.cambiarTelefono("3101234567") }
        )
        pasos.forEach { paso ->
            paso(vm)
            val estado = vm.uiState.value
            assertEquals(estado.continuarPaso1Habilitado, estado.mensajePendientePaso1 == null)
        }
    }

    // ---------- Paso 2 ----------

    @Test
    fun paso1_conUnCampoMarcado_elErrorExactoEstaSoloEnElCampoYAbajoVaElAvisoGeneral() {
        val vm = SaleViewModel().apply { elegirModo(ModoCliente.NUEVO) }
        assertEquals("Escribe el documento del cliente.", vm.uiState.value.documentoError)
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        vm.cambiarDocumento("1112223334")
        assertNull(vm.uiState.value.documentoError)
        assertEquals("Escribe el nombre del cliente.", vm.uiState.value.nombreError)
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
    }
}
