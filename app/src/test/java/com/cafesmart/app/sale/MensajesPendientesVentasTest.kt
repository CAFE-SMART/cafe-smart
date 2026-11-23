package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.purchase.MENSAJE_PRECIO
import com.cafesmart.app.viewmodel.sale.MENSAJE_REVISA_CAMPOS
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import com.cafesmart.app.viewmodel.sale.mensajePrecioPendiente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Texto sobre "Continuar" que dice que falta en el paso 1 y en el paso 2 de Ventas.
// Regla de oro: hay mensaje justo cuando el boton esta deshabilitado (salvo el minimo de kg, que ya se ve en rojo).
class MensajesPendientesVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    private fun SaleViewModel.paso1() = uiState.value.mensajePendientePaso1
    private fun SaleViewModel.paso2() = uiState.value.mensajePendientePaso2

    // ---------- Paso 1 ----------

    @Test
    fun paso1_sinOpcionPideElegirUna() {
        assertEquals("Por favor, selecciona una opción para continuar el proceso.", viewModelDeVentasDePrueba().paso1())
    }

    @Test
    fun paso1_clienteRegistradoSinElegirPideElegirUno() {
        val vm = viewModelDeVentasDePrueba().apply { elegirModo(ModoCliente.REGISTRADO) }
        assertEquals("Elige un cliente de la lista.", vm.paso1())
    }

    @Test
    fun paso1_clienteRegistradoElegidoNoTieneMensaje() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegirModo(ModoCliente.REGISTRADO)
            elegirCliente(clientesDeEjemplo.first())
        }
        assertNull(vm.paso1())
        assertTrue(vm.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun paso1_sinRegistrarNoTieneMensaje() {
        val vm = viewModelDeVentasDePrueba().apply { elegirModo(ModoCliente.SIN_REGISTRAR) }
        assertNull(vm.paso1())
    }

    @Test
    fun paso1_clienteNuevoVacioPideElDocumento() {
        val vm = viewModelDeVentasDePrueba().apply { elegirModo(ModoCliente.NUEVO) }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe el documento del cliente.", vm.uiState.value.documentoError)
    }

    @Test
    fun paso1_clienteNuevoConDocumentoCortoMuestraSuError() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("123")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("La cédula debe tener entre 6 y 10 números.", vm.uiState.value.documentoError)
    }

    @Test
    fun paso1_clienteNuevoConDocumentoBuenoPideElNombre() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe el nombre del cliente.", vm.uiState.value.nombreError)
    }

    @Test
    fun paso1_clienteNuevoEmpresaSinNombrePideElNombreDeLaEmpresa() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarTipoDocumento(TipoDocumento.NIT)
            cambiarDocumento("800987654")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe el nombre de la empresa.", vm.uiState.value.nombreError)
    }

    @Test
    fun paso1_clienteNuevoConSoloUnNombreMuestraSuError() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
            cambiarNombre("Juan")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Escribe tu nombre y apellido.", vm.uiState.value.nombreError)
    }

    @Test
    fun paso1_clienteNuevoConCelularMalMuestraSuError() {
        val vm = viewModelDeVentasDePrueba().apply {
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
        val vm = viewModelDeVentasDePrueba().apply {
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
        val vm = viewModelDeVentasDePrueba().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento("1112223334")
            cambiarNombre("Juan Pérez")
        }
        assertNull(vm.paso1())
    }

    @Test
    fun paso1_documentoYaRegistradoAvisa() {
        val repetido = clientesDeEjemplo.first().documento
        val vm = viewModelDeVentasDePrueba().apply {
            elegirModo(ModoCliente.NUEVO)
            cambiarDocumento(repetido)
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        assertEquals("Este documento ya está registrado. Verifica en tu lista.", vm.uiState.value.documentoError)
    }

    @Test
    fun paso1_hayMensajeJustoCuandoElBotonEstaDeshabilitado() {
        val vm = viewModelDeVentasDePrueba()
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
    fun paso2_sinSubloteElegidoPideElegirUno() {
        assertEquals("Elige al menos un sublote para continuar.", viewModelDeVentasDePrueba().paso2())
    }

    @Test
    fun paso2_conSubloteSinPrecioPideElPrecioDeEsaCalidad() {
        val vm = viewModelDeVentasDePrueba().apply { elegir(5) }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso2())
        assertEquals("Escribe el precio por kg de la calidad buena.", vm.uiState.value.grupos.first { it.precio != null }.precio!!.error)
    }

    @Test
    fun paso2_conPrecioFueraDeRangoMuestraElRango() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5)
            cambiarPrecio(Calidad.BUENA, "500000")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso2())
        assertEquals(MENSAJE_PRECIO, vm.uiState.value.grupos.first { it.precio != null }.precio!!.error)
    }

    @Test
    fun paso2_conSubloteYPrecioBuenoNoTieneMensaje() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5)
            cambiarPrecio(Calidad.BUENA, "20000")
        }
        assertNull(vm.paso2())
        assertTrue(vm.uiState.value.puedeContinuar)
    }

    @Test
    fun paso2_cantidadQuePasaDeLoDisponibleAvisa() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5)
            cambiarPrecio(Calidad.BUENA, "20000")
            escribirCantidad(5L, "9999")
        }
        assertNotNull(vm.paso2())
        assertTrue(!vm.uiState.value.puedeContinuar)
    }

    @Test
    fun paso2_siFaltaElMinimoDeKgElMensajeNoSeRepite() {
        // Menos de 5 kg: el aviso rojo de arriba ya lo explica, asi que debajo del boton no se repite.
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5)
            cambiarPrecio(Calidad.BUENA, "20000")
            escribirCantidad(5L, "2")
        }
        assertNotNull(vm.uiState.value.errorDeTotal)
        assertNull(vm.paso2())
        assertTrue(!vm.uiState.value.puedeContinuar)
    }

    @Test
    fun paso2_conDosCalidadesPideElPrecioDeLaQueFalta() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5, 8)
            cambiarPrecio(Calidad.BUENA, "20000")
        }
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso2())
        vm.cambiarPrecio(Calidad.REGULAR, "15000")
        assertNull(vm.paso2())
    }

    @Test
    fun paso2_alQuitarElSubloteVuelveAPedirElegirUno() {
        val vm = viewModelDeVentasDePrueba().apply { elegir(5) }
        vm.elegir(5)
        assertEquals("Elige al menos un sublote para continuar.", vm.paso2())
    }

    // ---------- Regla suelta del precio ----------

    @Test
    fun mensajePrecioPendiente_vacioValidoYFueraDeRango() {
        assertEquals("Escribe el precio por kg de la calidad mala.", mensajePrecioPendiente(Calidad.MALA, ""))
        assertNull(mensajePrecioPendiente(Calidad.MALA, "1000"))
        assertNull(mensajePrecioPendiente(Calidad.MALA, "100000"))
        assertEquals(MENSAJE_PRECIO, mensajePrecioPendiente(Calidad.MALA, "999"))
        assertEquals(MENSAJE_PRECIO, mensajePrecioPendiente(Calidad.MALA, "100001"))
    }

    // ---------- El campo que falta se marca con su error ----------

    @Test
    fun paso2_conSubloteSinPrecio_elCampoDelPrecioMuestraQueFalta() {
        val vm = viewModelDeVentasDePrueba().apply { elegir(5) }
        val precio = vm.uiState.value.grupos.first { it.precio != null }.precio!!
        assertEquals("Escribe el precio por kg de la calidad buena.", precio.error)
    }

    @Test
    fun paso2_alEscribirElPrecio_elCampoYaNoSeMarca() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5)
            cambiarPrecio(Calidad.BUENA, "20000")
        }
        assertNull(vm.uiState.value.grupos.first { it.precio != null }.precio!!.error)
    }

    @Test
    fun paso1_conUnCampoMarcado_elErrorExactoEstaSoloEnElCampoYAbajoVaElAvisoGeneral() {
        val vm = viewModelDeVentasDePrueba().apply { elegirModo(ModoCliente.NUEVO) }
        assertEquals("Escribe el documento del cliente.", vm.uiState.value.documentoError)
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
        vm.cambiarDocumento("1112223334")
        assertNull(vm.uiState.value.documentoError)
        assertEquals("Escribe el nombre del cliente.", vm.uiState.value.nombreError)
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso1())
    }

    @Test
    fun paso2_cantidadVaciaSinCampoEnRojoDiceQueEscribirla() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5)
            cambiarPrecio(Calidad.BUENA, "20000")
            escribirCantidad(5L, "")
        }
        assertEquals("Escribe la cantidad a vender de cada sublote elegido.", vm.paso2())
    }

    @Test
    fun paso2_cantidadQuePasaDeLoDisponible_marcaElCampoYAbajoVaElAvisoGeneral() {
        val vm = viewModelDeVentasDePrueba().apply {
            elegir(5)
            cambiarPrecio(Calidad.BUENA, "20000")
            escribirCantidad(5L, "9999")
        }
        assertNotNull(vm.uiState.value.grupos.first { it.precio != null }.sublotes.first { it.elegido }.errorCantidad)
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.paso2())
    }
}
