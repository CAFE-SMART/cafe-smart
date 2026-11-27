package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.NuevoCliente
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.viewmodel.purchase.SIN_RESULTADOS
import com.cafesmart.app.viewmodel.sale.SaleUiState
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Paso 1 de Ventas (cliente), probado como lo usa la pantalla: cada prueba es una accion de la vista
// (tocar una tarjeta, escribir en un campo, pulsar "Continuar") y se revisa lo que la vista mostraria.
class ClienteViewModelTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    private val exportadora = clientesDeEjemplo[2]
    private val carlos = clientesDeEjemplo[1]
    private val beneficiadora = clientesDeEjemplo[0]

    // ---------- Al empezar (Figma: "Cliente (sin elegir)") ----------

    @Test
    fun alEmpezar_ningunaOpcionEstaElegidaYContinuarEstaDeshabilitado() {
        val estado = viewModelDeVentasDePrueba().uiState.value
        assertNull(estado.modo)
        assertFalse(estado.registradoSeleccionado)
        assertFalse(estado.sinRegistrarSeleccionado)
        assertFalse(estado.nuevoSeleccionado)
        assertFalse(estado.continuarPaso1Habilitado)
        assertNull(estado.cliente)
        assertNull(estado.clienteDeLaVenta)
    }

    @Test
    fun alEmpezar_haySinBusquedaNiFormularioEscrito() {
        val estado = viewModelDeVentasDePrueba().uiState.value
        assertEquals("", estado.busqueda)
        assertEquals("", estado.busquedaLista)
        assertEquals(NuevoCliente(), estado.nuevoCliente)
        assertEquals(TipoDocumento.CC, estado.nuevoCliente.tipo)
        assertFalse(estado.esEmpresa)
    }

    @Test
    fun alEmpezar_seUsanLosClientesDeEjemplo() {
        assertEquals(clientesDeEjemplo, viewModelDeVentasDePrueba().uiState.value.clientes)
    }

    @Test
    fun alEmpezar_losRecientesSonLosDosUltimosElMasNuevoPrimero() {
        val estado = viewModelDeVentasDePrueba().uiState.value
        assertEquals(listOf("Exportadora del Sur S.A.", "Carlos Mejía"), estado.clientesEnPantalla.map { it.nombre })
        assertEquals("Recientes", estado.tituloLista)
        assertNull(estado.mensajeListaVacia)
        assertTrue(estado.hayMasClientes)
    }

    @Test
    fun cadaFilaMuestraElTipoYElNumeroDeDocumento() {
        val filas = viewModelDeVentasDePrueba().uiState.value.clientesEnPantalla
        assertEquals("NIT 9003421215", filas[0].documento)
        assertEquals("Cédula 16780012", filas[1].documento)
    }

    @Test
    fun alEmpezar_ningunaFilaEstaElegida() {
        assertTrue(viewModelDeVentasDePrueba().uiState.value.clientesEnPantalla.none { it.elegido })
    }

    // ---------- Elegir la opcion (las tres tarjetas) ----------

    @Test
    fun elegirModo_marcaSoloLaTarjetaTocada() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        assertTrue(viewModel.uiState.value.registradoSeleccionado)
        assertFalse(viewModel.uiState.value.sinRegistrarSeleccionado)
        assertFalse(viewModel.uiState.value.nuevoSeleccionado)

        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        assertFalse(viewModel.uiState.value.registradoSeleccionado)
        assertTrue(viewModel.uiState.value.sinRegistrarSeleccionado)

        viewModel.elegirModo(ModoCliente.NUEVO)
        assertFalse(viewModel.uiState.value.sinRegistrarSeleccionado)
        assertTrue(viewModel.uiState.value.nuevoSeleccionado)
    }

    @Test
    fun elegirModo_tocarLaMismaTarjetaDosVecesLaDejaElegida() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        assertEquals(ModoCliente.SIN_REGISTRAR, viewModel.uiState.value.modo)
    }

    // ---------- Sin registrar (Figma: "Cliente sin registrar") ----------

    @Test
    fun sinRegistrar_habilitaContinuarYLaVentaQuedaSinCliente() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
        assertNull(viewModel.uiState.value.clienteDeLaVenta)
    }

    @Test
    fun sinRegistrar_aunqueAntesSeHayaMarcadoACliente_laVentaQuedaSinCliente() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(carlos)
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        assertNull(viewModel.uiState.value.clienteDeLaVenta)
    }

    // ---------- Cliente registrado (Figma: "buscando") ----------

    @Test
    fun registrado_sinElegirACliente_continuarSigueDeshabilitado() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun registrado_alElegirACliente_continuarSeHabilitaYLaFilaQuedaMarcada() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(carlos)
        val estado = viewModel.uiState.value
        assertTrue(estado.continuarPaso1Habilitado)
        assertEquals(carlos, estado.clienteDeLaVenta)
        assertEquals(listOf(false, true), estado.clientesEnPantalla.map { it.elegido })
    }

    @Test
    fun registrado_cambiarDeClienteMarcaSoloElNuevo() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(carlos)
        viewModel.elegirCliente(exportadora)
        assertEquals(listOf(true, false), viewModel.uiState.value.clientesEnPantalla.map { it.elegido })
        assertEquals(exportadora, viewModel.uiState.value.clienteDeLaVenta)
    }

    @Test
    fun registrado_elClienteElegidoSeConservaAlCambiarDeOpcionYVolver() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(carlos)
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        assertEquals(carlos, viewModel.uiState.value.clienteDeLaVenta)
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun registrado_conClienteMarcadoPeroOtraOpcion_noCuentaComoCliente() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirCliente(carlos)
        // Sin elegir ninguna tarjeta, el cliente marcado no habilita Continuar.
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)
        assertNull(viewModel.uiState.value.clienteDeLaVenta)
    }

    // ---------- Buscar en la tarjeta ----------

    @Test
    fun buscar_porNombre() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("carlos")
        val estado = viewModel.uiState.value
        assertEquals(listOf("Carlos Mejía"), estado.clientesEnPantalla.map { it.nombre })
        assertEquals("Resultados", estado.tituloLista)
        assertFalse(estado.hayMasClientes)
    }

    @Test
    fun buscar_porDocumento() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("9003421215")
        assertEquals(listOf("Exportadora del Sur S.A."), viewModel.uiState.value.clientesEnPantalla.map { it.nombre })
    }

    @Test
    fun buscar_encuentraClientesQueNoEstabanEntreLosRecientes() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("beneficiadora")
        assertEquals(listOf(beneficiadora), viewModel.uiState.value.clientesEnPantalla.map { it.cliente })
    }

    @Test
    fun buscar_sinResultadosMuestraElMensaje() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("zzz")
        assertTrue(viewModel.uiState.value.clientesEnPantalla.isEmpty())
        assertEquals(SIN_RESULTADOS, viewModel.uiState.value.mensajeListaVacia)
    }

    @Test
    fun buscar_alBorrarElTextoVuelvenLosRecientes() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("carlos")
        viewModel.cambiarBusqueda("")
        assertEquals("Recientes", viewModel.uiState.value.tituloLista)
        assertEquals(2, viewModel.uiState.value.clientesEnPantalla.size)
        assertTrue(viewModel.uiState.value.hayMasClientes)
    }

    @Test
    fun buscar_losEspaciosSolosNoCuentanComoBusqueda() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("   ")
        assertEquals("Recientes", viewModel.uiState.value.tituloLista)
        assertEquals(2, viewModel.uiState.value.clientesEnPantalla.size)
    }

    @Test
    fun buscar_guardaLoEscritoTalCual() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("  Carlos ")
        assertEquals("  Carlos ", viewModel.uiState.value.busqueda)
    }

    @Test
    fun buscar_elClienteElegidoSigueMarcadoEnLosResultados() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(carlos)
        viewModel.cambiarBusqueda("carlos")
        assertEquals(listOf(true), viewModel.uiState.value.clientesEnPantalla.map { it.elegido })
    }

    @Test
    fun sinClientesRegistrados_elMensajeInvitaACrearUno() {
        val estado = SaleUiState(clientes = emptyList())
        assertTrue(estado.clientesEnPantalla.isEmpty())
        assertEquals("Aún no tienes clientes registrados. Usa \"Cliente nuevo\" para crear uno.", estado.mensajeListaVacia)
        assertFalse(estado.hayMasClientes)
    }

    @Test
    fun sinClientesRegistrados_buscarMuestraElMensajeDeSinResultados() {
        assertEquals(SIN_RESULTADOS, SaleUiState(clientes = emptyList(), busqueda = "ana").mensajeListaVacia)
    }

    @Test
    fun conDosClientesOMenos_noHayBotonVerMas() {
        assertFalse(SaleUiState(clientes = clientesDeEjemplo.take(2)).hayMasClientes)
    }

    // ---------- Hoja "Ver más clientes" ----------

    @Test
    fun hoja_muestraTodosLosClientesDelMasRecienteAlMasAntiguo() {
        val estado = viewModelDeVentasDePrueba().uiState.value
        assertEquals(
            listOf("Exportadora del Sur S.A.", "Carlos Mejía", "Beneficiadora Las Palmas S.A.S."),
            estado.clientesEnLista.map { it.nombre }
        )
        assertNull(estado.mensajeSinResultadosLista)
    }

    @Test
    fun hoja_tieneSuPropioBuscadorIndependienteDeLaTarjeta() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("carlos")
        viewModel.cambiarBusquedaLista("palmas")
        val estado = viewModel.uiState.value
        assertEquals(listOf("Beneficiadora Las Palmas S.A.S."), estado.clientesEnLista.map { it.nombre })
        assertEquals(listOf("Carlos Mejía"), estado.clientesEnPantalla.map { it.nombre })
    }

    @Test
    fun hoja_sinResultadosMuestraElMensaje() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusquedaLista("zzz")
        assertEquals(SIN_RESULTADOS, viewModel.uiState.value.mensajeSinResultadosLista)
    }

    @Test
    fun hoja_alCerrarSeLimpiaSuBuscadorYNoLaDeLaTarjeta() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("carlos")
        viewModel.cambiarBusquedaLista("palmas")
        viewModel.cerrarLista()
        assertEquals("", viewModel.uiState.value.busquedaLista)
        assertEquals("carlos", viewModel.uiState.value.busqueda)
        assertEquals(3, viewModel.uiState.value.clientesEnLista.size)
    }

    @Test
    fun hoja_elegirUnClienteLoMarcaEnLaListaYEnLaTarjeta() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(beneficiadora)
        val estado = viewModel.uiState.value
        assertEquals(listOf(false, false, true), estado.clientesEnLista.map { it.elegido })
        assertEquals(beneficiadora, estado.clienteDeLaVenta)
    }

    // ---------- Formulario "Cliente nuevo" (Figma: "Cliente nuevo") ----------

    @Test
    fun nuevo_sinEscribirNada_continuarEstaDeshabilitadoYSoloSeMarcaElDocumento() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        val estado = viewModel.uiState.value
        assertFalse(estado.continuarPaso1Habilitado)
        // Solo el primer campo que falta: el documento. El nombre se marca cuando el documento ya esta bien.
        assertEquals("Escribe el documento del cliente.", estado.documentoError)
        assertNull(estado.nombreError)
        assertNull(estado.telefonoError)
    }

    @Test
    fun nuevo_conDocumentoBueno_seMarcaElNombreVacio() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1112223334")
        val estado = viewModel.uiState.value
        assertNull(estado.documentoError)
        assertEquals("Escribe el nombre del cliente.", estado.nombreError)
    }

    @Test
    fun nuevo_empresaSinNombre_pideElNombreDeLaEmpresa() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarDocumento("800987654")
        assertEquals("Escribe el nombre de la empresa.", viewModel.uiState.value.nombreError)
    }

    @Test
    fun nuevo_sinElegirEseModo_noSeMarcaNingunCampo() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        val estado = viewModel.uiState.value
        assertNull(estado.documentoError)
        assertNull(estado.nombreError)
    }

    @Test
    fun nuevo_laCedulaViene_seleccionadaPorDefectoYSePuedeCambiarANit() {
        val viewModel = viewModelDeVentasDePrueba()
        assertFalse(viewModel.uiState.value.esEmpresa)
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        assertTrue(viewModel.uiState.value.esEmpresa)
        viewModel.cambiarTipoDocumento(TipoDocumento.CC)
        assertFalse(viewModel.uiState.value.esEmpresa)
    }

    @Test
    fun nuevo_documento_soloNumeros() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarDocumento("1.114-059 abc")
        assertEquals("1114059", viewModel.uiState.value.nuevoCliente.documento)
        viewModel.cambiarDocumento("abcdef")
        assertEquals("", viewModel.uiState.value.nuevoCliente.documento)
    }

    @Test
    fun nuevo_documento_nuncaPasaDeDiezDigitosConCedula() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarDocumento("12345678901234567890")
        assertEquals("1234567890", viewModel.uiState.value.nuevoCliente.documento)
    }

    @Test
    fun nuevo_documento_nuncaPasaDeDiezDigitosConNit() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarDocumento("90034212159999")
        assertEquals("9003421215", viewModel.uiState.value.nuevoCliente.documento)
    }

    @Test
    fun nuevo_documento_noTomaElPuntoNiElGuionDelNit() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarDocumento("900.342.121-5")
        assertEquals("9003421215", viewModel.uiState.value.nuevoCliente.documento)
    }

    @Test
    fun nuevo_nombreConCedula_noPermiteNumerosNiCaracteresRaros() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarNombre("Juan123 Pérez!!@#")
        assertEquals("Juan Pérez", viewModel.uiState.value.nuevoCliente.nombre)
    }

    @Test
    fun nuevo_nombreConCedula_conservaTildes() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarNombre("María Muñoz")
        assertEquals("María Muñoz", viewModel.uiState.value.nuevoCliente.nombre)
    }

    @Test
    fun nuevo_nombre_nuncaPasaDeTreintaCaracteres() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarNombre("A".repeat(100))
        assertEquals(30, viewModel.uiState.value.nuevoCliente.nombre.length)
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarNombre("1".repeat(100))
        assertEquals(30, viewModel.uiState.value.nuevoCliente.nombre.length)
    }

    @Test
    fun nuevo_nombreConNit_permiteNumerosPuntoYAmpersand() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarNombre("Café 2000 S.A.S. & Cía")
        assertEquals("Café 2000 S.A.S. & Cía", viewModel.uiState.value.nuevoCliente.nombre)
    }

    @Test
    fun nuevo_alPasarDeNitACedula_elNombreSeLimpia() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarNombre("Café 2000 S.A.S.")
        viewModel.cambiarTipoDocumento(TipoDocumento.CC)
        assertEquals("Café SAS", viewModel.uiState.value.nuevoCliente.nombre)
    }

    @Test
    fun nuevo_celular_noPermiteLetras() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTelefono("abc300xyz123")
        assertEquals("300123", viewModel.uiState.value.nuevoCliente.telefono)
    }

    @Test
    fun nuevo_celular_nuncaPasaDeDiezNumeros() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTelefono("30012345678901")
        assertEquals("3001234567", viewModel.uiState.value.nuevoCliente.telefono)
    }

    // ---------- Errores del formulario ----------

    @Test
    fun error_cedulaConMenosDeSeisNumeros() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarDocumento("12345")
        assertEquals("La cédula debe tener entre 6 y 10 números.", viewModel.uiState.value.documentoError)
        viewModel.cambiarDocumento("123456")
        assertNull(viewModel.uiState.value.documentoError)
    }

    @Test
    fun error_nitConMenosDeNueveNumeros() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarDocumento("12345678")
        assertEquals("El NIT debe tener 9 o 10 números.", viewModel.uiState.value.documentoError)
        viewModel.cambiarDocumento("123456789")
        assertNull(viewModel.uiState.value.documentoError)
    }

    @Test
    fun error_alCambiarDeTipoElMismoNumeroSeRevisaConLaReglaNueva() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarDocumento("1234567")
        assertNull(viewModel.uiState.value.documentoError)
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        assertEquals("El NIT debe tener 9 o 10 números.", viewModel.uiState.value.documentoError)
    }

    @Test
    fun error_documentoDeUnClienteYaRegistrado() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarDocumento("16780012")
        assertEquals("Este documento ya está registrado. Verifica en tu lista.", viewModel.uiState.value.documentoError)
    }

    @Test
    fun error_nombreDePersonaNecesitaNombreYApellido() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarNombre("Juan")
        assertEquals("Escribe tu nombre y apellido.", viewModel.uiState.value.nombreError)
        viewModel.cambiarNombre("Juan P")
        assertEquals("Escribe tu nombre y apellido.", viewModel.uiState.value.nombreError)
        viewModel.cambiarNombre("Juan Pé")
        assertNull(viewModel.uiState.value.nombreError)
    }

    @Test
    fun error_nombreDeEmpresaBastaConUnaPalabra() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarNombre("Alpes")
        assertNull(viewModel.uiState.value.nombreError)
    }

    @Test
    fun error_celularFueraDeFormato() {
        val viewModel = viewModelDeVentasDePrueba()
        val mensaje = "Recuerda, tu celular debe tener 10 números y empezar por el 3."
        viewModel.cambiarTelefono("6012345678")
        assertEquals(mensaje, viewModel.uiState.value.telefonoError)
        viewModel.cambiarTelefono("300123")
        assertEquals(mensaje, viewModel.uiState.value.telefonoError)
        viewModel.cambiarTelefono("3001234567")
        assertNull(viewModel.uiState.value.telefonoError)
    }

    @Test
    fun error_celularVacioNoEsError() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarTelefono("")
        assertNull(viewModel.uiState.value.telefonoError)
    }

    @Test
    fun error_noHayErroresMientrasLosCamposEstanVacios() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarDocumento("")
        viewModel.cambiarNombre("")
        viewModel.cambiarTelefono("")
        val estado = viewModel.uiState.value
        assertNull(estado.documentoError)
        assertNull(estado.nombreError)
        assertNull(estado.telefonoError)
    }

    // ---------- Continuar con "Cliente nuevo" ----------

    @Test
    fun nuevo_continuarSeHabilitaCuandoTodoEstaBien() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun nuevo_continuarSeHabilitaSinCelular() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        viewModel.cambiarNombre("Juan Pérez")
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun nuevo_continuarSigueDeshabilitadoSiFaltaOFallaUnCampo() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)     // falta el nombre
        viewModel.cambiarNombre("Juan")
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)     // nombre incompleto
        viewModel.cambiarNombre("Juan Pérez")
        viewModel.cambiarTelefono("6012345678")
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)     // celular mal
        viewModel.cambiarTelefono("")
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun nuevo_continuarSigueDeshabilitadoConDocumentoRepetido() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("16780012")
        viewModel.cambiarNombre("Juan Pérez")
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun nuevo_continuarRegistraAlClienteYLoDejaElegido() {
        val repositorio = RepositorioDeVentasFalso(clientesDeEjemplo)
        val viewModel = viewModelDeVentasDePrueba(repositorio)
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        val estado = viewModel.uiState.value
        // El cliente que guardo el repositorio ya trae su id real; los demas datos son los del formulario.
        val guardado = repositorio.clientes.last()
        assertNotEquals(0L, guardado.id)
        assertEquals(
            Cliente(nombre = "Juan Pérez", documento = "1114059684", tipoDocumento = TipoDocumento.CC, telefono = "3001234567"),
            guardado.copy(id = 0)
        )
        assertEquals(4, estado.clientes.size)
        assertEquals(guardado, estado.clientes.last())
        assertEquals(guardado, estado.cliente)
        assertEquals(guardado, estado.clienteDeLaVenta)
        assertEquals(ModoCliente.REGISTRADO, estado.modo)
    }

    @Test
    fun nuevo_continuarLimpiaElFormularioYLaBusqueda() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.cambiarBusqueda("carlos")
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        assertEquals(NuevoCliente(), viewModel.uiState.value.nuevoCliente)
        assertEquals("", viewModel.uiState.value.busqueda)
    }

    @Test
    fun nuevo_elClienteRegistradoApareceDePrimeroEnRecientes() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        val filas = viewModel.uiState.value.clientesEnPantalla
        assertEquals(listOf("Juan Pérez", "Exportadora del Sur S.A."), filas.map { it.nombre })
        assertEquals(listOf(true, false), filas.map { it.elegido })
        assertEquals("Cédula 1114059684", filas[0].documento)
    }

    @Test
    fun nuevo_despuesDeRegistrarloContinuarSigueHabilitado() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun nuevo_continuarDosVecesNoRegistraAlClienteDosVeces() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        viewModel.continuarPaso1()
        assertEquals(4, viewModel.uiState.value.clientes.size)
    }

    @Test
    fun nuevo_elDocumentoRecienRegistradoYaCuentaComoRepetido() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        assertEquals("Este documento ya está registrado. Verifica en tu lista.", viewModel.uiState.value.documentoError)
    }

    @Test
    fun nuevo_registrarUnaEmpresaConNit() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarDocumento("900.555.444-1")
        viewModel.cambiarNombre("Café Los Andes S.A.S.")
        viewModel.continuarPaso1()
        val cliente = viewModel.uiState.value.cliente
        assertEquals(TipoDocumento.NIT, cliente?.tipoDocumento)
        assertEquals("9005554441", cliente?.documento)
        assertEquals("", cliente?.telefono)
        assertEquals("NIT 9005554441", viewModel.uiState.value.clientesEnPantalla[0].documento)
    }

    @Test
    fun nuevo_elNombreSeGuardaSinEspaciosDeMas() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        viewModel.cambiarNombre("  Juan     Pérez ")
        viewModel.continuarPaso1()
        assertEquals("Juan Pérez", viewModel.uiState.value.cliente?.nombre)
    }

    @Test
    fun nuevo_dosClientesNuevosQuedanEnOrdenDeLlegada() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059685")
        viewModel.cambiarNombre("Pedro Gómez")
        viewModel.continuarPaso1()
        assertEquals(listOf("Pedro Gómez", "Juan Pérez"), viewModel.uiState.value.clientesEnPantalla.map { it.nombre })
        assertEquals(5, viewModel.uiState.value.clientes.size)
    }

    // ---------- Continuar no cambia nada cuando no debe ----------

    @Test
    fun continuar_conElFormularioIncompletoNoCambiaElEstado() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        val antes = viewModel.uiState.value
        viewModel.continuarPaso1()
        assertEquals(antes, viewModel.uiState.value)
    }

    @Test
    fun continuar_sinOpcionElegidaNoCambiaElEstado() {
        val viewModel = viewModelDeVentasDePrueba()
        val antes = viewModel.uiState.value
        viewModel.continuarPaso1()
        assertEquals(antes, viewModel.uiState.value)
    }

    @Test
    fun continuar_conClienteRegistradoNoCambiaElEstado() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(carlos)
        val antes = viewModel.uiState.value
        viewModel.continuarPaso1()
        assertEquals(antes, viewModel.uiState.value)
    }

    @Test
    fun continuar_sinRegistrarNoCambiaElEstado() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        val antes = viewModel.uiState.value
        viewModel.continuarPaso1()
        assertEquals(antes, viewModel.uiState.value)
    }

    // ---------- Cambiar de opcion conserva lo escrito ----------

    @Test
    fun cambiarDeOpcion_noBorraLoEscritoEnElFormulario() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirModo(ModoCliente.NUEVO)
        assertEquals(NuevoCliente(TipoDocumento.CC, "1114059684", "Juan Pérez", "3001234567"), viewModel.uiState.value.nuevoCliente)
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    @Test
    fun cambiarDeOpcion_noBorraLaBusqueda() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.cambiarBusqueda("carlos")
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        assertEquals("carlos", viewModel.uiState.value.busqueda)
    }

    @Test
    fun formularioValidoPeroOpcionRegistrado_continuarDependeDelClienteElegido() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)
    }

    // ---------- Reiniciar la venta ----------

    @Test
    fun reiniciar_borraLaEleccionPeroConservaLosClientesNuevos() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.continuarPaso1()
        viewModel.cambiarBusqueda("juan")
        viewModel.cambiarBusquedaLista("palmas")
        viewModel.reiniciarVenta()
        val estado = viewModel.uiState.value
        assertEquals(4, estado.clientes.size)
        assertNull(estado.modo)
        assertNull(estado.cliente)
        assertNull(estado.clienteDeLaVenta)
        assertEquals("", estado.busqueda)
        assertEquals("", estado.busquedaLista)
        assertEquals(NuevoCliente(), estado.nuevoCliente)
        assertFalse(estado.continuarPaso1Habilitado)
    }

    @Test
    fun reiniciar_borraTambienLoEscritoEnElFormulario() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.escribirClienteValido()
        viewModel.reiniciarVenta()
        assertEquals(NuevoCliente(), viewModel.uiState.value.nuevoCliente)
        assertNull(viewModel.uiState.value.modo)
    }

    // ---------- Paso 1 no toca el paso 2 ----------

    @Test
    fun elegirCliente_noCambiaLoDelPaso2() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(carlos)
        val estado = viewModel.uiState.value
        assertTrue(estado.elegidos.isEmpty())
        assertTrue(estado.precios.isEmpty())
        assertFalse(estado.puedeContinuar)
        assertEquals(0L, estado.totalARecibir)
    }

    @Test
    fun cambiarPrecio_noCambiaLoDelPaso1() {
        val viewModel = viewModelDeVentasDePrueba()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        assertEquals(ModoCliente.SIN_REGISTRAR, viewModel.uiState.value.modo)
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
    }
}
