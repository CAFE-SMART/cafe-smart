package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.NuevoCliente
import com.cafesmart.app.viewmodel.sale.clienteValido
import com.cafesmart.app.viewmodel.sale.clientesDeLaLista
import com.cafesmart.app.viewmodel.sale.clientesVisibles
import com.cafesmart.app.viewmodel.sale.crearCliente
import com.cafesmart.app.viewmodel.sale.filtrarClientes
import com.cafesmart.app.viewmodel.sale.hayMasClientes
import com.cafesmart.app.viewmodel.sale.puedeContinuarPaso1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Reglas del paso 1 de Ventas (cliente): la lista, la busqueda, el formulario "Cliente nuevo" y "Continuar".
class ClienteRulesTest {

    // Del mas antiguo al mas reciente, como se guardan.
    private val antiguo = Cliente(nombre = "Ana Gómez", documento = "123456789")
    private val medio = Cliente(nombre = "Café Los Alpes", documento = "9001234567", tipoDocumento = TipoDocumento.NIT)
    private val reciente = Cliente(nombre = "Carlos Mejía", documento = "16780012")
    private val clientes = listOf(antiguo, medio, reciente)

    private val formularioValido = NuevoCliente(TipoDocumento.CC, "1114059684", "Juan Pérez", "3001234567")

    // ---------- filtrarClientes ----------

    @Test
    fun filtrar_sinBusquedaDevuelveTodos() {
        assertEquals(clientes, filtrarClientes(clientes, ""))
    }

    @Test
    fun filtrar_conSoloEspaciosDevuelveTodos() {
        assertEquals(clientes, filtrarClientes(clientes, "    "))
    }

    @Test
    fun filtrar_porNombreSinImportarMayusculas() {
        assertEquals(listOf(antiguo), filtrarClientes(clientes, "ana"))
        assertEquals(listOf(reciente), filtrarClientes(clientes, "CARLOS"))
    }

    @Test
    fun filtrar_porDocumento() {
        assertEquals(listOf(medio), filtrarClientes(clientes, "9001234567"))
        assertEquals(listOf(reciente), filtrarClientes(clientes, "16780"))
    }

    @Test
    fun filtrar_puedeEncontrarVarios() {
        // "a" esta en los tres nombres
        assertEquals(clientes, filtrarClientes(clientes, "a"))
    }

    @Test
    fun filtrar_sinCoincidenciasDevuelveListaVacia() {
        assertEquals(emptyList<Cliente>(), filtrarClientes(clientes, "zzz"))
        assertEquals(emptyList<Cliente>(), filtrarClientes(emptyList(), "ana"))
    }

    @Test
    fun filtrar_noModificaLaListaOriginal() {
        filtrarClientes(clientes, "ana")
        assertEquals(listOf(antiguo, medio, reciente), clientes)
    }

    // ---------- clientesVisibles ("Recientes") ----------

    @Test
    fun visibles_sinBusquedaSonLosDosMasRecientesElUltimoPrimero() {
        assertEquals(listOf(reciente, medio), clientesVisibles(clientes, ""))
    }

    @Test
    fun visibles_conPocosClientesMuestraLosQueHay() {
        assertEquals(listOf(antiguo), clientesVisibles(listOf(antiguo), ""))
        assertEquals(emptyList<Cliente>(), clientesVisibles(emptyList(), ""))
    }

    @Test
    fun visibles_conBusquedaMuestraTodosLosQueCoincidenSinLimite() {
        val muchos = (1..5).map { Cliente(nombre = "Juan Pérez", documento = "10000000$it") }
        assertEquals(5, clientesVisibles(muchos, "juan").size)
    }

    @Test
    fun visibles_conBusquedaTambienVaDelMasRecienteAlMasAntiguo() {
        assertEquals(listOf(reciente, medio, antiguo), clientesVisibles(clientes, "a"))
    }

    @Test
    fun visibles_busquedaDeEspaciosEsComoNoBuscar() {
        assertEquals(listOf(reciente, medio), clientesVisibles(clientes, "   "))
    }

    // ---------- hayMasClientes ("Ver más clientes") ----------

    @Test
    fun hayMas_cuandoHayMasDeLosRecientesYNoSeBusca() {
        assertTrue(hayMasClientes(clientes, ""))
    }

    @Test
    fun hayMas_noSeMuestraConDosOMenosClientes() {
        assertFalse(hayMasClientes(listOf(antiguo, medio), ""))
        assertFalse(hayMasClientes(listOf(antiguo), ""))
        assertFalse(hayMasClientes(emptyList(), ""))
    }

    @Test
    fun hayMas_noSeMuestraMientrasSeBusca() {
        assertFalse(hayMasClientes(clientes, "ana"))
    }

    // ---------- clientesDeLaLista (hoja "Clientes") ----------

    @Test
    fun lista_muestraTodosDelMasRecienteAlMasAntiguo() {
        assertEquals(listOf(reciente, medio, antiguo), clientesDeLaLista(clientes, ""))
    }

    @Test
    fun lista_filtraConSuPropiaBusqueda() {
        assertEquals(listOf(medio), clientesDeLaLista(clientes, "alpes"))
        assertEquals(emptyList<Cliente>(), clientesDeLaLista(clientes, "zzz"))
    }

    // ---------- clienteValido (formulario "Cliente nuevo") ----------

    @Test
    fun formulario_completoYBienEscritoEsValido() {
        assertTrue(clienteValido(formularioValido, clientes))
    }

    @Test
    fun formulario_elCelularEsOpcional() {
        assertTrue(clienteValido(formularioValido.copy(telefono = ""), clientes))
    }

