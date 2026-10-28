package com.cafesmart.app

import com.cafesmart.app.data.CafeFormulario
import com.cafesmart.app.data.Fecha
import com.cafesmart.app.data.Calidad
import com.cafesmart.app.data.ModoProductor
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.Productor
import com.cafesmart.app.data.TipoDocumento
import com.cafesmart.app.ui.compra.cafesCompletos
import com.cafesmart.app.ui.compra.crearSublote
import com.cafesmart.app.ui.compra.errorPeso
import com.cafesmart.app.ui.compra.errorPrecio
import com.cafesmart.app.ui.compra.fechaAMilis
import com.cafesmart.app.ui.compra.filtrarProductores
import com.cafesmart.app.ui.compra.formatoDinero
import com.cafesmart.app.ui.compra.formatoFechaLarga
import com.cafesmart.app.ui.compra.formatoKg
import com.cafesmart.app.ui.compra.hayMasProductores
import com.cafesmart.app.ui.compra.limpiarDocumento
import com.cafesmart.app.ui.compra.limpiarNombre
import com.cafesmart.app.ui.compra.limpiarPeso
import com.cafesmart.app.ui.compra.milisAFecha
import com.cafesmart.app.ui.compra.precioEnPantalla
import com.cafesmart.app.ui.compra.productoresVisibles
import com.cafesmart.app.ui.compra.puedeContinuarPaso1
import com.cafesmart.app.ui.compra.totalEstimado
import com.cafesmart.app.ui.compra.totalKg
import com.cafesmart.app.ui.compra.validarDocumento
import com.cafesmart.app.ui.compra.validarNombre
import com.cafesmart.app.ui.compra.validarTelefono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroCompraLogicaTest {

    private val productores = listOf(
        Productor(nombre = "Juan Pérez", documento = "123456789"),
        Productor(nombre = "Ana Gómez", documento = "900123456"),
        Productor(nombre = "Pepito Pérez", documento = "890301886"),
        Productor(nombre = "Andy López", documento = "11140596")
    )

    private val cafeCompleto = CafeFormulario(tipoCafe = "Verde", calidad = Calidad.BUENA, peso = "25", precio = "14.000")

    // ---------- Paso 1 ----------

    @Test
    fun filtrarProductores_porNombreSinImportarMayusculas() {
        assertEquals(listOf(productores[1]), filtrarProductores(productores, "ana"))
    }

    @Test
    fun filtrarProductores_porDocumento() {
        assertEquals(listOf(productores[0]), filtrarProductores(productores, "1234567"))
    }

    @Test
    fun recientes_muestraLosUltimos3PrimeroElMasNuevo() {
        val visibles = productoresVisibles(productores, "", verTodos = false)
        assertEquals(listOf(productores[3], productores[2], productores[1]), visibles)
        assertTrue(hayMasProductores(productores, "", verTodos = false))
        assertEquals(4, productoresVisibles(productores, "", verTodos = true).size)
        assertFalse(hayMasProductores(productores, "", verTodos = true))
    }

    @Test
    fun continuarPaso1_segunLaOpcionElegida() {
        val nuevoValido = NuevoProductor(TipoDocumento.CC, "1114059684", "Pepito Pérez", "")
        assertFalse(puedeContinuarPaso1(null, null, NuevoProductor()))
        assertTrue(puedeContinuarPaso1(ModoProductor.SIN_REGISTRAR, null, NuevoProductor()))
        assertFalse(puedeContinuarPaso1(ModoProductor.REGISTRADO, null, NuevoProductor()))
        assertTrue(puedeContinuarPaso1(ModoProductor.REGISTRADO, productores[0], NuevoProductor()))
        assertFalse(puedeContinuarPaso1(ModoProductor.NUEVO, null, NuevoProductor()))
        assertTrue(puedeContinuarPaso1(ModoProductor.NUEVO, null, nuevoValido))
    }

    @Test
    fun persona_exigeNombreYApellidoConLetras() {
        assertNull(validarNombre("Juan Pérez", TipoDocumento.CC))
        assertNotNull(validarNombre("Juan", TipoDocumento.CC))
        assertNotNull(validarNombre("Juan P", TipoDocumento.CC))
        assertEquals("Juan Pérez", limpiarNombre("Juan2 Pérez!", TipoDocumento.CC))
    }

    @Test
    fun empresa_aceptaNumerosYSimbolosEnElNombre() {
        assertNull(validarNombre("Café & Cía 2", TipoDocumento.NIT))
        assertNotNull(validarNombre("1234", TipoDocumento.NIT))
    }

    @Test
    fun cedula_entre6y10Digitos() {
        assertNull(validarDocumento("1234567890", TipoDocumento.CC))
        assertNotNull(validarDocumento("12345", TipoDocumento.CC))
        assertEquals("1234567890", limpiarDocumento("1234-567890123", TipoDocumento.CC))
    }

    @Test
    fun nit_conDigitoDeVerificacionOpcional() {
        assertNull(validarDocumento("900123456", TipoDocumento.NIT))
        assertNull(validarDocumento("900123456-7", TipoDocumento.NIT))
        assertNotNull(validarDocumento("900-123-456", TipoDocumento.NIT))
        assertNotNull(validarDocumento("1234", TipoDocumento.NIT))
    }

    @Test
    fun celular_opcionalODe10QueEmpiezaPor3() {
        assertNull(validarTelefono(""))
        assertNull(validarTelefono("3001234567"))
        assertNotNull(validarTelefono("6012345678"))
        assertNotNull(validarTelefono("300123"))
    }

    // ---------- Paso 2 ----------

    @Test
    fun limpiarPeso_aceptaComaYUnSoloPunto() {
        assertEquals("25.5", limpiarPeso("25,5"))
        assertEquals("25.55", limpiarPeso("25.5.5"))
        assertEquals("30", limpiarPeso("30kg"))
    }

    @Test
    fun precio_seMuestraConPuntoDeMiles() {
        assertEquals("14.000", precioEnPantalla("14000"))
        assertEquals("140.005", precioEnPantalla("14.0005"))
        assertEquals("", precioEnPantalla("abc"))
    }

    @Test
    fun crearSublote_conDatosCompletos() {
        val sublote = crearSublote(cafeCompleto)
        assertNotNull(sublote)
        assertEquals(350000.0, sublote!!.total, 0.001)
    }

    @Test
    fun crearSublote_faltaDatoOFueraDeRango_devuelveNull() {
        assertNull(crearSublote(cafeCompleto.copy(tipoCafe = null)))
        assertNull(crearSublote(cafeCompleto.copy(calidad = null)))
        assertNull(crearSublote(cafeCompleto.copy(peso = "0")))
        assertNull(crearSublote(cafeCompleto.copy(peso = "100000")))
        assertNull(crearSublote(cafeCompleto.copy(precio = "100.001")))
    }

    @Test
    fun errores_soloCuandoSePasaDelMaximo() {
        assertNull(errorPeso(cafeCompleto))
        assertNotNull(errorPeso(cafeCompleto.copy(peso = "100000")))
        assertNull(errorPrecio(cafeCompleto.copy(precio = "100.000")))
        assertNotNull(errorPrecio(cafeCompleto.copy(precio = "100.001")))
    }

    @Test
    fun totales_sumanTodasLasTarjetas() {
        val cafes = listOf(cafeCompleto, CafeFormulario(peso = "10", precio = "1.000"))
        assertEquals(35.0, totalKg(cafes), 0.001)
        assertEquals(360000.0, totalEstimado(cafes), 0.001)
        assertFalse(cafesCompletos(cafes))
        assertTrue(cafesCompletos(listOf(cafeCompleto)))
    }

    // ---------- Formatos y fechas ----------

    @Test
    fun formatos_estiloColombiano() {
        assertEquals("25 kg", formatoKg(25.0))
        assertEquals("25,5 kg", formatoKg(25.5))
        assertEquals("1.200 kg", formatoKg(1200.0))
        assertEquals("$ 350.000", formatoDinero(350000.0))
    }

    @Test
    fun fechaLarga_diceHoySiEsHoy() {
        val fecha = Fecha(dia = 2, mes = 6, anio = 2026)
        assertEquals("Hoy, 2 de junio de 2026", formatoFechaLarga(fecha, hoy = fecha))
        assertEquals("2 de junio de 2026", formatoFechaLarga(fecha, hoy = Fecha(3, 6, 2026)))
    }

    @Test
    fun fecha_idaYVueltaConMilisegundosDelCalendario() {
        val fecha = Fecha(dia = 31, mes = 12, anio = 2026)
        assertEquals(fecha, milisAFecha(fechaAMilis(fecha)))
    }
}
