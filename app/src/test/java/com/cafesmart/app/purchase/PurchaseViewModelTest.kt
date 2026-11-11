package com.cafesmart.app.purchase

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.productorSinRegistrar
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseViewModelTest {

    @Test
    fun alEmpezar_estaEnElPaso1SinOpcionElegida() {
        val viewModel = PurchaseViewModel()
        val estado = viewModel.uiState.value
        assertEquals(1, estado.paso)
        assertFalse(estado.registradoSeleccionado)
        assertFalse(estado.continuarPaso1Habilitado)
        assertEquals("Por favor, selecciona una opción para continuar el proceso.", estado.mensajePendientePaso1)
    }

    @Test
    fun sinRegistrar_pasaAlPaso2ConProductorSinRegistrar() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.SIN_REGISTRAR)
        assertTrue(viewModel.uiState.value.sinRegistrarSeleccionado)
        viewModel.continuarPaso1()
        val estado = viewModel.uiState.value
        assertEquals(2, estado.paso)
        assertEquals(productorSinRegistrar, estado.productorDeLaCompra)
    }

    @Test
    fun productorNuevo_seGuardaEnLaListaYQuedaElegido() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarDocumento("1114059684")
        viewModel.cambiarNombre("Pepito Pérez")
        // Sin guardar no se puede continuar
        assertFalse(viewModel.uiState.value.continuarPaso1Habilitado)
        viewModel.guardarNuevoProductor()
        assertEquals(ModoProductor.REGISTRADO, viewModel.uiState.value.modo)
        viewModel.continuarPaso1()
        val estado = viewModel.uiState.value
        assertEquals(2, estado.paso)
        assertEquals("Pepito Pérez", estado.productorDeLaCompra?.nombre)
        // El nuevo queda de primero en la lista y elegido
        assertTrue(estado.productoresEnPantalla[0].elegido)
        assertEquals(NuevoProductor(), estado.nuevoProductor)
        // La lista ya muestra el texto listo para la vista
        assertEquals("Cédula 1114059684", estado.productoresEnPantalla[0].documento)
    }

    @Test
    fun productorRegistrado_buscarYSeleccionar() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.REGISTRADO)
        // Recientes: 2 productores y el boton "Ver más"
        assertEquals("Recientes", viewModel.uiState.value.tituloLista)
        assertEquals("Pepito Pérez", viewModel.uiState.value.productoresEnPantalla[0].nombre)
        assertEquals(2, viewModel.uiState.value.productoresEnPantalla.size)
        assertTrue(viewModel.uiState.value.hayMasProductores)
        // Buscar por nombre
        viewModel.cambiarBusqueda("andy")
        assertEquals("Resultados", viewModel.uiState.value.tituloLista)
        assertEquals(1, viewModel.uiState.value.productoresEnPantalla.size)
        // Seleccionar: se marca y se puede continuar
        viewModel.elegirProductor(viewModel.uiState.value.productoresEnPantalla[0].productor)
        assertTrue(viewModel.uiState.value.productoresEnPantalla[0].elegido)
        assertTrue(viewModel.uiState.value.continuarPaso1Habilitado)
        // Sin resultados
        viewModel.cambiarBusqueda("xyz")
        assertEquals("Lo siento, no pudimos encontrarlo. Prueba con otro nombre o cédula.", viewModel.uiState.value.mensajeListaVacia)
    }

    @Test
    fun documentoYaRegistrado_noDejaContinuar() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarDocumento("890301886")
        viewModel.cambiarNombre("Otro Nombre")
        assertEquals("Este documento ya está registrado. Verifica en tu lista.", viewModel.uiState.value.documentoError)
        assertFalse(viewModel.uiState.value.guardarNuevoHabilitado)
    }

    @Test
    fun continuarPaso1_noAvanzaSiFaltanDatos() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.REGISTRADO)
        viewModel.continuarPaso1()
        assertEquals(1, viewModel.uiState.value.paso)
        assertEquals("Elige un productor de la lista.", viewModel.uiState.value.mensajePendientePaso1)
    }

    @Test
    fun productorNuevo_limpiaLoEscrito() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarTipoDocumento(TipoDocumento.NIT)
        viewModel.cambiarDocumento("12.345-678")
        viewModel.cambiarNombre("Ana2 Gómez!")
        viewModel.cambiarTelefono("300 123")
        val estado = viewModel.uiState.value
        assertTrue(estado.esEmpresa)
        assertEquals("12345678", estado.nuevoProductor.documento)
        // Con NIT (empresa) el nombre puede llevar números; los demás signos se quitan (decisión del 11 nov).
        assertEquals("Ana2 Gómez", estado.nuevoProductor.nombre)
        assertEquals("300123", estado.nuevoProductor.telefono)
    }

    @Test
    fun paso2_entregaTextosListosParaLaVista() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarTipoCafe(0, "Verde")
        viewModel.cambiarCalidad(0, Calidad.BUENA)
        viewModel.cambiarPeso(0, "25")
        viewModel.cambiarPrecio(0, "14.000")
        val estado = viewModel.uiState.value
        assertEquals("14000", estado.cafes[0].precio)
        assertEquals("14.000", estado.cafesEnPantalla[0].precio)
        assertEquals("25 kg", estado.totalKgTexto)
        assertEquals("$ 350.000", estado.totalEstimadoTexto)
        assertTrue(estado.cafesListos)
        assertNull(estado.mensajePendientePaso2)
    }

    @Test
    fun paso2_agregarYQuitarCafes() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarTipoCafe(0, "Verde")
        viewModel.cambiarCalidad(0, Calidad.BUENA)
        viewModel.cambiarPeso(0, "25")
        viewModel.cambiarPrecio(0, "14000")
        viewModel.agregarCafe()
        assertEquals(2, viewModel.uiState.value.cafesEnPantalla.size)
        assertTrue(viewModel.uiState.value.cafesEnPantalla[1].puedeQuitar)
        assertFalse(viewModel.uiState.value.cafesListos)

        viewModel.quitarCafe(1)
        assertEquals(1, viewModel.uiState.value.cafes.size)
        assertFalse(viewModel.uiState.value.cafesEnPantalla[0].puedeQuitar)
        assertEquals(1, viewModel.uiState.value.sublotes.size)
    }

    @Test
    fun cambiarFecha_desdeElCalendario() {
        val viewModel = PurchaseViewModel()
        // 15 de mayo de 2026 a medianoche UTC, como lo entrega el calendario
        viewModel.cambiarFecha(1778803200000L)
        val fecha = viewModel.uiState.value.fecha
        assertEquals(15, fecha.dia)
        assertEquals(5, fecha.mes)
        assertEquals(2026, fecha.anio)
    }

    // Lleva el flujo hasta el paso 3 con un cafe completo
    private fun viewModelEnPaso3(): PurchaseViewModel {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.SIN_REGISTRAR)
        viewModel.continuarPaso1()
        viewModel.cambiarTipoCafe(0, "Verde")
        viewModel.cambiarCalidad(0, Calidad.BUENA)
        viewModel.cambiarPeso(0, "25")
        viewModel.cambiarPrecio(0, "14000")
        viewModel.continuarPaso2()
        return viewModel
    }

    @Test
    fun continuarPaso2_noAvanzaSiFaltanDatos() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.SIN_REGISTRAR)
        viewModel.continuarPaso1()
        viewModel.continuarPaso2()
        assertEquals(2, viewModel.uiState.value.paso)
    }

    @Test
    fun paso3_entregaElResumenListoParaLaVista() {
        val estado = viewModelEnPaso3().uiState.value
        assertEquals(3, estado.paso)
        assertEquals("Sin registrar", estado.productorTexto)
        val cafe = estado.cafesEnResumen[0]
        assertEquals("Café verde", cafe.titulo)
        assertEquals("Calidad buena", cafe.calidad)
        assertEquals("25 kg × $ 14.000", cafe.detalle)
        assertEquals("$ 350.000", cafe.total)
    }

    @Test
    fun paso3_editarVuelveAlPaso2SinPerderDatos() {
        val viewModel = viewModelEnPaso3()
        viewModel.volverAlPaso2()
        assertEquals(2, viewModel.uiState.value.paso)
        assertEquals("25", viewModel.uiState.value.cafes[0].peso)
    }

    @Test
    fun confirmarCompra_muestraCompraRegistrada() {
        val viewModel = viewModelEnPaso3()
        viewModel.confirmarCompra()
        assertTrue(viewModel.uiState.value.compraRegistrada)
    }

    @Test
    fun reiniciarCompra_empiezaDeCeroYConservaLosProductores() {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarDocumento("1114059684")
        viewModel.cambiarNombre("Ana Gómez")
        viewModel.guardarNuevoProductor()
        val productores = viewModel.uiState.value.productores

        viewModel.reiniciarCompra()

        val estado = viewModel.uiState.value
        assertEquals(1, estado.paso)
        assertFalse(estado.compraRegistrada)
        assertNull(estado.modo)
        assertEquals(productores, estado.productores)
    }

    // Deja el primer cafe completo
    private fun viewModelConUnCafe(): PurchaseViewModel {
        val viewModel = PurchaseViewModel()
        viewModel.cambiarTipoCafe(0, "Verde")
        viewModel.cambiarCalidad(0, Calidad.BUENA)
        viewModel.cambiarPeso(0, "25")
        viewModel.cambiarPrecio(0, "14000")
        return viewModel
    }

    @Test
    fun agregarCafe_elAnteriorQuedaComoTiraYElNuevoAbierto() {
        val viewModel = viewModelConUnCafe()
        viewModel.agregarCafe()
        val cafes = viewModel.uiState.value.cafesEnPantalla
        assertFalse(cafes[0].abierto)
        assertTrue(cafes[1].abierto)
        assertEquals("Café 1 · Verde", cafes[0].tituloTira)
        assertEquals("Buena · 25 kg × $ 14.000", cafes[0].detalleTira)
        assertEquals("$ 350.000", cafes[0].totalTira)
    }

    @Test
    fun editarTira_abreEseCafeSinPerderLosDemas() {
        val viewModel = viewModelConUnCafe()
        viewModel.agregarCafe()
        viewModel.abrirCafe(0)
        val cafes = viewModel.uiState.value.cafesEnPantalla
        assertTrue(cafes[0].abierto)
        assertFalse(cafes[1].abierto)
        assertEquals("Café 2", cafes[1].tituloTira)
        assertEquals("Sin completar", cafes[1].detalleTira)
        assertEquals("", cafes[1].totalTira)
    }

    @Test
    fun quitarCafe_mantieneAbiertoElCorrecto() {
        val viewModel = viewModelConUnCafe()
        viewModel.agregarCafe()
        // Quitar una tira de arriba: el abierto baja de la posicion 1 a la 0
        viewModel.quitarCafe(0)
        assertEquals(0, viewModel.uiState.value.cafeAbierto)
        assertEquals(1, viewModel.uiState.value.cafes.size)

        val otro = viewModelConUnCafe()
        otro.agregarCafe()
        // Quitar el abierto: se abre el ultimo que quede
        otro.quitarCafe(1)
        assertEquals(0, otro.uiState.value.cafeAbierto)
        assertTrue(otro.uiState.value.cafesEnPantalla[0].abierto)
    }

    @Test
    fun editarDesdeElPaso3_abreEseCafeEnElPaso2() {
        val viewModel = viewModelEnPaso3()
        viewModel.editarCafe(0)
        assertEquals(2, viewModel.uiState.value.paso)
        assertTrue(viewModel.uiState.value.cafesEnPantalla[0].abierto)
    }

    @Test
    fun cancelarNuevoProductor_borraLoEscritoYConservaLaOpcionElegida() {
        val viewModel = PurchaseViewModel()
        viewModel.elegirModo(ModoProductor.SIN_REGISTRAR)
        viewModel.cambiarNombre("Ana Gómez")
        viewModel.cancelarNuevoProductor()
        assertEquals(ModoProductor.SIN_REGISTRAR, viewModel.uiState.value.modo)
        assertEquals(NuevoProductor(), viewModel.uiState.value.nuevoProductor)
    }

    @Test
    fun listaDeProductores_buscaYSeLimpiaAlCerrar() {
        val viewModel = PurchaseViewModel()
        assertEquals(3, viewModel.uiState.value.productoresEnLista.size)
        viewModel.cambiarBusquedaLista("xyz")
        assertEquals("Lo siento, no pudimos encontrarlo. Prueba con otro nombre o cédula.", viewModel.uiState.value.mensajeSinResultadosLista)
        viewModel.cerrarLista()
        assertEquals("", viewModel.uiState.value.busquedaLista)
        assertEquals(3, viewModel.uiState.value.productoresEnLista.size)
    }

    @Test
    fun editarProductor_guardaLosCambiosYActualizaElElegido() {
        val viewModel = PurchaseViewModel()
        val pepito = viewModel.uiState.value.productores.last()
        viewModel.elegirProductor(pepito)
        viewModel.editarProductor(pepito)
        // Su propio documento no cuenta como repetido
        assertNull(viewModel.uiState.value.edicionDocumentoError)
        viewModel.cambiarNombreEdicion("Pepito Gómez")
        viewModel.guardarEdicion()
        val estado = viewModel.uiState.value
        assertNull(estado.productorEnEdicion)
        assertEquals("Pepito Gómez", estado.productorElegido?.nombre)
        assertEquals("Pepito Gómez", estado.productores.last().nombre)
    }

    @Test
    fun editarProductor_noDejaUsarElDocumentoDeOtro() {
        val viewModel = PurchaseViewModel()
        val pepito = viewModel.uiState.value.productores.last()
        viewModel.editarProductor(pepito)
        viewModel.cambiarDocumentoEdicion("11140596")
        assertFalse(viewModel.uiState.value.guardarEdicionHabilitado)
    }

    @Test
    fun cancelarEdicion_noCambiaAlProductor() {
        val viewModel = PurchaseViewModel()
        val pepito = viewModel.uiState.value.productores.last()
        viewModel.editarProductor(pepito)
        viewModel.cambiarNombreEdicion("Otro Nombre")
        viewModel.cancelarEdicion()
        assertEquals(pepito, viewModel.uiState.value.productores.last())
    }
}