    @Test
    fun formulario_vacioNoEsValido() {
        assertFalse(clienteValido(NuevoCliente(), clientes))
    }

    @Test
    fun formulario_cedulaDeSeisADiezDigitos() {
        assertFalse(clienteValido(formularioValido.copy(documento = "12345"), clientes))
        assertTrue(clienteValido(formularioValido.copy(documento = "123456"), clientes))
        assertTrue(clienteValido(formularioValido.copy(documento = "1234567890"), clientes))
    }

    @Test
    fun formulario_nitDeNueveADiezDigitos() {
        val empresa = NuevoCliente(TipoDocumento.NIT, "900123457", "Café Los Andes", "")
        assertFalse(clienteValido(empresa.copy(documento = "90012345"), clientes))
        assertTrue(clienteValido(empresa, clientes))
        assertTrue(clienteValido(empresa.copy(documento = "9001234571"), clientes))
    }

    @Test
    fun formulario_sinDocumentoNoEsValido() {
        assertFalse(clienteValido(formularioValido.copy(documento = ""), clientes))
    }

    @Test
    fun formulario_documentoRepetidoNoEsValido() {
        assertFalse(clienteValido(formularioValido.copy(documento = "16780012"), clientes))
        assertFalse(clienteValido(formularioValido.copy(documento = "9001234567"), clientes))
    }

    @Test
    fun formulario_elMismoDocumentoEsValidoSiAunNoHayClientes() {
        assertTrue(clienteValido(formularioValido.copy(documento = "16780012"), emptyList()))
    }

    @Test
    fun formulario_personaNecesitaNombreYApellido() {
        assertFalse(clienteValido(formularioValido.copy(nombre = ""), clientes))
        assertFalse(clienteValido(formularioValido.copy(nombre = "Juan"), clientes))
        assertFalse(clienteValido(formularioValido.copy(nombre = "Juan P"), clientes))
        assertTrue(clienteValido(formularioValido.copy(nombre = "Juan Pé"), clientes))
    }

    @Test
    fun formulario_empresaSoloNecesitaSuNombre() {
        val empresa = NuevoCliente(TipoDocumento.NIT, "900123457", "Alpes", "")
        assertTrue(clienteValido(empresa, clientes))
        assertFalse(clienteValido(empresa.copy(nombre = ""), clientes))
    }

    @Test
    fun formulario_celularDebeTenerDiezNumerosYEmpezarPorTres() {
        assertFalse(clienteValido(formularioValido.copy(telefono = "6012345678"), clientes))
        assertFalse(clienteValido(formularioValido.copy(telefono = "300123"), clientes))
        assertFalse(clienteValido(formularioValido.copy(telefono = "30012345678"), clientes))
        assertTrue(clienteValido(formularioValido.copy(telefono = "3109876543"), clientes))
    }

    // ---------- crearCliente ----------

    @Test
    fun crear_guardaLosDatosDelFormulario() {
        assertEquals(
            Cliente(nombre = "Juan Pérez", documento = "1114059684", tipoDocumento = TipoDocumento.CC, telefono = "3001234567"),
            crearCliente(formularioValido)
        )
    }

    @Test
    fun crear_quitaLosEspaciosDeMasDelNombre() {
        assertEquals("Juan Pérez", crearCliente(formularioValido.copy(nombre = "  Juan    Pérez ")).nombre)
    }

    @Test
    fun crear_conservaElTipoNit() {
        assertEquals(TipoDocumento.NIT, crearCliente(formularioValido.copy(tipo = TipoDocumento.NIT)).tipoDocumento)
    }

    @Test
    fun crear_sinCelularLoDejaVacio() {
        assertEquals("", crearCliente(formularioValido.copy(telefono = "")).telefono)
    }

    // ---------- puedeContinuarPaso1 ----------

    @Test
    fun continuar_sinOpcionElegidaNoSePuede() {
        assertFalse(puedeContinuarPaso1(null, null, NuevoCliente(), clientes))
        // Aunque haya un cliente marcado de antes, sin opcion no se continua.
        assertFalse(puedeContinuarPaso1(null, reciente, formularioValido, clientes))
    }

    @Test
    fun continuar_registradoPideHaberElegidoACliente() {
        assertFalse(puedeContinuarPaso1(ModoCliente.REGISTRADO, null, NuevoCliente(), clientes))
        assertTrue(puedeContinuarPaso1(ModoCliente.REGISTRADO, reciente, NuevoCliente(), clientes))
    }

    @Test
    fun continuar_sinRegistrarSiempreSePuede() {
        assertTrue(puedeContinuarPaso1(ModoCliente.SIN_REGISTRAR, null, NuevoCliente(), clientes))
    }

    @Test
    fun continuar_clienteNuevoPideElFormularioValido() {
        assertFalse(puedeContinuarPaso1(ModoCliente.NUEVO, null, NuevoCliente(), clientes))
        assertFalse(puedeContinuarPaso1(ModoCliente.NUEVO, null, formularioValido.copy(nombre = "Juan"), clientes))
        assertTrue(puedeContinuarPaso1(ModoCliente.NUEVO, null, formularioValido, clientes))
    }

    @Test
    fun continuar_clienteNuevoNoDependeDelClienteElegidoDeLaLista() {
        // Con "Cliente nuevo" solo cuenta el formulario, no quien estaba marcado en la lista.
        assertFalse(puedeContinuarPaso1(ModoCliente.NUEVO, reciente, NuevoCliente(), clientes))
    }
}
